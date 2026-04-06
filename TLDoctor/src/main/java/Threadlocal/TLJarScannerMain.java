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
        // 【新增】：记录引擎启动的绝对时间戳
        long startTime = System.currentTimeMillis();
        String targetJar = "/Users/qiuyucheng/Desktop/Threadlocal_Tool/src/main/java/influxdb-java-2.26-SNAPSHOT.jar";
        initSoot(targetJar);

        System.out.println("=== 开始加载类与方法体 ===");
        Scene.v().loadNecessaryClasses();

        // 【新增】：在生成 CallGraph 之前，必须告诉 Soot 哪些是入口方法
        setEntryPoints();

        // 【新增】：运行 PackManager 生成全程序的 CallGraph
        System.out.println("=== 正在构建 SPARK 调用图 (Call Graph) ===");
        PackManager.v().runPacks();

        System.out.println("=== 开始执行 ThreadLocal 泄露扫描 ===");
        runScanner();
        // 【新增】：记录引擎结束时间，并计算总耗时
        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;
        double durationSec = durationMs / 1000.0;
        System.out.println("\n⏱️ [性能看板] 扫描分析完成！总耗时: " + durationSec + " 秒 (" + durationMs + " ms)\n");
    }

    private static void initSoot(String jarPath) {
        soot.G.reset();
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_process_dir(Collections.singletonList(jarPath));
        Options.v().set_whole_program(true); // 全程序模式是生成 CG 的前提
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_keep_line_number(true);
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().set_no_bodies_for_excluded(true);

        // 【新增】：开启 SPARK 指针分析与调用图构建
        Options.v().setPhaseOption("cg.spark", "on");
        Options.v().set_exclude(Collections.singletonList("java.*"));
        Options.v().set_exclude(Collections.singletonList("javax.*"));
        Options.v().set_exclude(Collections.singletonList("sun.*"));
    }

    /**
     * 为普通的 JAR 包设置分析入口点
     * 简单起见，我们将所有业务类中的 public 方法都视作潜在的外部调用入口
     */
    private static void setEntryPoints() {
        List<SootMethod> entryMethods = new ArrayList<>();

        // 遍历所有应用类
        for (SootClass sootClass : new ArrayList<>(Scene.v().getApplicationClasses())) {

            // 遍历类中的所有方法 (使用快照防并发修改)
            for (SootMethod method : new ArrayList<>(sootClass.getMethods())) {

                // 排除私有方法 (外界无法直接调用)，排除抽象方法
                if (!method.isPrivate() && method.isConcrete() && !method.isAbstract()) {
                    entryMethods.add(method);
                }
            }
        }

        // 将这些方法设置为 Soot SPARK 分析的虚拟起点
        Scene.v().setEntryPoints(entryMethods);
        System.out.println(">>> 成功提取虚拟入口点数量: " + entryMethods.size());
    }

    private static void runScanner() {
        List<DetectionIssue> allIssues = new ArrayList<>();

        // 【测试 CallGraph 是否构建成功】
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

        // 依次执行四种检测器
        for (SootClass sootClass : new ArrayList<>(Scene.v().getApplicationClasses())) {
            SootTypeIDetector.analyzeClass(sootClass, allIssues);
            SootTypeIIDetector.analyzeClass(sootClass, allIssues);
            SootTypeIIIDetector.analyzeClass(sootClass, allIssues);
            SootTypeIVDetector.analyzeClass(sootClass, allIssues);
        }

        // ==========================================================
        // 【新增】：高危覆盖抑制机制 (Suppression) - 完美适配版
        // 策略：如果某行代码触发了 Type IV (隐式传递后台任务)，则判定该处不该执行 remove，
        // 从而抹除由于缺少 remove 引发的 Type III 误报。
        // ==========================================================

        // 1. 收集所有触发了 Type IV 报警的代码坐标 (filePath:lineNumber)
        Set<String> type4Locations = new HashSet<>();
        for (DetectionIssue issue : allIssues) {
            if (issue.type != null && issue.type.contains("类型 IV")) {
                // 直接访问 public 字段 filePath 和 lineNumber
                String locationKey = issue.filePath + ":" + issue.lineNumber;
                type4Locations.add(locationKey);
            }
        }

        // 2. 遍历所有报警，移除命中了上述坐标的 Type III 报警
        int suppressedCount = 0;
        Iterator<DetectionIssue> iterator = allIssues.iterator();
        while (iterator.hasNext()) {
            DetectionIssue issue = iterator.next();

            if (issue.type != null && issue.type.contains("类型 III")) {
                // 直接访问 public 字段
                String locationKey = issue.filePath + ":" + issue.lineNumber;

                // 如果这个 Type III 所在的行同时也报了 Type IV，则将其作为误报移除
                if (type4Locations.contains(locationKey)) {
                    iterator.remove();
                    suppressedCount++;
                }
            }
        }

        if (suppressedCount > 0) {
            System.out.println(">>> [智能去噪] 触发高危覆盖抑制机制，成功过滤了 " + suppressedCount + " 条语义冲突的 Type III 误报。");
        }
        // ==========================================================

        // 统一格式化输出报告
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