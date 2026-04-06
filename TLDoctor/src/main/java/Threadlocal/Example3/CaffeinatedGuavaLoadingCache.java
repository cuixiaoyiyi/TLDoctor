package Threadlocal.Example3;

/**
 * 还原 Caffeine Cache (Issue #1944) 的 Type III 漏洞真实场景
 * 跨请求状态污染 (Context Pollution)
 */
public class CaffeinatedGuavaLoadingCache {

    // 【危险源】：用于在批量加载时，向底层回调传递“允许 null 值”的信号
    // 注意：泛型是 Boolean，属于 JDK 基础类。
    private static final ThreadLocal<Boolean> nullBulkLoad = new ThreadLocal<>();

    /**
     * 模拟 Guava Cache 的 getAll 批量获取逻辑
     */
    public Object getAll(Iterable<String> keys) {

        // 【漏洞核心 1：状态挂载】
        // 在批量加载前，将当前工作线程的 ThreadLocal 标记为 true
        nullBulkLoad.set(true);

        // 模拟调用底层的批量加载器。
        // 在 JVM 和 Soot 的眼中，任何方法调用都有可能抛出 RuntimeException（系统异常）。
        internalBulkLoader(keys);

        // 【漏洞核心 2：致命的非结构化清理】
        // 开发者试图在方法末尾重置状态，但这行代码并没有被包裹在 finally 块中！
        // 如果上面的 internalBulkLoader 抛出了异常，这行代码将被直接跳过。
        // 结果：当前 Tomcat 线程带着 nullBulkLoad = true 的脏数据回到了线程池。
        // 下一个无辜的用户请求复用这个线程时，缓存逻辑将发生严重错乱！
        nullBulkLoad.remove();

        return new Object();
    }

    /**
     * 内部加载逻辑（模拟可能发生的业务异常）
     */
    private void internalBulkLoader(Iterable<String> keys) {
        if (keys == null) {
            // 模拟加载失败抛出异常，这将切断 getAll 方法的正常控制流
            throw new RuntimeException("InvalidCacheLoadException: keys is null");
        }
        System.out.println("Executing bulk load...");
    }
}