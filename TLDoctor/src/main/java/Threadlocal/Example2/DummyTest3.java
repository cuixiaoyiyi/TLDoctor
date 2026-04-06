package Threadlocal.Example2;

/**
 * Type II 漏洞靶场：线程池环境下的状态残留与类加载器泄漏
 */
public class DummyTest3 {

    // 【漏洞 1：静态 ThreadLocal 滥用】
    // 开发者图省事，用 static 声明。在 Tomcat 等容器中，这个变量会被 WebappClassLoader 强引用。
    // 如果不彻底清理，热部署时会导致整个旧的 ClassLoader 无法被回收，引发 Metaspace OOM。
    private static final ThreadLocal<String> USER_SESSION_CONTEXT = new ThreadLocal<>();

    /**
     * 模拟处理 Web HTTP 请求的方法
     */
    public void handleHttpRequest(String userId) {
        // 请求刚进入，将当前用户信息绑定到执行该请求的 Tomcat 线程上
        USER_SESSION_CONTEXT.set(userId);

        try {
            // 模拟复杂的业务逻辑调用...
            processBusinessLogic();
        } finally {
            // 【漏洞 2：致命的伪清理 (The set(null) Fallacy)】
            // 开发者以为把 value 设为 null 就可以让 GC 回收了。
            // 实际上，ThreadLocalMap 里的 Entry 节点依然存在！(Key 依然是当前的 USER_SESSION_CONTEXT)
            // 这会导致：
            // 1. 下一个复用该线程的用户，可能会遭遇空指针或逻辑错乱。
            // 2. 强引用的 Entry 节点导致类加载器泄漏。
            USER_SESSION_CONTEXT.set(null);
        }
    }

    private void processBusinessLogic() {
        String currentUser = USER_SESSION_CONTEXT.get();
        System.out.println("正在处理用户 [" + currentUser + "] 的敏感业务...");
    }
}