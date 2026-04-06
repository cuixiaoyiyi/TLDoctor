package Threadlocal.Example2;

/**
 * Type II 漏洞靶场：线程池环境下的状态残留与类加载器泄漏
 */
public class DummyTest3 {

    
    
    
    private static final ThreadLocal<String> USER_SESSION_CONTEXT = new ThreadLocal<>();

    /**
     * 模拟处理 Web HTTP 请求的方法
     */
    public void handleHttpRequest(String userId) {
        
        USER_SESSION_CONTEXT.set(userId);

        try {
            
            processBusinessLogic();
        } finally {
            
            
            
            
            
            
            USER_SESSION_CONTEXT.set(null);
        }
    }

    private void processBusinessLogic() {
        String currentUser = USER_SESSION_CONTEXT.get();
        System.out.println("正在处理用户 [" + currentUser + "] 的敏感业务...");
    }
}