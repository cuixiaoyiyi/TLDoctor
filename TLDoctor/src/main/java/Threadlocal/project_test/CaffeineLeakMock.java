package Threadlocal.project_test;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟 Caffeine #1944 状态残留漏洞 (由 QiuYucheng 报告)
 * 核心特征：使用 ThreadLocal 作为旁路信号，但在异常路径上缺失 finally { remove() } 的严密保护。
 */
public class CaffeineLeakMock {

    // 静态 ThreadLocal，用于旁路传递 "是否包含 null 值" 的状态
    private static final ThreadLocal<Boolean> nullBulkLoad = new ThreadLocal<>();

    // 模拟底层的批量加载逻辑
    private void internalLoad() {
        // 遇到特定情况，设置旁路信号为 true
        nullBulkLoad.set(true);

        // 模拟可能发生的其他不可控异常
        if (Math.random() > 0.5) {
            throw new RuntimeException("Some internal loading error");
        }
    }

    // ⚠️ 漏洞方法 (修复前的 getAll 逻辑)
    public Map<String, String> getAll() {
        try {
            // 1. 触发底层加载
            internalLoad();
            Map<String, String> result = new HashMap<>();

            // 2. 检查旁路信号
            Boolean isNull = nullBulkLoad.get();
            if (isNull != null && isNull) {
                // 原代码的致命错误：试图在这里通过 set(false) 恢复状态
                nullBulkLoad.set(false);
                throw new RuntimeException("InvalidCacheLoadException: null key or value");
            }

            return result;
        } catch (Error e) {
            throw e;
        }
        // 漏洞核心：缺乏 finally { nullBulkLoad.remove(); }
        // 如果 internalLoad() 抛出了异常，控制流直接跳过 set(false)，
        // 导致 true 状态永久残留在当前线程中，污染下一个请求。
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