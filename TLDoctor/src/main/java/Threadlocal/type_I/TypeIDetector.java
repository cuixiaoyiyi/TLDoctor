package Threadlocal.type_I;

import Threadlocal.common.DetectionIssue;
import Threadlocal.common.ThreadLocalDetector;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.List;

public class TypeIDetector implements ThreadLocalDetector {

    @Override
    public List<DetectionIssue> analyze(CompilationUnit cu, String filePath) {
        List<DetectionIssue> issues = new ArrayList<>();

        
        boolean hasGlobalCapacityCheck = !cu.findAll(MethodCallExpr.class, mc -> mc.getNameAsString().equals("capacity")).isEmpty();
        boolean hasGlobalClearCall = !cu.findAll(MethodCallExpr.class, mc -> mc.getNameAsString().equals("clear")).isEmpty();

        cu.findAll(VariableDeclarator.class).forEach(varDecl -> {
            if (varDecl.getType().isClassOrInterfaceType()) {
                ClassOrInterfaceType type = varDecl.getType().asClassOrInterfaceType();

                if (type.getNameAsString().equals("ThreadLocal") || type.getNameAsString().endsWith(".ThreadLocal")) {

                    String varName = varDecl.getNameAsString(); 

                    
                    boolean hasSpecificRemoveCall = !cu.findAll(MethodCallExpr.class, mc ->
                            mc.getNameAsString().equals("remove") &&
                                    mc.getScope().isPresent() &&
                                    mc.getScope().get().toString().equals(varName)
                    ).isEmpty();

                    type.getTypeArguments().ifPresent(typeArgs -> {
                        if (!typeArgs.isEmpty()) {
                            String genericTypeName = typeArgs.get(0).toString();
                            int line = varDecl.getBegin().isPresent() ? varDecl.getBegin().get().line : -1;

                            boolean isDetected = checkDangerousType(genericTypeName, genericTypeName, filePath, line, issues, hasGlobalCapacityCheck, hasSpecificRemoveCall, hasGlobalClearCall);

                            if (!isDetected) {
                                try {
                                    ResolvedType resolvedType = typeArgs.get(0).resolve();
                                    if (resolvedType.isReferenceType()) {
                                        for (ResolvedFieldDeclaration field : resolvedType.asReferenceType().getDeclaredFields()) {
                                            String fieldTypeStr = field.getType().describe();
                                            String displayStr = genericTypeName + " 的成员变量 " + field.getName() + " (" + fieldTypeStr + ")";

                                            
                                            checkDangerousType(fieldTypeStr, displayStr, filePath, line, issues, hasGlobalCapacityCheck, hasSpecificRemoveCall, hasGlobalClearCall);
                                        }
                                    }
                                } catch (Exception e) {
                                    
                                }
                            }
                        }
                    });
                }
            }
        });

        return issues;
    }

    private boolean checkDangerousType(String typeStr, String displayStr, String filePath, int line, List<DetectionIssue> issues, boolean hasCapacityCheck, boolean hasSpecificRemoveCall, boolean hasClearCall) {
        String lowerType = typeStr.toLowerCase();

        if (lowerType.contains("stringbuilder") || lowerType.contains("stringbuffer")) {
            if (hasCapacityCheck && hasSpecificRemoveCall) {
                return true; 
            }
            issues.add(new DetectionIssue("类型 I (大对象滞留)", "检测到动态膨胀对象 [" + displayStr + "]，当前类未检测到容量防御校验或 remove()，易导致老年代耗尽。", filePath, line));
            return true;
        } else if (lowerType.contains("simpledateformat")) {
            issues.add(new DetectionIssue("类型 I (大对象滞留)", "检测到重量级解析器 [" + displayStr + "]，易导致堆内存缓慢耗尽。", filePath, line));
            return true;
        } else if (lowerType.contains("collator")) {
            issues.add(new DetectionIssue("类型 I (大对象滞留)", "检测到重量级比较器 [" + displayStr + "]，易导致堆内存与元空间堆积。", filePath, line));
            return true;
        } else if (lowerType.contains("memorystack")) {
            issues.add(new DetectionIssue("类型 I (大对象滞留)", "检测到直接内存分配器 [" + displayStr + "]，缺失清理会导致 JNI 堆外物理内存耗尽。", filePath, line));
            return true;
        } else if (lowerType.matches(".*\\b(list|map|set|hashmap|arraylist|hashset)\\b.*")) {
            if (!hasSpecificRemoveCall && !hasClearCall) {
                issues.add(new DetectionIssue("类型 I (集合膨胀)", "检测到动态集合 [" + displayStr + "] 挂载于 ThreadLocal，且未执行专属的 remove() 或 clear()，存在无界膨胀 OOM 风险。", filePath, line));
                return true;
            }
        }
        return false;
    }
}