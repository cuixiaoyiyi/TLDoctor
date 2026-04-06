package Threadlocal;

import Threadlocal.common.DetectionIssue;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SignatureTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.toolkits.scalar.SimpleLocalDefs;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SootTypeIDetector {

    public static void analyzeClass(SootClass sootClass, List<DetectionIssue> globalIssues) {
        if (sootClass.isInterface() || sootClass.isPhantom()) {
            return;
        }

        Map<String, String> allThreadLocals = new HashMap<>();
        Map<String, Integer> threadLocalLines = new HashMap<>();
        Set<String> removedThreadLocals = new HashSet<>();

        // 1. 收集潜在泄露源与真实行号
        for (SootField field : sootClass.getFields()) {
            if (isThreadLocalType(field)) {
                allThreadLocals.put(field.getName(), extractGenericType(field));
                threadLocalLines.put(field.getName(), findFieldInitLineNumber(sootClass, field));
            }
        }

        // 2. 扫描寻找 remove() 的触发点
        for (SootMethod method : new ArrayList<>(sootClass.getMethods())) { // 【修复点1】
            if (!method.isConcrete()) continue;

            Body body = method.retrieveActiveBody();
            for (Unit unit : body.getUnits()) {
                if (unit instanceof Stmt) {
                    Stmt stmt = (Stmt) unit;
                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr invokeExpr = stmt.getInvokeExpr();
                        SootMethod invokedMethod = invokeExpr.getMethod();

                        if (invokedMethod.getDeclaringClass().getName().equals("java.lang.ThreadLocal")
                                && invokedMethod.getName().equals("remove")) {

                            if (invokeExpr instanceof InstanceInvokeExpr) {
                                Value base = ((InstanceInvokeExpr) invokeExpr).getBase();
                                if (base instanceof Local) {
                                    // 【核心升级】：一旦发现 remove，进入强大的递归跨过程追踪器！
                                    traceBaseToField((Local) base, stmt, method, removedThreadLocals);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. 规则引擎裁决
        for (Map.Entry<String, String> entry : allThreadLocals.entrySet()) {
            String fieldName = entry.getKey();
            String genericType = entry.getValue();
            boolean hasRemove = removedThreadLocals.contains(fieldName);
            int realLine = threadLocalLines.getOrDefault(fieldName, 0);

            checkDangerousType(fieldName, genericType, sootClass.getName(), hasRemove, realLine, globalIssues);
        }
    }

    /**
     * 【全新核心】：支持跨方法的递归数据流追踪器
     */
    private static void traceBaseToField(Value targetValue, Stmt targetStmt, SootMethod currentMethod, Set<String> removedThreadLocals) {
        if (!currentMethod.isConcrete()) return;

        Body body = currentMethod.retrieveActiveBody();
        UnitGraph graph = new ExceptionalUnitGraph(body);
        LocalDefs localDefs = new SimpleLocalDefs(graph);

        if (!(targetValue instanceof Local)) return;

        List<Unit> defs = localDefs.getDefsOfAt((Local) targetValue, targetStmt);
        if (defs.isEmpty()) return;

        Unit defUnit = defs.get(0);

        // 场景 A：在当前方法内找到了直接赋值 (如：$stack0 = NORMAL_COUNTER)
        if (defUnit instanceof AssignStmt) {
            Value rightOp = ((AssignStmt) defUnit).getRightOp();
            if (rightOp instanceof StaticFieldRef) {
                SootField targetField = ((StaticFieldRef) rightOp).getField();
                removedThreadLocals.add(targetField.getName());
                System.out.println("[跨过程追踪] 成功锁定目标变量: " + targetField.getName());
            }
        }
        // 场景 B：追溯到了当前方法的参数 (如：targetLocal := @parameter0)
        else if (defUnit instanceof IdentityStmt) {
            Value rightOp = ((IdentityStmt) defUnit).getRightOp();
            if (rightOp instanceof ParameterRef) {
                ParameterRef paramRef = (ParameterRef) rightOp;
                int paramIndex = paramRef.getIndex(); // 记录这是第几个参数

                // 【逆流而上】：利用 Call Graph 寻找所有调用了当前方法的地方
                Iterator<Edge> edgesInto = Scene.v().getCallGraph().edgesInto(currentMethod);
                while (edgesInto.hasNext()) {
                    Edge edge = edgesInto.next();
                    SootMethod callerMethod = edge.src(); // 谁调用的？
                    Stmt callSite = edge.srcStmt();       // 在哪行代码调用的？

                    if (callSite != null && callSite.containsInvokeExpr()) {
                        // 提取调用时传入的真实参数
                        Value argPassed = callSite.getInvokeExpr().getArg(paramIndex);

                        // 递归：回到父方法，继续追踪这个传入的参数
                        traceBaseToField(argPassed, callSite, callerMethod, removedThreadLocals);
                    }
                }
            }
        }
    }

    private static int findFieldInitLineNumber(SootClass sootClass, SootField targetField) {
        for (SootMethod method : new ArrayList<>(sootClass.getMethods())) {
            if (method.getName().equals("<clinit>") || method.getName().equals("<init>")) {
                if (!method.isConcrete()) continue;

                Body body = method.retrieveActiveBody();
                for (Unit unit : body.getUnits()) {
                    if (unit instanceof AssignStmt) {
                        Value leftOp = ((AssignStmt) unit).getLeftOp();
                        if (leftOp instanceof FieldRef) {
                            SootField refField = ((FieldRef) leftOp).getField();
                            if (refField.getSignature().equals(targetField.getSignature())) {
                                LineNumberTag tag = (LineNumberTag) unit.getTag("LineNumberTag");
                                if (tag != null) {
                                    return tag.getLineNumber();
                                }
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }

    private static void checkDangerousType(String fieldName, String typeStr, String className, boolean hasSpecificRemoveCall, int line, List<DetectionIssue> issues) {
        String lowerType = typeStr.toLowerCase();
        String displayStr = fieldName + " (" + typeStr + ")";

        if (hasSpecificRemoveCall) {
            return;
        }

        // 1. 集合类与常用大对象
        if (lowerType.matches(".*\\b(list|map|set|hashmap|arraylist|hashset)\\b.*")) {
            issues.add(new DetectionIssue("类型 I (集合膨胀)", "检测到动态集合 [" + displayStr + "] 挂载于 ThreadLocal，未执行 remove()，存在无界膨胀 OOM 风险。", className, line));
        } else if (lowerType.contains("stringbuilder") || lowerType.contains("stringbuffer")) {
            issues.add(new DetectionIssue("类型 I (大对象滞留)", "检测到动态膨胀字符序列 [" + displayStr + "]，未检测到 remove() 动作，易导致老年代耗尽。", className, line));
        }
        // 2. 重量级 JDK 工具
        else if (lowerType.contains("simpledateformat") || lowerType.contains("collator") || lowerType.contains("decimalformat") || lowerType.contains("numberformat")) {
            issues.add(new DetectionIssue("类型 I (重量级组件)", "检测到重量级/格式化组件 [" + displayStr + "]，缺失清理动作易导致内存缓慢耗尽或底层 ClassLoader 泄漏。", className, line));
        } else if (lowerType.contains("memorystack")) {
            issues.add(new DetectionIssue("类型 I (堆外内存)", "检测到直接内存分配器 [" + displayStr + "]，缺失清理会导致 JNI 堆外物理内存耗尽。", className, line));
        }
        // 3. 【新增】：序列化、I/O流与自定义缓冲区 (匹配 FSTObjectOutput)
        else if (lowerType.contains("output") || lowerType.contains("stream") || lowerType.contains("buffer") || lowerType.contains("codec") || lowerType.contains("serializer")) {
            issues.add(new DetectionIssue("类型 I (序列化/缓冲滞留)", "危险！检测到序列化或流缓冲对象 [" + displayStr + "]。此类对象底层常绑定大型 byte[] 数组，缺失 remove() 极易在处理大报文时引发严重的 OOM (如 Dubbo FST 漏洞)。", className, line));
        }
        // 4. 【新增】：激进模式兜底 (可选) - 任何非 JDK 核心类且没有 remove 的，都提示 Warning
        else if (!lowerType.startsWith("java.") && !lowerType.startsWith("javax.") && !lowerType.equals("unknown")) {
            issues.add(new DetectionIssue("类型 I (未知自定义对象风险)", "警告：检测到自定义对象 [" + displayStr + "] 挂载于 ThreadLocal 且未主动 remove()。若该对象包含大数组或复杂树结构，长期存活于线程池将导致内存泄漏。", className, line));
        }
    }

    private static boolean isThreadLocalType(SootField field) {
        String typeName = field.getType().toString();
        return typeName.equals("java.lang.ThreadLocal") || typeName.equals("io.netty.util.concurrent.FastThreadLocal");
    }

    private static String extractGenericType(SootField field) {
        for (Tag tag : field.getTags()) {
            if (tag instanceof SignatureTag) {
                String signature = ((SignatureTag) tag).getSignature();
                Pattern pattern = Pattern.compile("<L(.*?);>");
                Matcher matcher = pattern.matcher(signature);
                if (matcher.find()) {
                    // 1. 获取初步提取的带有 / 的字符串
                    String rawType = matcher.group(1).replace('/', '.');

                    // 2. 【核心修复】：剥离内部嵌套的泛型参数！
                    // 例如把 "TypeReference<Ljava.util.HashMap..." 截断，只保留 "TypeReference"
                    int angleBracketIndex = rawType.indexOf('<');
                    if (angleBracketIndex != -1) {
                        rawType = rawType.substring(0, angleBracketIndex);
                    }

                    return rawType;
                }
            }
        }
        return "Unknown";
    }
}