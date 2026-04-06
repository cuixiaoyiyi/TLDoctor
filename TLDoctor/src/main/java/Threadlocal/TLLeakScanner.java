package Threadlocal;

import Threadlocal.common.DetectionIssue;
import Threadlocal.common.ThreadLocalDetector;
import Threadlocal.type_I.TypeIDetector;
import Threadlocal.type_II.TypeIIDetector;
import Threadlocal.type_III.TypeIIIDetector;
import Threadlocal.type_IV.TypeIVDetector;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;


public class TLLeakScanner {
    public static void main(String[] args) {
        
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        
        combinedTypeSolver.add(new ReflectionTypeSolver());
        
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main/java")));

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

        
        File targetJavaFile = new File("src/main/java/Threadlocal/Example3/DummyTest.java");

        List<ThreadLocalDetector> detectors = new ArrayList<>();
        detectors.add(new TypeIVDetector());
        detectors.add(new TypeIDetector());
        detectors.add(new TypeIIDetector());
        detectors.add(new TypeIIIDetector());

        List<DetectionIssue> allIssues = new ArrayList<>();

        try {
            if (!targetJavaFile.exists()) {
                System.out.println("找不到目标文件: " + targetJavaFile.getAbsolutePath());
                return;
            }

            CompilationUnit cu = StaticJavaParser.parse(targetJavaFile);

            for (ThreadLocalDetector detector : detectors) {
                allIssues.addAll(detector.analyze(cu, targetJavaFile.getAbsolutePath()));
            }

            System.out.println("=== ThreadLocal 误用检测报告 ===");
            if (allIssues.isEmpty()) {
                System.out.println("恭喜！未检测到已知的 ThreadLocal 误用。");
            } else {
                for (DetectionIssue issue : allIssues) {
                    System.out.println(issue.toString());
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}