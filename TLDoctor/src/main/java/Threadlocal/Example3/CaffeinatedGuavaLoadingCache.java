package Threadlocal.Example3;

/**
 * 还原 Caffeine Cache (Issue #1944) 的 Type III 漏洞真实场景
 * 跨请求状态污染 (Context Pollution)
 */
public class CaffeinatedGuavaLoadingCache {

    
    
    private static final ThreadLocal<Boolean> nullBulkLoad = new ThreadLocal<>();

    /**
     * 模拟 Guava Cache 的 getAll 批量获取逻辑
     */
    public Object getAll(Iterable<String> keys) {

        
        
        nullBulkLoad.set(true);

        
        
        internalBulkLoader(keys);

        
        
        
        
        
        nullBulkLoad.remove();

        return new Object();
    }

    /**
     * 内部加载逻辑（模拟可能发生的业务异常）
     */
    private void internalBulkLoader(Iterable<String> keys) {
        if (keys == null) {
            
            throw new RuntimeException("InvalidCacheLoadException: keys is null");
        }
        System.out.println("Executing bulk load...");
    }
}