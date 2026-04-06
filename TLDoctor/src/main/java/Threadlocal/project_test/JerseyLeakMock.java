package Threadlocal.project_test;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟 Jersey #5772 (NonInjectionManager ThreadLocal 伪清理漏洞)
 * 核心特征：试图使用 set(null) 来代替 remove() 进行清理，导致底层 ThreadLocalMap 的 Entry 残留。
 */
public class JerseyLeakMock {

    // 模拟 NonInjectionManager 中存储上下文的 ThreadLocal
    // Issue 中提到里面存的是 MultiValueHashMap，这里用普通的 Map 代替
    private final ThreadLocal<Map<String, String>> contextMap = new ThreadLocal<>();

    public void processRequest() {
        try {
            // 1. 请求到来，初始化并存入大对象
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                map.put("key" + i, "Very large payload data...");
            }
            contextMap.set(map);

            // 2. 模拟执行 Reactor 线程池中的业务逻辑
            System.out.println("Executing reactive request on epoll thread...");

        } finally {
            // ⚠️ 漏洞核心：开发者以为这样就能释放内存 (对应 Issue #5710 的错误修复)
            // 实际上这只是把 Entry 的 value 设为了 null，Entry 本身依然滞留在 ThreadLocalMap 中。
            // 正确的做法必须是 contextMap.remove();
            contextMap.set(null);
        }
    }

    public static void main(String[] args) {
        JerseyLeakMock mock = new JerseyLeakMock();
        mock.processRequest();
    }
}