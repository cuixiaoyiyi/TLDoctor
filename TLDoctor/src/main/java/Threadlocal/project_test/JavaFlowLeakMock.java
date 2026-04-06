package Threadlocal.project_test;

/**
 * 模拟 tascalate-javaflow #15 僵尸变量漏洞 (由 QiuYucheng 报告)
 * 核心特征：定义了 static ThreadLocal (还是个 Raw Type)，但实际上是没有任何调用的死代码。
 */
public class JavaFlowLeakMock {

    
    
    private static final ThreadLocal threadMap = new ThreadLocal();

    
    public void executeContinuation() {
        System.out.println("Executing continuation... without using threadMap");
    }

    public static void main(String[] args) {
        JavaFlowLeakMock mock = new JavaFlowLeakMock();
        mock.executeContinuation();
    }
}