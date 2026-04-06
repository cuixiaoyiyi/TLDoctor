package Threadlocal.project_test;

/**
 * 模拟 tascalate-javaflow #15 僵尸变量漏洞 (由 QiuYucheng 报告)
 * 核心特征：定义了 static ThreadLocal (还是个 Raw Type)，但实际上是没有任何调用的死代码。
 */
public class JavaFlowLeakMock {

    // 1. 模拟原始代码中的 Dead Code
    // 注意：这是一个 Raw Type (原生类型)，没有写 <...> 泛型
    private static final ThreadLocal threadMap = new ThreadLocal();

    // 2. 模拟其他正常的业务逻辑，完全没有碰到过 threadMap
    public void executeContinuation() {
        System.out.println("Executing continuation... without using threadMap");
    }

    public static void main(String[] args) {
        JavaFlowLeakMock mock = new JavaFlowLeakMock();
        mock.executeContinuation();
    }
}