package Threadlocal.project_test;

/**
 * 模拟 Dubbo #1664 (FST 序列化 ThreadLocal 内存泄漏)
 * 核心逻辑：ThreadLocal 缓存了带大数组的对象，且没有显式调用 remove()。
 * 所谓的 reset 只是重置了游标，没有释放内存。
 */
public class DubboFSTLeakMock {

    
    static class FSTOutputStream {
        byte[] buffer = new byte[1024]; 
        int pos = 0;

        public void write(byte[] data) {
            
            if (pos + data.length > buffer.length) {
                byte[] newBuffer = new byte[Math.max(buffer.length * 2, pos + data.length)];
                System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                buffer = newBuffer; 
            }
            pos += data.length;
        }

        
        public void reset() {
            pos = 0;
        }
    }

    
    static class FSTObjectOutput {
        FSTOutputStream buffout;
        boolean closed = false;

        public FSTObjectOutput() {
            this.buffout = new FSTOutputStream();
        }

        public void resetForReUse() {
            if (closed) throw new RuntimeException("Stream is closed");
            buffout.reset(); 
        }

        public void writeObject(byte[] data) {
            buffout.write(data);
        }
    }

    
    static class FSTConfiguration {
        
        static ThreadLocal<FSTObjectOutput> output = new ThreadLocal<>();

        public FSTObjectOutput getObjectOutput() {
            FSTObjectOutput fstOut = output.get();
            if (fstOut == null || fstOut.closed) {
                fstOut = new FSTObjectOutput();
                output.set(fstOut); 
            } else {
                fstOut.resetForReUse(); 
            }
            return fstOut;
        }
    }

    
    public void handleRequest(byte[] payload) {
        FSTConfiguration conf = new FSTConfiguration();
        FSTObjectOutput out = null;

        try {
            out = conf.getObjectOutput();

            
            if (payload.length > 8388608) { 
                throw new RuntimeException("ExceedPayloadLimitException: Data length too large");
            }

            out.writeObject(payload);
            System.out.println("Payload encoded successfully.");

        } catch (Exception e) {
            System.err.println("Exception caught: " + e.getMessage());
        } finally {
            
            
        }
    }

    
    public static void main(String[] args) {
        DubboFSTLeakMock worker = new DubboFSTLeakMock();

        
        worker.handleRequest(new byte[512]);

        
        worker.handleRequest(new byte[10 * 1024 * 1024]);
    }
}