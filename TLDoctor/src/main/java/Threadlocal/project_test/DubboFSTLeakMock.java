package Threadlocal.project_test;

/**
 * 模拟 Dubbo #1664 (FST 序列化 ThreadLocal 内存泄漏)
 * 核心逻辑：ThreadLocal 缓存了带大数组的对象，且没有显式调用 remove()。
 * 所谓的 reset 只是重置了游标，没有释放内存。
 */
public class DubboFSTLeakMock {

    // 1. 模拟 FST 底层的 OutputStream (持有大数组)
    static class FSTOutputStream {
        byte[] buffer = new byte[1024]; // 初始小缓冲
        int pos = 0;

        public void write(byte[] data) {
            // 模拟动态扩容
            if (pos + data.length > buffer.length) {
                byte[] newBuffer = new byte[Math.max(buffer.length * 2, pos + data.length)];
                System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                buffer = newBuffer; // 数组变大
            }
            pos += data.length;
        }

        // ⚠️ 漏洞核心：只重置游标，不缩小也不释放 buffer
        public void reset() {
            pos = 0;
        }
    }

    // 2. 模拟 FSTObjectOutput
    static class FSTObjectOutput {
        FSTOutputStream buffout;
        boolean closed = false;

        public FSTObjectOutput() {
            this.buffout = new FSTOutputStream();
        }

        public void resetForReUse() {
            if (closed) throw new RuntimeException("Stream is closed");
            buffout.reset(); // 调用了伪清理
        }

        public void writeObject(byte[] data) {
            buffout.write(data);
        }
    }

    // 3. 模拟 FSTConfiguration 和 ThreadLocal 管理
    static class FSTConfiguration {
        // 模拟 FSTDefaultStreamCoderFactory 中的 ThreadLocal
        static ThreadLocal<FSTObjectOutput> output = new ThreadLocal<>();

        public FSTObjectOutput getObjectOutput() {
            FSTObjectOutput fstOut = output.get();
            if (fstOut == null || fstOut.closed) {
                fstOut = new FSTObjectOutput();
                output.set(fstOut); // 初始化并 set
            } else {
                fstOut.resetForReUse(); // 尝试复用
            }
            return fstOut;
        }
    }

    // 4. 模拟 Dubbo 的 Worker 线程处理请求 (入口方法)
    public void handleRequest(byte[] payload) {
        FSTConfiguration conf = new FSTConfiguration();
        FSTObjectOutput out = null;

        try {
            out = conf.getObjectOutput();

            // 模拟检查 Payload 大小，如果过大直接抛出异常 (对应 ExceedPayloadLimitException)
            if (payload.length > 8388608) { // 8MB
                throw new RuntimeException("ExceedPayloadLimitException: Data length too large");
            }

            out.writeObject(payload);
            System.out.println("Payload encoded successfully.");

        } catch (Exception e) {
            System.err.println("Exception caught: " + e.getMessage());
        } finally {
            // ⚠️ 漏洞特征：在 finally 块中，没有任何 ThreadLocal.remove() 的调用
            // 也没有 out.buffout.buffer = null 的显式释放
        }
    }

    // 测试主函数
    public static void main(String[] args) {
        DubboFSTLeakMock worker = new DubboFSTLeakMock();

        // 正常小请求，buffer 为 1024
        worker.handleRequest(new byte[512]);

        // 恶意大请求，触发扩容并抛出异常，此时 buffer 膨胀到 10MB 并滞留在 ThreadLocal 中
        worker.handleRequest(new byte[10 * 1024 * 1024]);
    }
}