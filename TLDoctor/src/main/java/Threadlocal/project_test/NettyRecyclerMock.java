package Threadlocal.project_test;

/**
 * 模拟 gRPC #4495 / Netty 4.1.23.Final 高 CPU 与内存泄漏漏洞
 * 核心特征：向 ThreadLocal 中塞入匿名内部类，触发 this$0 强引用隐式泄漏。
 */
public class NettyRecyclerMock {

    
    
    private final ThreadLocal<Runnable> threadLocal = new ThreadLocal<>();

    
    private final byte[] massiveData = new byte[10 * 1024 * 1024];

    public void registerCleanupTask() {

        
        
        Runnable cleanupTask = new Runnable() {
            @Override
            public void run() {
                
                System.out.println("ObjectCleanerThread: 试图回收内存...");
                
            }
        };

        
        
        threadLocal.set(cleanupTask);
    }

    public static void main(String[] args) {
        NettyRecyclerMock mock = new NettyRecyclerMock();
        mock.registerCleanupTask();
    }
}