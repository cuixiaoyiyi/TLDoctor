package Threadlocal.Example3;

public class DummyTest {

    private static final ThreadLocal<Boolean> nullBulkLoad = new ThreadLocal<>();
    private static final ThreadLocal<String> AccessContext = new ThreadLocal<>();
    private static final ThreadLocal<String> Biff8EncryptionKey = new ThreadLocal<>();
    private static final ThreadLocal<String> safeContext = new ThreadLocal<>();

    
    
    
    
    public void processCache() {
        nullBulkLoad.set(true);
        
        if (System.currentTimeMillis() > 0) throw new RuntimeException("Cache Exception");
        nullBulkLoad.remove();
    }

    
    
    
    
    public void processDatabaseRequest(String userId) {
        try {
            AccessContext.set(userId);
            
        } catch (Exception e) {
            System.out.println("Error occurred");
        }
        
    }

    
    
    
    
    public void decryptFile(String password) {
        try {
            Biff8EncryptionKey.set(password);
            
        } finally {
            
            System.out.println("Cleanup finished");
        }
    }

    
    
    
    public void safeExecution() {
        try {
            safeContext.set("tenant-01");
            
        } finally {
            
            safeContext.remove();
        }
    }
}