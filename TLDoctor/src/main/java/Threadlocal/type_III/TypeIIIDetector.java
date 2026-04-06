package Threadlocal.type_III;

import Threadlocal.common.DetectionIssue;
import Threadlocal.common.ThreadLocalDetector;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TypeIIIDetector implements ThreadLocalDetector {

    @Override
    public List<DetectionIssue> analyze(CompilationUnit cu, String filePath) {
        List<DetectionIssue> issues = new ArrayList<>();

        cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
            if (methodCall.getNameAsString().equals("set")) {

                boolean isThreadLocal = false;
                String scopeName = "";
                if (methodCall.getScope().isPresent()) {
                    scopeName = methodCall.getScope().get().toString();
                    try {
                        ResolvedType scopeType = methodCall.getScope().get().calculateResolvedType();
                        if (scopeType.describe().startsWith("java.lang.ThreadLocal") ||
                                scopeType.describe().startsWith("java.lang.InheritableThreadLocal")) {
                            isThreadLocal = true;
                        }
                    } catch (Exception e) {
                        if (scopeName.toLowerCase().contains("threadlocal") || scopeName.toLowerCase().contains("context") || scopeName.toLowerCase().contains("key")) {
                            isThreadLocal = true;
                        }
                    }
                }

                if (!isThreadLocal) return;

                boolean isProtected = false;

                
                Node current = methodCall;
                while (current != null) {
                    if (current instanceof TryStmt) {
                        if (hasUnconditionalRemove((TryStmt) current, scopeName)) {
                            isProtected = true;
                            break;
                        }
                    }
                    current = current.getParentNode().orElse(null);
                }

                
                if (!isProtected) {
                    Optional<ExpressionStmt> exprStmtOpt = methodCall.findAncestor(ExpressionStmt.class);
                    if (exprStmtOpt.isPresent()) {
                        ExpressionStmt exprStmt = exprStmtOpt.get();
                        Optional<BlockStmt> parentBlockOpt = exprStmt.findAncestor(BlockStmt.class);
                        if (parentBlockOpt.isPresent()) {
                            BlockStmt parentBlock = parentBlockOpt.get();
                            int index = parentBlock.getStatements().indexOf(exprStmt);
                            
                            if (index >= 0 && index + 1 < parentBlock.getStatements().size()) {
                                Statement nextStmt = parentBlock.getStatements().get(index + 1);
                                if (nextStmt.isTryStmt()) {
                                    if (hasUnconditionalRemove(nextStmt.asTryStmt(), scopeName)) {
                                        isProtected = true;
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isProtected) {
                    int line = methodCall.getBegin().isPresent() ? methodCall.getBegin().get().line : -1;
                    String severity = "严重";
                    String detail = "上下文状态残留 (参考 Caffeine #1944)";
                    if (scopeName.toLowerCase().contains("context") || scopeName.toLowerCase().contains("user") || scopeName.toLowerCase().contains("tenant")) {
                        severity = "极危";
                        detail = "极其严重的身份污染与越权访问风险 (参考 DBFlute #8)";
                    } else if (scopeName.toLowerCase().contains("key") || scopeName.toLowerCase().contains("password") || scopeName.toLowerCase().contains("credential")) {
                        severity = "极危 (安全漏洞)";
                        detail = "极其严重的明文凭证/密码内存泄漏风险 (参考 Apache POI #1015)";
                    }

                    issues.add(new DetectionIssue(
                            "类型 III (上下文污染)",
                            "[" + severity + "] ThreadLocal.set() 缺乏必然执行的 remove() 保护（未被 Try-Finally 包裹，或 Finally 中属于条件执行）。异常中断将导致" + detail + "。",
                            filePath, line));
                }
            }
        });

        return issues;
    }

    /**
     * 【升级 3：必然执行校验】
     * 检查 TryStmt 的 finally 块中，是否存在“无条件执行”的 remove()
     */
    private boolean hasUnconditionalRemove(TryStmt tryStmt, String targetScope) {
        if (!tryStmt.getFinallyBlock().isPresent()) return false;
        BlockStmt finallyBlock = tryStmt.getFinallyBlock().get();

        List<MethodCallExpr> removeCalls = finallyBlock.findAll(MethodCallExpr.class, mc ->
                mc.getNameAsString().equals("remove") &&
                        mc.getScope().isPresent() &&
                        mc.getScope().get().toString().equals(targetScope)
        );

        if (removeCalls.isEmpty()) return false;

        
        for (MethodCallExpr removeCall : removeCalls) {
            boolean isConditional = false;
            Node current = removeCall;
            while (current != null && current != finallyBlock) {
                if (current instanceof IfStmt || current instanceof ForStmt ||
                        current instanceof WhileStmt || current instanceof SwitchStmt ||
                        current instanceof DoStmt) {
                    isConditional = true;
                    break;
                }
                current = current.getParentNode().orElse(null);
            }
            if (!isConditional) {
                return true; 
            }
        }
        return false;
    }
}