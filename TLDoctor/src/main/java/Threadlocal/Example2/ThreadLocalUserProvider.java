package Threadlocal.Example2;

// 1. 模拟一个自定义的业务类 (非 JDK 基础类)
// 这是触发“类加载器锁定”的必要条件，因为 FeatureUser.class 会强引用 WebappClassLoader
class FeatureUser {
    private String name;
    private boolean featureEnabled;

    public FeatureUser(String name, boolean featureEnabled) {
        this.name = name;
        this.featureEnabled = featureEnabled;
    }
}

/**
 * 还原 Togglz (Issue #1344) 的 Type II 漏洞真实场景
 */
public class ThreadLocalUserProvider {

    // 【漏洞核心 1：全局静态 ThreadLocal 挂载自定义类】
    private static final ThreadLocal<FeatureUser> threadLocal = new ThreadLocal<>();

    /**
     * 将当前用户信息绑定到 Tomcat 的工作线程上
     */
    public static void bind(FeatureUser user) {
        threadLocal.set(user);
    }

    /**
     * 获取当前线程绑定的用户
     */
    public static FeatureUser getCurrentUser() {
        return threadLocal.get();
    }

    /**
     * 【漏洞核心 2：致命的伪清理 (The set(null) Fallacy)】
     * 开发者试图在请求结束时清理用户上下文，但使用了错误的 API
     */
    public static void release() {
        // <--- INCORRECT: Entry remains in ThreadLocalMap
        // 这会导致当前 Tomcat 线程的 ThreadLocalMap 里永远残留一个 Key 为该 threadLocal 的强引用 Entry
        threadLocal.set(null);
    }
}