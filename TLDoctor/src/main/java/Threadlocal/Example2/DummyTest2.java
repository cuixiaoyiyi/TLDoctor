package Threadlocal.Example2;

// 模拟一个干扰类，名字以 ThreadLocal 开头
class ThreadLocalManager {
    public void set(Object obj) {}
}

public class DummyTest2 {

    // ==========================================================
    // 【防守测试：消除规则 1 误报】名字匹配，但类型不是真的 ThreadLocal
    // ==========================================================
    // V1.0 会误报，V2.0 通过符号解析知道它只是个普通的 Manager，安全放过。
    private static final ThreadLocalManager ThreadLocalCache = new ThreadLocalManager();

    // ==========================================================
    // 【攻击测试：消除规则 1 漏报】完全限定名与子类
    // ==========================================================
    // V1.0 因为带有 "java.lang" 或 "Inheritable" 开头会漏报。
    // V2.0 准确识别出它们底层的真正身份并报警。
    private static final java.lang.ThreadLocal<String> FQ_CONTEXT = new java.lang.ThreadLocal<>();
    private static final InheritableThreadLocal<String> INHERIT_CTX = new InheritableThreadLocal<>();

    // ==========================================================
    // 【防守测试：消除规则 2 误报】普通类的 set(null) 且变量名含 context
    // ==========================================================
    public void testSafeSet() {
        ThreadLocalManager fakeContext = new ThreadLocalManager();
        // V1.0 因为变量名含 "Context" 且调用了 set(null) 会误报警。
        // V2.0 查到底层不是 ThreadLocal 类，安全放过。
        fakeContext.set(null);
    }

    // ==========================================================
    // 【攻击测试：消除规则 2 漏报】随意命名的 ThreadLocal 调用 set(null)
    // ==========================================================
    public void testDangerousSet() {
        ThreadLocal<String> userSession = new ThreadLocal<>();
        // V1.0 因为 "userSession" 不包含 context/threadlocal 关键字而漏报。
        // V2.0 查到底层是 ThreadLocal 类，果断报警！
        userSession.set(null);
    }
}