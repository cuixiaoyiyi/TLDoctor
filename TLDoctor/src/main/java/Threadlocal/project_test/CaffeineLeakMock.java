package Threadlocal.project_test;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟 Caffeine #1944 状态残留漏洞 (由 QiuYucheng 报告)
 * 核心特征：使用 ThreadLocal 作为旁路信号，但在异常路径上缺失 finally { remove() } 的严密保护。
 */
public class CaffeineLeakMock {

    
    private static final ThreadLocal<Boolean> nullBulkLoad = new ThreadLocal<>();

    
    private void internalLoad() {
        
        nullBulkLoad.set(true);

        
        if (Math.random() > 0.5) {
            throw new RuntimeException("Some internal loading error");
        }
    }

    
    public Map<String, String> getAll() {
        try {
            
            internalLoad();
            Map<String, String> result = new HashMap<>();

            
            Boolean isNull = nullBulkLoad.get();
            if (isNull != null && isNull) {
                
                nullBulkLoad.set(false);
                throw new RuntimeException("InvalidCacheLoadException: null key or value");
            }

            return result;
        } catch (Error e) {
            throw e;
        }
        
        
        
    }

    public static void main(String[] args) {
        CaffeineLeakMock cache = new CaffeineLeakMock();
        try {
            cache.getAll();
        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }
}