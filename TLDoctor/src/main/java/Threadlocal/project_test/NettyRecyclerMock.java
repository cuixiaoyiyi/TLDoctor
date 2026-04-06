package Threadlocal.project_test;

/**
 * 模拟 gRPC #4495 / Netty 4.1.23.Final 高 CPU 与内存泄漏漏洞
 * 核心特征：向 ThreadLocal 中塞入匿名内部类，触发 this$0 强引用隐式泄漏。
 */
public class NettyRecyclerMock {

    // 你的工具也支持检测 io.netty.util.concurrent.FastThreadLocal，
    // 为了方便本地零依赖编译，我们用 JDK 的 ThreadLocal 等价替换，分析原理完全相同。
    private final ThreadLocal<Runnable> threadLocal = new ThreadLocal<>();

    // 模拟一段占用大量内存的外部类状态
    private final byte[] massiveData = new byte[10 * 1024 * 1024];

    public void registerCleanupTask() {

        // ⚠️ 漏洞核心：在 Netty 4.1.23 中，清理任务被写成了匿名内部类。
        // 编译器会强制生成一个 this$0 字段指向外部的 NettyRecyclerMock 实例（连带那 10MB 内存）。
        Runnable cleanupTask = new Runnable() {
            @Override
            public void run() {
                // 模拟 ObjectCleanerThread 的后台清理逻辑
                System.out.println("ObjectCleanerThread: 试图回收内存...");
                // 内部类里即使什么都不写，this$0 的强引用也已经存在了！
            }
        };

        // 将带有隐式外部引用的内部类强行塞入 ThreadLocal 缓存池。
        // 这彻底破坏了 GC 的可达性分析，导致 ObjectCleaner 线程陷入死循环 (CPU 100%)。
        threadLocal.set(cleanupTask);
    }

    public static void main(String[] args) {
        NettyRecyclerMock mock = new NettyRecyclerMock();
        mock.registerCleanupTask();
    }
}