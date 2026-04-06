package Threadlocal.Example4;



import java.util.HashMap;
import java.util.Map;

public class DummyTest2 {

    private final ThreadLocal<Runnable> threadContext = new ThreadLocal<>();
    private final Map<String, Runnable> normalMap = new HashMap<>();

    
    class MyNamedTask implements Runnable {
        @Override
        public void run() {}
    }

    public void testMethod() {
        
        
        
        
        
        
        normalMap.put("key", new Runnable() {
            @Override
            public void run() {}
        });

        
        
        
        
        Runnable tempTask = new Runnable() {
            @Override
            public void run() {}
        };
        
        threadContext.set(tempTask);

        
        
        
        
        
        
        threadContext.set(new MyNamedTask());
    }
}
