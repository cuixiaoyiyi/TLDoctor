package Threadlocal;

import Threadlocal.common.DetectionIssue;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.File;
import java.util.*;

public class TLJarScannerMain {

    public static void main(String[] args) {
		
		if (args.length < 1) {
        System.err.println("Usage: java -jar detector.jar <path-to-jar-file>");
        System.exit(1);
    }
    String targetJar = args[0];
	
        
        long startTime = System.currentTimeMillis();
        String targetJar = "/Users/qiuyucheng/Desktop/Threadlocal_Tool/src/main/java/influxdb-java-2.26-SNAPSHOT.jar";
        initSoot(targetJar);

        System.out.println("=== 开始加载类与方法体 ===");
        Scene.v().loadNecessaryClasses();

        
        setEntryPoints();

        
        System.out.println("=== 正在构建 SPARK 调用图 (Call Graph) ===");
        PackManager.v().runPacks();

        System.out.println("=== 开始执行 ThreadLocal 泄露扫描 ===");
        runScanner();
        
        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;
        double durationSec = durationMs / 1000.0;
        System.out.println("\n⏱️ [性能看板] 扫描分析完成！总耗时: " + durationSec + " 秒 (" + durationMs + " ms)\n");
    }

    private static void initSoot(String jarPath) {
        soot.G.reset();
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_process_dir(Collections.singletonList(jarPath));
        Options.v().set_whole_program(true); 
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_keep_line_number(true);
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().set_no_bodies_for_excluded(true);

        
        Options.v().setPhaseOption("cg.spark", "on");
        Options.v().set_exclude(Collections.singletonList("java.*"));
        Options.v().set_exclude(Collections.singletonList("javax.*"));
        Options.v().set_exclude(Collections.singletonList("sun.*"));
    }

    private static void setEntryPoints() {
        List<SootMethod> entryMethods = new ArrayList<>();

        
        for (SootClass sootClass : new ArrayList<>(Scene.v().getApplicationClasses())) {

            
            for (SootMethod method : new ArrayList<>(sootClass.getMethods())) {

                
                if (!method.isPrivate() && method.isConcrete() && !method.isAbstract()) {
                    entryMethods.add(method);
                }
            }
        }

        
        Scene.v().setEntryPoints(entryMethods);
        System.out.println(">>> 成功提取虚拟入口点数量: " + entryMethods.size());
    }

    private static void runScanner() {
        List<DetectionIssue> allIssues = new ArrayList<>();

        
        CallGraph cg = Scene.v().getCallGraph();
        SootClass dummy3 = Scene.v().getSootClassUnsafe("Threadlocal.Example1.DummyTest3");
        if (dummy3 != null) {
            SootMethod doBusinessMethod = dummy3.getMethodByNameUnsafe("doBusinessLogic");
            if (doBusinessMethod != null) {
                System.out.println("\n[CG 测试] 探查 doBusinessLogic() 的所有流出调用边：");
                Iterator<Edge> edges = cg.edgesOutOf(doBusinessMethod);
                while (edges.hasNext()) {
                    Edge edge = edges.next();
                    System.out.println("  ---> 调向了方法: " + edge.tgt().getName());
                }
            }
        }

        
        for (SootClass sootClass : new ArrayList<>(Scene.v().getApplicationClasses())) {
            SootTypeIDetector.analyzeClass(sootClass, allIssues);
            SootTypeIIDetector.analyzeClass(sootClass, allIssues);
            SootTypeIIIDetector.analyzeClass(sootClass, allIssues);
            SootTypeIVDetector.analyzeClass(sootClass, allIssues);
        }

        
        
        
        
        

        
        Set<String> type4Locations = new HashSet<>();
        for (DetectionIssue issue : allIssues) {
            if (issue.type != null && issue.type.contains("类型 IV")) {
                
                String locationKey = issue.filePath + ":" + issue.lineNumber;
                type4Locations.add(locationKey);
            }
        }

        
        int suppressedCount = 0;
        Iterator<DetectionIssue> iterator = allIssues.iterator();
        while (iterator.hasNext()) {
            DetectionIssue issue = iterator.next();

            if (issue.type != null && issue.type.contains("类型 III")) {
                
                String locationKey = issue.filePath + ":" + issue.lineNumber;

                
                if (type4Locations.contains(locationKey)) {
                    iterator.remove();
                    suppressedCount++;
                }
            }
        }

        if (suppressedCount > 0) {
            System.out.println(">>> 成功过滤了 " + suppressedCount + " 条语义冲突的 Type III 误报。");
        }
        

        
        System.out.println("\n================================================");
        System.out.println("           ThreadLocal 误用检测最终报告            ");
        System.out.println("================================================\n");

        if (allIssues.isEmpty()) {
            System.out.println("✅ 恭喜！所扫描的 Jar 包中未检测到已知的 ThreadLocal 泄露风险。");
        } else {
            System.out.println("⚠️ 发现 " + allIssues.size() + " 处潜在的 ThreadLocal 泄露风险：\n");
            for (DetectionIssue issue : allIssues) {
                System.out.println(issue.toString());
            }
        }
        System.out.println("\n================================================");
    }
}