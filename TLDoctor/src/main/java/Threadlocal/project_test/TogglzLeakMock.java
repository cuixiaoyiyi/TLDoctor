package Threadlocal.project_test;

/**
 * 模拟 Togglz #1344 类加载器与伪清理泄漏漏洞 (由 QiuYucheng 报告)
 * 核心特征：static ThreadLocal 配合 set(null) 伪清理，导致强引用链未断开，引发 Metaspace OOM。
 */
public class TogglzLeakMock {

    
    public static class FeatureUser {
        private String name;
        public FeatureUser(String name) {
            this.name = name;
        }
    }

    
    public static class ThreadLocalUserProvider {

        
        private static final ThreadLocal<FeatureUser> threadLocal = new ThreadLocal<>();

        
        public void bindUser(FeatureUser featureUser) {
            threadLocal.set(featureUser);
        }

        
        public static void release() {
            
            threadLocal.set(null);
        }
    }

    public static void main(String[] args) {
        ThreadLocalUserProvider provider = new ThreadLocalUserProvider();
        provider.bindUser(new FeatureUser("admin"));

        
        ThreadLocalUserProvider.release();
    }
}