package Threadlocal;

import Threadlocal.common.DetectionIssue;
import soot.Body;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SignatureTag;
import soot.tagkit.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SootTypeIIDetector {

    public static void analyzeClass(SootClass sootClass, List<DetectionIssue> globalIssues) {
        if (sootClass.isInterface() || sootClass.isPhantom()) {
            return;
        }

        // =========================================================================
        // 规则 1: 扫描静态 ThreadLocal (类加载器强绑定风险) - 【已搭载降噪引擎】
        // =========================================================================
        for (SootField field : sootClass.getFields()) {
            if (field.isStatic() && isThreadLocalType(field.getType().toString())) {

                // 【核心升级】：提取真实泛型类型
                String genericType = extractGenericType(field);

                // 【降噪过滤】：如果是 JDK 基础类，绝对不会引起 ClassLoader 泄漏，安全放行！
                if (!isJdkCoreClass(genericType)) {
                    int line = findFieldInitLineNumber(sootClass, field);
                    globalIssues.add(new DetectionIssue(
                            "类型 II (类加载器锁定)",
                            "检测到全局静态 ThreadLocal [" + field.getName() + " (泛型: " + genericType + ")]。在 Web 容器中，挂载自定义类会导致应用类加载器生命周期强绑定，热部署时引发 Metaspace OOM。",
                            sootClass.getName(), line));
                }
            }
        }

        // =========================================================================
        // 规则 2: 扫描 set(null) 伪清理 (陈旧节点残留风险) - 【已搭载去重引擎】
        // =========================================================================
        Set<String> reportedSetNulls = new HashSet<>();

        for (SootMethod method : new ArrayList<>(sootClass.getMethods())) {
            if (!method.isConcrete()) continue;

            Body body = method.retrieveActiveBody();
            for (Unit unit : body.getUnits()) {
                if (unit instanceof Stmt) {
                    Stmt stmt = (Stmt) unit;
                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr invokeExpr = stmt.getInvokeExpr();
                        SootMethod invokedMethod = invokeExpr.getMethod();

                        if (isThreadLocalType(invokedMethod.getDeclaringClass().getName())
                                && invokedMethod.getName().equals("set")) {

                            if (invokeExpr.getArgCount() == 1) {
                                Value arg = invokeExpr.getArg(0);

                                if (arg instanceof NullConstant) {
                                    int line = getLineNumber(unit);
                                    String dedupKey = method.getName() + ":" + line;

                                    if (!reportedSetNulls.contains(dedupKey)) {
                                        reportedSetNulls.add(dedupKey);

                                        globalIssues.add(new DetectionIssue(
                                                "类型 II (类加载器泄漏 - 伪清理)",
                                                "严重警告：检测到使用 set(null) 进行状态清理 (所在方法: " + method.getName() + ")。这无法移除底层 Entry 节点，会导致陈旧节点残留并锁定 WebappClassLoader，必须替换为 remove()。",
                                                sootClass.getName(), line));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isThreadLocalType(String typeName) {
        return typeName.equals("java.lang.ThreadLocal") ||
                typeName.equals("java.lang.InheritableThreadLocal") ||
                typeName.equals("io.netty.util.concurrent.FastThreadLocal");
    }

    /**
     * 判断是否为 JDK 基础类库（不会引发 WebappClassLoader 泄漏）
     */
    private static boolean isJdkCoreClass(String typeName) {
        if (typeName.equals("Unknown")) return false; // 如果解析失败，保守起见视为高危自定义类

        // 【核心升级】：剥夺危险 JDK 类的豁免权！
        // 诸如 DecimalFormat, SimpleDateFormat, NumberFormat 底层会缓存包含 ClassLoader 强引用的符号表
        String lowerName = typeName.toLowerCase();
        if (lowerName.contains("decimalformat") || lowerName.contains("numberformat") || lowerName.contains("simpledateformat")) {
            return false; // 判定为高危类，交给检测引擎报警！
        }

        return typeName.startsWith("java.") ||
                typeName.startsWith("javax.") ||
                typeName.startsWith("sun.") ||
                typeName.startsWith("jdk.");
    }

    /**
     * 从 SignatureTag 中抠出真实泛型类型
     */
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

    private static int getLineNumber(Unit unit) {
        LineNumberTag tag = (LineNumberTag) unit.getTag("LineNumberTag");
        return tag != null ? tag.getLineNumber() : 0;
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
                                return getLineNumber(unit);
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }
}