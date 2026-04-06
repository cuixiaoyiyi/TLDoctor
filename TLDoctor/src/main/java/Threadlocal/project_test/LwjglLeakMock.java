package Threadlocal.project_test;

import java.nio.ByteBuffer;

/**
 * 模拟 LWJGL3 #1109 堆外内存泄漏风险 (由 QiuYucheng 报告)
 * 核心特征：ThreadLocal 缓存了包装着直接内存 (DirectByteBuffer) 的对象，且没有 remove 机制。
 */
public class LwjglLeakMock {

    
    public static class MemoryStack {
        
        private ByteBuffer buffer = ByteBuffer.allocateDirect(64 * 1024);

        protected MemoryStack() {}
    }

    
    
    private static final ThreadLocal<MemoryStack> TLS = ThreadLocal.withInitial(() -> new MemoryStack());

    
    public static MemoryStack stackGet() {
        return TLS.get(); 
    }

    public static void main(String[] args) {
        
        MemoryStack stack = stackGet();
        System.out.println("Allocated 64KB off-heap memory for current thread.");

        
    }
}