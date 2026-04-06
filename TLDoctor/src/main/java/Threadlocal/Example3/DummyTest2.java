package Threadlocal.Example3;

public class DummyTest2 {
    private static final ThreadLocal<String> userContext = new ThreadLocal<>();

    
    
    
    
    public void standardSafeExecution() {
        userContext.set("tenant-01");
        try {
            System.out.println("working...");
        } finally {
            userContext.remove();
        }
    }

    
    
    
    
    public void nestedSafeExecution() {
        try {
            userContext.set("tenant-02");
            try {
                System.out.println("dangerous operation");
            } catch (Exception e) {}
        } finally {
            userContext.remove();
        }
    }

    
    
    
    
    public void deadlyConditionalCleanup(boolean flag) {
        try {
            userContext.set("admin-user");
        } finally {
            
            if (flag) {
                userContext.remove();
            }
        }
    }
}