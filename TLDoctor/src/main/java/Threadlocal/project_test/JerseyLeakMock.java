package Threadlocal.project_test;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟 Jersey #5772 (NonInjectionManager ThreadLocal 伪清理漏洞)
 * 核心特征：试图使用 set(null) 来代替 remove() 进行清理，导致底层 ThreadLocalMap 的 Entry 残留。
 */
public class JerseyLeakMock {

    
    
    private final ThreadLocal<Map<String, String>> contextMap = new ThreadLocal<>();

    public void processRequest() {
        try {
            
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                map.put("key" + i, "Very large payload data...");
            }
            contextMap.set(map);

            
            System.out.println("Executing reactive request on epoll thread...");

        } finally {
            
            
            
            contextMap.set(null);
        }
    }

    public static void main(String[] args) {
        JerseyLeakMock mock = new JerseyLeakMock();
        mock.processRequest();
    }
}