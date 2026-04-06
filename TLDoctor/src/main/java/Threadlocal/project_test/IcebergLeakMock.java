package Threadlocal.project_test;

import java.util.Collections;
import java.util.Map;

/**
 * 模拟 Apache Iceberg #15284 伪清理导致的容量泄漏 (由 QiuYucheng 报告)
 * 核心特征：在 finally 块中使用了 .set(emptyMap) 替代 .remove()。
 */
public class IcebergLeakMock {

    
    private static final ThreadLocal<Map<String, String>> COMMIT_PROPERTIES =
            ThreadLocal.withInitial(Collections::emptyMap);

    
    public static void withCommitProperties(Map<String, String> properties, Runnable task) {
        
        COMMIT_PROPERTIES.set(properties);

        try {
            
            task.run();
        } finally {
            
            
            COMMIT_PROPERTIES.set(Collections.emptyMap());

            
            
        }
    }

    public static void main(String[] args) {
        withCommitProperties(Collections.singletonMap("spark.app.id", "app-123"), () -> {
            System.out.println("Executing Iceberg commit...");
        });
    }
}