package Threadlocal.Example3;

public class DummyTest2 {
    private static final ThreadLocal<String> userContext = new ThreadLocal<>();

    // ==========================================
    // 【防守测试：消除误报 1】 set 写在 try 外面
    // V1.0 会误报，V2.0 会发现下一个语句就是 try，安全放过。
    // ==========================================
    public void standardSafeExecution() {
        userContext.set("tenant-01");
        try {
            System.out.println("working...");
        } finally {
            userContext.remove();
        }
    }

    // ==========================================
    // 【防守测试：消除误报 2】 嵌套 Try 块
    // V1.0 会因为最近的 try 没有 finally 而误报，V2.0 扫描外层放过。
    // ==========================================
    public void nestedSafeExecution() {
        try {
            userContext.set("tenant-02");
            try {
                System.out.println("dangerous operation");
            } catch (Exception e) {}
        } finally {
            userContext.remove();
        }
    }

    // ==========================================
    // 【攻击测试：消除漏报】 Finally 中条件清理
    // V1.0 会漏报，V2.0 发现 remove 在 if 里面，果断报警！
    // ==========================================
    public void deadlyConditionalCleanup(boolean flag) {
        try {
            userContext.set("admin-user");
        } finally {
            // 致命缺陷：如果 flag 为 false，权限将被残留！
            if (flag) {
                userContext.remove();
            }
        }
    }
}