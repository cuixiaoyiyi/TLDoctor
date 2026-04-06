package Threadlocal.project_test;

import java.util.Collections;
import java.util.Map;

/**
 * 模拟 Apache Iceberg #15284 伪清理导致的容量泄漏 (由 QiuYucheng 报告)
 * 核心特征：在 finally 块中使用了 .set(emptyMap) 替代 .remove()。
 */
public class IcebergLeakMock {

    // 模拟 Iceberg 中的 CommitMetadata.COMMIT_PROPERTIES
    private static final ThreadLocal<Map<String, String>> COMMIT_PROPERTIES =
            ThreadLocal.withInitial(Collections::emptyMap);

    // 模拟带着元数据提交任务的业务方法
    public static void withCommitProperties(Map<String, String> properties, Runnable task) {
        // 1. 注入上下文
        COMMIT_PROPERTIES.set(properties);

        try {
            // 2. 执行 Spark 提交任务
            task.run();
        } finally {
            // 3. 【致命伪清理】开发者试图清理上下文，但用错了方法！
            // 导致 ThreadLocalMap 中的 Entry 槽位被永久占用
            COMMIT_PROPERTIES.set(Collections.emptyMap());

            // 正确的做法应该是：
            // COMMIT_PROPERTIES.remove();
        }
    }

    public static void main(String[] args) {
        withCommitProperties(Collections.singletonMap("spark.app.id", "app-123"), () -> {
            System.out.println("Executing Iceberg commit...");
        });
    }
}