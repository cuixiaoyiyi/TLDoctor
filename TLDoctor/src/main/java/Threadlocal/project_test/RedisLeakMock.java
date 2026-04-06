package Threadlocal.project_test;

/**
 * 模拟 Redis OM Spring #718 上下文污染漏洞 (由 QiuYucheng 报告并修复)
 * 核心特征：暴露了裸露的 setContext 方法，缺乏强制清理的生命周期管理。
 */
public class RedisLeakMock {

    
    public static class RedisIndexContext {
        public String tenantId;
        public RedisIndexContext(String tenantId) {
            this.tenantId = tenantId;
        }
    }

    
    private static final ThreadLocal<RedisIndexContext> CONTEXT_HOLDER = new ThreadLocal<>();

    
    public static void setContext(RedisIndexContext context) {
        CONTEXT_HOLDER.set(context); 
    }

    public static RedisIndexContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    
    public static void main(String[] args) {
        try {
            
            setContext(new RedisIndexContext("Tenant-A"));
            System.out.println("Executing Redis query for: " + getContext().tenantId);

            
            if (true) throw new RuntimeException("Unexpected Error!");

            
            CONTEXT_HOLDER.remove();
        } catch (Exception e) {
            System.out.println("Error handled, but context is leaked!");
        }
    }
}