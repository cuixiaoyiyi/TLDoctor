package Threadlocal.type_IV;

import Threadlocal.common.DetectionIssue;
import Threadlocal.common.ThreadLocalDetector;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TypeIVDetector implements ThreadLocalDetector {

    @Override
    public List<DetectionIssue> analyze(CompilationUnit cu, String filePath) {
        List<DetectionIssue> issues = new ArrayList<>();

        // 规则 1 保持不变 (检测直接继承的匿名内部类)
        cu.findAll(ObjectCreationExpr.class).forEach(expr -> {
            boolean isThreadLocal = expr.getType().getNameAsString().equals("ThreadLocal");
            if (isThreadLocal && expr.getAnonymousClassBody().isPresent()) {
                int line = expr.getBegin().isPresent() ? expr.getBegin().get().line : -1;
                issues.add(new DetectionIssue("类型 IV (直接继承)", "检测到 ThreadLocal 使用了匿名内部类实例化", filePath, line));
            }
        });

        // 规则 2: 深度上下文与数据流分析
        cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
            String methodName = methodCall.getNameAsString();
            if (methodName.equals("set") || methodName.equals("withInitial")) {

                // 【核心优化 1：消除误报】确认调用这个方法的对象，到底是不是 ThreadLocal
                boolean isThreadLocalContext = false;
                try {
                    if (methodCall.getScope().isPresent()) {
                        ResolvedType scopeType = methodCall.getScope().get().calculateResolvedType();
                        if (scopeType.describe().startsWith("java.lang.ThreadLocal")) {
                            isThreadLocalContext = true;
                        }
                    } else if (methodName.equals("withInitial")) {
                        // 静态方法调用 ThreadLocal.withInitial
                        if (methodCall.resolve().declaringType().getQualifiedName().equals("java.lang.ThreadLocal")) {
                            isThreadLocalContext = true;
                        }
                    }
                } catch (Exception e) {
                    // 容错处理：如果符号解析失败（例如缺少第三方依赖），退化为启发式字符串匹配
                    String scopeStr = methodCall.getScope().map(Expression::toString).orElse("");
                    if (scopeStr.toLowerCase().contains("threadlocal") || scopeStr.toLowerCase().contains("context")) {
                        isThreadLocalContext = true;
                    }
                }

                if (!isThreadLocalContext) return; // 不是 ThreadLocal 的调用，直接放过，消除误报！

                // 开始分析传入的参数
                for (Expression arg : methodCall.getArguments()) {
                    traceAndCheckExpression(arg, methodCall, methodName, filePath, issues);
                }
            }
        });

        return issues;
    }

    /**
     * 递归追踪变量并检测是否为非静态内部类
     */
    private void traceAndCheckExpression(Expression expr, MethodCallExpr contextNode, String methodName, String filePath, List<DetectionIssue> issues) {
        int line = contextNode.getBegin().isPresent() ? contextNode.getBegin().get().line : -1;

        // 场景 A: 直接 new 出来的对象
        if (expr.isObjectCreationExpr()) {
            ObjectCreationExpr objCreate = expr.asObjectCreationExpr();

            // 1. 匿名内部类
            if (objCreate.getAnonymousClassBody().isPresent()) {
                issues.add(new DetectionIssue("类型 IV (隐式传递)", "向 " + methodName + "() 传入了匿名内部类", filePath, line));
                return;
            }

            // 2. 【核心优化 2：消除漏报】具名的非静态内部类
            String className = objCreate.getType().getNameAsString();
            Optional<ClassOrInterfaceDeclaration> classDecl = contextNode.findAncestor(CompilationUnit.class)
                    .flatMap(cu -> cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                            .filter(c -> c.getNameAsString().equals(className))
                            .findFirst());

            if (classDecl.isPresent()) {
                boolean isStatic = classDecl.get().isStatic();
                boolean isInner = classDecl.get().isNestedType();
                if (isInner && !isStatic) {
                    issues.add(new DetectionIssue("类型 IV (隐式传递)", "向 " + methodName + "() 传入了非静态内部类 (" + className + ")", filePath, line));
                }
            }
        }

        // 场景 B: 【核心优化 3：消除漏报】传入的是一个变量，我们需要顺藤摸瓜找到它是在哪里被 new 出来的
        else if (expr.isNameExpr()) {
            try {
                ResolvedValueDeclaration resolvedValue = expr.asNameExpr().resolve();
                if (resolvedValue.isVariable()) {
                    // 在 AST 中反向查找这个变量的声明节点
                    contextNode.findAncestor(CompilationUnit.class).ifPresent(cu -> {
                        cu.findAll(VariableDeclarator.class).stream()
                                .filter(v -> v.getNameAsString().equals(resolvedValue.getName()))
                                .findFirst()
                                .ifPresent(varDecl -> {
                                    if (varDecl.getInitializer().isPresent()) {
                                        // 递归！检查它被赋予的初始值
                                        traceAndCheckExpression(varDecl.getInitializer().get(), contextNode, methodName, filePath, issues);
                                    }
                                });
                    });
                }
            } catch (Exception e) {
                // 解析失败时忽略
            }
        }
    }
}