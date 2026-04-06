package Threadlocal.project_test;

/**
 * 模拟 QuickFIX/J #1137 高水位内存泄漏漏洞 (由 QiuYucheng 报告)
 * 核心特征：ThreadLocal 缓存 StringBuilder，使用 setLength(0) 进行伪清理，导致底层 char[] 永久膨胀。
 */
public class QuickfixLeakMock {

    
    static class Context {
        StringBuilder stringBuilder = new StringBuilder();
    }

    
    private static final ThreadLocal<Context> stringContexts = new ThreadLocal<Context>() {
        @Override
        protected Context initialValue() {
            return new Context();
        }
    };

    
    public String processMessage(String largePayload) {
        Context context = stringContexts.get();
        StringBuilder stringBuilder = context.stringBuilder;

        try {
            
            stringBuilder.append(largePayload);
            return stringBuilder.toString();
        } finally {
            
            
            stringBuilder.setLength(0);

            
            
            
            
        }
    }

    public static void main(String[] args) {
        QuickfixLeakMock mock = new QuickfixLeakMock();
        mock.processMessage("... massive 20MB string payload ...");
    }
}