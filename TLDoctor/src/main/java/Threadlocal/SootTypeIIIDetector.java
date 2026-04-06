package Threadlocal;

import Threadlocal.common.DetectionIssue;
import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Trap;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.toolkits.scalar.SimpleLocalDefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class SootTypeIIIDetector {

    public static void analyzeClass(SootClass sootClass, List<DetectionIssue> globalIssues) {
        if (sootClass.isInterface() || sootClass.isPhantom()) {
            return;
        }

        for (SootMethod method : new ArrayList<>(sootClass.getMethods())) {
            if (!method.isConcrete()) continue;

            Body body;
            try {
                body = method.retrieveActiveBody();
            } catch (Exception e) {
                continue;
            }

            ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
            LocalDefs localDefs = new SimpleLocalDefs(graph);
            Set<String> reportedLines = new HashSet<>();

            for (Unit unit : body.getUnits()) {
                if (unit instanceof Stmt) {
                    Stmt stmt = (Stmt) unit;
                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr invokeExpr = stmt.getInvokeExpr();
                        SootMethod invokedMethod = invokeExpr.getMethod();

                        if (isThreadLocalType(invokedMethod.getDeclaringClass().getName())
                                && invokedMethod.getName().equals("set")) {

                            // 【新增智能过滤】：如果 set 的参数是 null，说明这是伪清理，交给 Type II 去管，Type III 不插手！
                            if (invokeExpr.getArgCount() > 0 && invokeExpr.getArg(0) instanceof soot.jimple.NullConstant) {
                                continue;
                            }

                            if (invokeExpr instanceof InstanceInvokeExpr) {
                                Value base = ((InstanceInvokeExpr) invokeExpr).getBase();
                                if (base instanceof Local) {
                                    String tlId = getThreadLocalId((Local) base, stmt, localDefs);
                                    String tlName = extractSimpleName(tlId);

                                    boolean isSafe = checkAllPathsToExitHaveRemove(graph, unit, tlId, localDefs);

                                    if (!isSafe) {
                                        int line = getLineNumber(unit);
                                        String dedupKey = method.getName() + ":" + line;

                                        if (!reportedLines.contains(dedupKey)) {
                                            reportedLines.add(dedupKey);

                                            String severity = "严重";
                                            String detail = "上下文状态残留 (参考 Caffeine #1944)";
                                            String lower = tlName.toLowerCase();

                                            if (lower.contains("context") || lower.contains("user") || lower.contains("tenant") || lower.contains("access")) {
                                                severity = "极危";
                                                detail = "极其严重的身份污染与越权访问风险 (参考 DBFlute #8)";
                                            } else if (lower.contains("key") || lower.contains("password") || lower.contains("credential") || lower.contains("encryption")) {
                                                severity = "极危 (安全漏洞)";
                                                detail = "极其严重的明文凭证/密码内存泄漏风险 (参考 Apache POI #1015)";
                                            }

                                            globalIssues.add(new DetectionIssue(
                                                    "类型 III (脏数据/越权)",
                                                    "[" + severity + "] " + detail + "。检测到 ThreadLocal [" + tlName + "] 调用了 set()，但缺乏必然执行的 remove() 保护（未被 Try-Finally 严密包裹，抛出的异常会绕过清理）。所在方法: " + method.getName() + "()。",
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
    }

    private static boolean checkAllPathsToExitHaveRemove(ExceptionalUnitGraph graph, Unit setStmt, String targetTlId, LocalDefs localDefs) {
        Unit normalSucc = graph.getBody().getUnits().getSuccOf(setStmt);
        if (normalSucc == null) return false;

        Queue<Unit> queue = new LinkedList<>();
        Set<Unit> visited = new HashSet<>();
        queue.add(normalSucc);

        while (!queue.isEmpty()) {
            Unit curr = queue.poll();
            if (!visited.add(curr)) continue;

            if (isRemoveForThreadLocal(curr, targetTlId, localDefs)) {
                continue;
            }

            // 【核心升级】：隐式异常逃逸拦截
            // 如果遇到方法调用，且该调用不在 try-finally 中，则判定为高危逃逸！
            if (curr instanceof Stmt && ((Stmt) curr).containsInvokeExpr()) {
                InvokeExpr expr = ((Stmt) curr).getInvokeExpr();
                String targetClass = expr.getMethod().getDeclaringClass().getName();

                // 排除 ThreadLocal 自身的 api 操作
                if (!isThreadLocalType(targetClass)) {
                    if (!isCoveredByCatchAll(curr, graph.getBody())) {
                        return false; // 发现可能引发异常并绕过 remove 的逃逸调用！
                    }
                }
            }

            List<Unit> succs = graph.getSuccsOf(curr);
            if (succs.isEmpty()) {
                return false;
            }

            for (Unit succ : succs) {
                queue.add(succ);
            }
        }

        return true;
    }

    /**
     * 判断某条语句是否被编译层面的 try-finally (catch Throwable) 严密包裹
     */
    private static boolean isCoveredByCatchAll(Unit target, Body body) {
        for (Trap trap : body.getTraps()) {
            String exName = trap.getException().getName();
            // Java 的 finally 块在字节码中会生成针对 Throwable 或 Exception 的 Trap
            if (exName.equals("java.lang.Throwable") || exName.equals("java.lang.Exception")) {
                boolean inRange = false;
                for (Unit u : body.getUnits()) {
                    if (u == trap.getBeginUnit()) inRange = true;
                    if (u == trap.getEndUnit()) inRange = false; // 边界结束
                    if (inRange && u == target) return true; // 目标被成功包裹！
                }
            }
        }
        return false;
    }

    private static boolean isRemoveForThreadLocal(Unit unit, String targetTlId, LocalDefs localDefs) {
        if (!(unit instanceof Stmt)) return false;
        Stmt stmt = (Stmt) unit;
        if (!stmt.containsInvokeExpr()) return false;

        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        SootMethod method = invokeExpr.getMethod();

        if (method.getName().equals("remove") && isThreadLocalType(method.getDeclaringClass().getName())) {
            if (invokeExpr instanceof InstanceInvokeExpr) {
                Value base = ((InstanceInvokeExpr) invokeExpr).getBase();
                if (base instanceof Local) {
                    String tlId = getThreadLocalId((Local) base, stmt, localDefs);
                    return targetTlId.equals(tlId);
                }
            }
        }
        return false;
    }

    private static String getThreadLocalId(Local base, Stmt stmt, LocalDefs localDefs) {
        List<Unit> defs = localDefs.getDefsOfAt(base, stmt);
        if (defs.isEmpty()) return base.getName();
        Unit def = defs.get(0);

        if (def instanceof AssignStmt) {
            Value rightOp = ((AssignStmt) def).getRightOp();
            if (rightOp instanceof StaticFieldRef) {
                return ((StaticFieldRef) rightOp).getField().getSignature();
            } else if (rightOp instanceof InstanceFieldRef) {
                return ((InstanceFieldRef) rightOp).getField().getSignature();
            } else if (rightOp instanceof ParameterRef) {
                return "Param_" + ((ParameterRef) rightOp).getIndex();
            } else if (rightOp instanceof Local) {
                return getThreadLocalId((Local) rightOp, (Stmt) def, localDefs);
            }
        }
        return def.toString();
    }

    private static String extractSimpleName(String signature) {
        if (signature.contains("<") && signature.contains(">")) {
            int lastSpace = signature.lastIndexOf(' ');
            int closeBracket = signature.indexOf('>');
            if (lastSpace > 0 && closeBracket > lastSpace) {
                return signature.substring(lastSpace + 1, closeBracket);
            }
        }
        return signature;
    }

    private static boolean isThreadLocalType(String typeName) {
        return typeName.equals("java.lang.ThreadLocal") ||
                typeName.equals("java.lang.InheritableThreadLocal") ||
                typeName.equals("io.netty.util.concurrent.FastThreadLocal");
    }

    private static int getLineNumber(Unit unit) {
        LineNumberTag tag = (LineNumberTag) unit.getTag("LineNumberTag");
        return tag != null ? tag.getLineNumber() : 0;
    }
}