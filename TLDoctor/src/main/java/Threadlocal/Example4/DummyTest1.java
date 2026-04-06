package Threadlocal.Example4;

public class DummyTest1 {

    private final ThreadLocal<Runnable> taskContext = new ThreadLocal<>();

    public void processRequest() {
        
        
        
        taskContext.set(new Runnable() {
            @Override
            public void run() {
                System.out.println("Processing in isolated context...");
            }
        });
    }

    public void initContext() {
        
        
        ThreadLocal<String> configLoader = ThreadLocal.withInitial(new java.util.function.Supplier<String>() {
            @Override
            public String get() {
                return "DefaultConfig";
            }
        });
    }
}