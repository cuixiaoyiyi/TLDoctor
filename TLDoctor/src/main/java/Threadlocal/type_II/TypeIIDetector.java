package Threadlocal.type_II;

import Threadlocal.common.DetectionIssue;
import Threadlocal.common.ThreadLocalDetector;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.List;

public class TypeIIDetector implements ThreadLocalDetector {

    @Override
    public List<DetectionIssue> analyze(CompilationUnit cu, String filePath) {
        List<DetectionIssue> issues = new ArrayList<>();

        // =========================================================================
        // 规则 1: 捕获静态 ThreadLocal (Static ThreadLocal Abuse) [消除文本匹配的误报与漏报]
        // =========================================================================
        cu.findAll(FieldDeclaration.class).forEach(fieldDecl -> {
            if (fieldDecl.isStatic()) {
                fieldDecl.getVariables().forEach(varDecl -> {
                    boolean isThreadLocal = false;
                    try {
                        // 【核心升级】：解析真实的类声明类型
                        ResolvedType resolvedType = varDecl.getType().resolve();
                        String typeDesc = resolvedType.describe();
                        if (typeDesc.startsWith("java.lang.ThreadLocal") || typeDesc.startsWith("java.lang.InheritableThreadLocal")) {
                            isThreadLocal = true;
                        }
                    } catch (Exception e) {
                        // 容错降级：如果符号解析失败，退化为文本匹配
                        String typeName = varDecl.getType().asString();
                        if (typeName.contains("ThreadLocal")) {
                            isThreadLocal = true;
                        }
                    }

                    if (isThreadLocal) {
                        int line = fieldDecl.getBegin().isPresent() ? fieldDecl.getBegin().get().line : -1;
                        String varName = varDecl.getNameAsString();
                        issues.add(new DetectionIssue(
                                "类型 II (类加载器锁定)",
                                "检测到全局静态 ThreadLocal [" + varName + "]。在 Web 容器或 OSGi 环境中，静态 ThreadLocal 会与应用类加载器生命周期强绑定，热部署时极易引发 Metaspace OOM。",
                                filePath, line));
                    }
                });
            }
        });

        // =========================================================================
        // 规则 2: 捕获伪清理 set(null) (The set(null) Fallacy) [击穿变量命名伪装]
        // =========================================================================
        cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
            if (methodCall.getNameAsString().equals("set")) {
                List<Expression> args = methodCall.getArguments();
                if (args.size() == 1 && args.get(0).isNullLiteralExpr()) {

                    boolean isActualThreadLocal = false;
                    try {
                        // 【核心升级】：解析调用 set(null) 的那个对象到底是不是 ThreadLocal 家族的
                        if (methodCall.getScope().isPresent()) {
                            ResolvedType scopeType = methodCall.getScope().get().calculateResolvedType();
                            String typeDesc = scopeType.describe();
                            if (typeDesc.startsWith("java.lang.ThreadLocal") || typeDesc.startsWith("java.lang.InheritableThreadLocal")) {
                                isActualThreadLocal = true;
                            }
                        }
                    } catch (Exception e) {
                        // 容错降级
                        String scopeStr = methodCall.getScope().map(Expression::toString).orElse("").toLowerCase();
                        if (scopeStr.contains("threadlocal") || scopeStr.contains("context") || scopeStr.contains("provider")) {
                            isActualThreadLocal = true;
                        }
                    }

                    if (isActualThreadLocal) {
                        int line = methodCall.getBegin().isPresent() ? methodCall.getBegin().get().line : -1;
                        issues.add(new DetectionIssue(
                                "类型 II (类加载器泄漏 - 伪清理)",
                                "严重警告：检测到使用 set(null) 进行状态清理。这无法移除底层 Entry 节点，会导致陈旧节点残留并锁定 WebappClassLoader，必须替换为 remove() (参考 Togglz #1344)。",
                                filePath, line));
                    }
                }
            }
        });

        return issues;
    }
}