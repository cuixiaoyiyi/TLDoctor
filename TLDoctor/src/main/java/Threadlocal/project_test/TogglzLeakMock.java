package Threadlocal.project_test;

/**
 * 模拟 Togglz #1344 类加载器与伪清理泄漏漏洞 (由 QiuYucheng 报告)
 * 核心特征：static ThreadLocal 配合 set(null) 伪清理，导致强引用链未断开，引发 Metaspace OOM。
 */
public class TogglzLeakMock {

    // 1. 模拟 Togglz 的 FeatureUser 自定义类
    public static class FeatureUser {
        private String name;
        public FeatureUser(String name) {
            this.name = name;
        }
    }

    // 2. 模拟 ThreadLocalUserProvider
    public static class ThreadLocalUserProvider {

        // 静态 ThreadLocal，极易锁定 ClassLoader
        private static final ThreadLocal<FeatureUser> threadLocal = new ThreadLocal<>();

        // 绑定用户到当前线程
        public void bindUser(FeatureUser featureUser) {
            threadLocal.set(featureUser);
        }

        // ⚠️ 漏洞核心：开发者自作聪明的伪清理
        public static void release() {
            // 正确做法应该是 threadLocal.remove();
            threadLocal.set(null);
        }
    }

    public static void main(String[] args) {
        ThreadLocalUserProvider provider = new ThreadLocalUserProvider();
        provider.bindUser(new FeatureUser("admin"));

        // 模拟业务执行完毕后清理资源
        ThreadLocalUserProvider.release();
    }
}