package Threadlocal;

import Threadlocal.common.DetectionIssue;
import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.toolkits.scalar.SimpleLocalDefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SootTypeIVDetector {

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

            
            LocalDefs localDefs = new SimpleLocalDefs(new ExceptionalUnitGraph(body));

            for (Unit unit : body.getUnits()) {
                if (!(unit instanceof Stmt)) continue;
                Stmt stmt = (Stmt) unit;

                
                
                
                if (stmt instanceof AssignStmt) {
                    Value rightOp = ((AssignStmt) stmt).getRightOp();
                    if (rightOp instanceof NewExpr) {
                        SootClass newClass = ((NewExpr) rightOp).getBaseType().getSootClass();

                        
                        if (newClass.hasSuperclass() && isThreadLocalType(newClass.getSuperclass().getName())) {
                            if (isLeakyInnerClass(newClass)) {
                                int line = getLineNumber(unit);
                                globalIssues.add(new DetectionIssue(
                                        "类型 IV (直接继承)",
                                        "检测到 ThreadLocal 使用了匿名内部类实例化 (" + newClass.getShortName() + ")。这将隐式持有外部类的强引用 (this$0)，导致外部类及类加载器严重泄漏。",
                                        sootClass.getName(), line));
                            }
                        }
                    }
                }

                
                
                
                if (stmt.containsInvokeExpr()) {
                    InvokeExpr invoke = stmt.getInvokeExpr();
                    SootMethod invokedMethod = invoke.getMethod();
                    String declaringClass = invokedMethod.getDeclaringClass().getName();

                    if (isThreadLocalType(declaringClass)) {
                        String methodName = invokedMethod.getName();

                        
                        if (methodName.equals("set") || methodName.equals("withInitial")) {
                            Value arg = invoke.getArg(0);

                            if (arg instanceof Local) {
                                
                                List<Unit> defs = localDefs.getDefsOfAt((Local) arg, stmt);
                                if (!defs.isEmpty() && defs.get(0) instanceof AssignStmt) {
                                    Value rightOp = ((AssignStmt) defs.get(0)).getRightOp();

                                    
                                    if (rightOp instanceof soot.jimple.DynamicInvokeExpr) {
                                        int line = getLineNumber(unit);
                                        globalIssues.add(new DetectionIssue(
                                                "类型 IV (Lambda/闭包隐式传递)",
                                                "检测到向 ThreadLocal." + methodName + "() 传入了 Lambda 表达式 (DynamicInvokeExpr)。在 Web 容器等热部署环境中，Lambda 代理类及方法引用极易隐式捕获宿主类的强引用，引发 Metaspace 泄漏。",
                                                sootClass.getName(), line));
                                        continue; 
                                    }
                                }

                                
                                SootClass leakedClass = findNewExprClass((Local) arg, stmt, localDefs, new HashSet<>());

                                if (leakedClass != null && isLeakyInnerClass(leakedClass)) {
                                    int line = getLineNumber(unit);
                                    String leakType = leakedClass.getName().matches(".*\\$[0-9]+$") ? "匿名内部类" : "非静态内部类";

                                    globalIssues.add(new DetectionIssue(
                                            "类型 IV (隐式传递)",
                                            "检测到向 ThreadLocal." + methodName + "() 传入了" + leakType + " (" + leakedClass.getShortName() + ")。该对象隐式持有外部类的强引用，将造成严重的内存与类加载器泄漏 (参考 DataDog #292)。",
                                            sootClass.getName(), line));
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private static boolean isLeakyInnerClass(SootClass c) {
        
        if (!c.getName().contains("$")) {
            return false;
        }

        
        for (soot.SootField field : c.getFields()) {
            
            if (field.getName().startsWith("this$")) {
                return true; 
            }
        }

        return false; 
    }
    private static SootClass findNewExprClass(Local var, Unit currentUnit, LocalDefs localDefs, Set<Unit> visited) {
        List<Unit> defs = localDefs.getDefsOfAt(var, currentUnit);
        if (defs.isEmpty()) return null;
        Unit def = defs.get(0);

        
        if (!visited.add(def)) return null;

        if (def instanceof AssignStmt) {
            Value right = ((AssignStmt) def).getRightOp();
            if (right instanceof NewExpr) {
                return ((NewExpr) right).getBaseType().getSootClass();
            } else if (right instanceof Local) {
                return findNewExprClass((Local) right, def, localDefs, visited); 
            } else if (right instanceof CastExpr) {
                Value inner = ((CastExpr) right).getOp();
                if (inner instanceof Local) {
                    return findNewExprClass((Local) inner, def, localDefs, visited); 
                }
            }
        }
        return null;
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