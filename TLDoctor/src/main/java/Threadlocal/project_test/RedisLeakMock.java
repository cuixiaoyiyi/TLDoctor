package Threadlocal.project_test;

/**
 * 模拟 Redis OM Spring #718 上下文污染漏洞 (由 QiuYucheng 报告并修复)
 * 核心特征：暴露了裸露的 setContext 方法，缺乏强制清理的生命周期管理。
 */
public class RedisLeakMock {

    // 1. 模拟 Redis 的索引上下文对象
    public static class RedisIndexContext {
        public String tenantId;
        public RedisIndexContext(String tenantId) {
            this.tenantId = tenantId;
        }
    }

    // 2. 静态 ThreadLocal 存储上下文
    private static final ThreadLocal<RedisIndexContext> CONTEXT_HOLDER = new ThreadLocal<>();

    // 3. 【致命缺陷 API】：向外界暴露了毫无防护的 set 方法
    public static void setContext(RedisIndexContext context) {
        CONTEXT_HOLDER.set(context); // 没有任何强制 remove 的约束！
    }

    public static RedisIndexContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    // 4. 模拟业务调用方的日常翻车现场
    public static void main(String[] args) {
        try {
            // 开发者顺手调用了暴露的 setContext 方法
            setContext(new RedisIndexContext("Tenant-A"));
            System.out.println("Executing Redis query for: " + getContext().tenantId);

            // 模拟业务逻辑抛出异常
            if (true) throw new RuntimeException("Unexpected Error!");

            // 这里的 remove 永远不会被执行，状态永久残留！
            CONTEXT_HOLDER.remove();
        } catch (Exception e) {
            System.out.println("Error handled, but context is leaked!");
        }
    }
}