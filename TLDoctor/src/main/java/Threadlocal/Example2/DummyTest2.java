package Threadlocal.Example2;


class ThreadLocalManager {
    public void set(Object obj) {}
}

public class DummyTest2 {

    
    
    
    
    private static final ThreadLocalManager ThreadLocalCache = new ThreadLocalManager();

    
    
    
    
    
    private static final java.lang.ThreadLocal<String> FQ_CONTEXT = new java.lang.ThreadLocal<>();
    private static final InheritableThreadLocal<String> INHERIT_CTX = new InheritableThreadLocal<>();

    
    
    
    public void testSafeSet() {
        ThreadLocalManager fakeContext = new ThreadLocalManager();
        
        
        fakeContext.set(null);
    }

    
    
    
    public void testDangerousSet() {
        ThreadLocal<String> userSession = new ThreadLocal<>();
        
        
        userSession.set(null);
    }
}