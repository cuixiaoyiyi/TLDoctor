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

        
        cu.findAll(ObjectCreationExpr.class).forEach(expr -> {
            boolean isThreadLocal = expr.getType().getNameAsString().equals("ThreadLocal");
            if (isThreadLocal && expr.getAnonymousClassBody().isPresent()) {
                int line = expr.getBegin().isPresent() ? expr.getBegin().get().line : -1;
                issues.add(new DetectionIssue("类型 IV (直接继承)", "检测到 ThreadLocal 使用了匿名内部类实例化", filePath, line));
            }
        });

        
        cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
            String methodName = methodCall.getNameAsString();
            if (methodName.equals("set") || methodName.equals("withInitial")) {

                
                boolean isThreadLocalContext = false;
                try {
                    if (methodCall.getScope().isPresent()) {
                        ResolvedType scopeType = methodCall.getScope().get().calculateResolvedType();
                        if (scopeType.describe().startsWith("java.lang.ThreadLocal")) {
                            isThreadLocalContext = true;
                        }
                    } else if (methodName.equals("withInitial")) {
                        
                        if (methodCall.resolve().declaringType().getQualifiedName().equals("java.lang.ThreadLocal")) {
                            isThreadLocalContext = true;
                        }
                    }
                } catch (Exception e) {
                    
                    String scopeStr = methodCall.getScope().map(Expression::toString).orElse("");
                    if (scopeStr.toLowerCase().contains("threadlocal") || scopeStr.toLowerCase().contains("context")) {
                        isThreadLocalContext = true;
                    }
                }

                if (!isThreadLocalContext) return; 

                
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

        
        if (expr.isObjectCreationExpr()) {
            ObjectCreationExpr objCreate = expr.asObjectCreationExpr();

            
            if (objCreate.getAnonymousClassBody().isPresent()) {
                issues.add(new DetectionIssue("类型 IV (隐式传递)", "向 " + methodName + "() 传入了匿名内部类", filePath, line));
                return;
            }

            
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

        
        else if (expr.isNameExpr()) {
            try {
                ResolvedValueDeclaration resolvedValue = expr.asNameExpr().resolve();
                if (resolvedValue.isVariable()) {
                    
                    contextNode.findAncestor(CompilationUnit.class).ifPresent(cu -> {
                        cu.findAll(VariableDeclarator.class).stream()
                                .filter(v -> v.getNameAsString().equals(resolvedValue.getName()))
                                .findFirst()
                                .ifPresent(varDecl -> {
                                    if (varDecl.getInitializer().isPresent()) {
                                        
                                        traceAndCheckExpression(varDecl.getInitializer().get(), contextNode, methodName, filePath, issues);
                                    }
                                });
                    });
                }
            } catch (Exception e) {
                
            }
        }
    }
}