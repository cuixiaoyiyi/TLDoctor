package Threadlocal.project_test;

import java.nio.ByteBuffer;

/**
 * 模拟 LWJGL3 #1109 堆外内存泄漏风险 (由 QiuYucheng 报告)
 * 核心特征：ThreadLocal 缓存了包装着直接内存 (DirectByteBuffer) 的对象，且没有 remove 机制。
 */
public class LwjglLeakMock {

    // 1. 模拟 LWJGL 的 MemoryStack，内部持有一块堆外内存
    public static class MemoryStack {
        // 模拟 64KB 的直接物理内存分配
        private ByteBuffer buffer = ByteBuffer.allocateDirect(64 * 1024);

        protected MemoryStack() {}
    }

    // 2. 模拟底层的 TLS 缓存机制
    // 作者为了极致性能，故意不写 remove()
    private static final ThreadLocal<MemoryStack> TLS = ThreadLocal.withInitial(() -> new MemoryStack());

    // 3. 模拟业务获取 Stack
    public static MemoryStack stackGet() {
        return TLS.get(); // 隐式触发 allocateDirect
    }

    public static void main(String[] args) {
        // 模拟在一个临时工作线程中调用
        MemoryStack stack = stackGet();
        System.out.println("Allocated 64KB off-heap memory for current thread.");

        // 线程结束，但是 ThreadLocal 永远不会被清理，物理内存被永久锁定！
    }
}