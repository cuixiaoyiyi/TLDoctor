package Threadlocal.Example3;

public class DummyTest {

    private static final ThreadLocal<Boolean> nullBulkLoad = new ThreadLocal<>();
    private static final ThreadLocal<String> AccessContext = new ThreadLocal<>();
    private static final ThreadLocal<String> Biff8EncryptionKey = new ThreadLocal<>();
    private static final ThreadLocal<String> safeContext = new ThreadLocal<>();

    // ==========================================
    // 【攻击测试 1：Caffeine 状态残留】
    // 完全没有 try-catch-finally 保护
    // ==========================================
    public void processCache() {
        nullBulkLoad.set(true);
        // 模拟业务逻辑抛出异常，线程直接跳出，永远无法执行后续清理
        if (System.currentTimeMillis() > 0) throw new RuntimeException("Cache Exception");
        nullBulkLoad.remove();
    }

    // ==========================================
    // 【攻击测试 2：DBFlute 越权风险】
    // 有 try，但是没有 finally 块
    // ==========================================
    public void processDatabaseRequest(String userId) {
        try {
            AccessContext.set(userId);
            // 业务执行...
        } catch (Exception e) {
            System.out.println("Error occurred");
        }
        // 漏报：没有 finally 保证 remove 必定执行
    }

    // ==========================================
    // 【攻击测试 3：Apache POI 密码泄漏】
    // 有 try-finally，但 finally 里写错了变量或没调用 remove
    // ==========================================
    public void decryptFile(String password) {
        try {
            Biff8EncryptionKey.set(password);
            // 解密逻辑...
        } finally {
            // 致命错误：开发者忘记写 Biff8EncryptionKey.remove() 了！
            System.out.println("Cleanup finished");
        }
    }

    // ==========================================
    // 【防守测试：完美的结构化清理】
    // ==========================================
    public void safeExecution() {
        try {
            safeContext.set("tenant-01");
            // 业务逻辑...
        } finally {
            // 完美防御：被 finally 严密包裹的同变量 remove()
            safeContext.remove();
        }
    }
}