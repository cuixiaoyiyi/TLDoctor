package Threadlocal.project_test;

/**
 * 模拟 QuickFIX/J #1137 高水位内存泄漏漏洞 (由 QiuYucheng 报告)
 * 核心特征：ThreadLocal 缓存 StringBuilder，使用 setLength(0) 进行伪清理，导致底层 char[] 永久膨胀。
 */
public class QuickfixLeakMock {

    // 1. 模拟 QuickFIX 中的 Context 包装类
    static class Context {
        StringBuilder stringBuilder = new StringBuilder();
    }

    // 2. 使用 initialValue 初始化的 ThreadLocal (注意：这里没有显式调用 set)
    private static final ThreadLocal<Context> stringContexts = new ThreadLocal<Context>() {
        @Override
        protected Context initialValue() {
            return new Context();
        }
    };

    // 3. 模拟 toString() 方法中的报文处理逻辑
    public String processMessage(String largePayload) {
        Context context = stringContexts.get();
        StringBuilder stringBuilder = context.stringBuilder;

        try {
            // 模拟追加超大报文，触发 StringBuilder 底层 char[] 暴涨 (例如扩容到 20MB)
            stringBuilder.append(largePayload);
            return stringBuilder.toString();
        } finally {
            // ⚠️ 漏洞核心：开发者以为这样清理了内存，但其实仅仅重置了游标。
            // 扩容后的 20MB 数组随着 ThreadLocal 永久存活于当前线程！
            stringBuilder.setLength(0);

            // 正确的防膨胀修复应当是：
            // if (stringBuilder.capacity() > 65536) {
            //     stringBuilder.trimToSize();  // 或者 stringContexts.remove();
            // }
        }
    }

    public static void main(String[] args) {
        QuickfixLeakMock mock = new QuickfixLeakMock();
        mock.processMessage("... massive 20MB string payload ...");
    }
}