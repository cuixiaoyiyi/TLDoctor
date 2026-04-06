package Threadlocal.Example1;

import java.text.SimpleDateFormat;

public class DummyTest3 {

    
    private static final ThreadLocal<SimpleDateFormat> FORMATTER =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    public void doBusinessLogic() {
        System.out.println("处理业务...");
        
        cleanUpTool(FORMATTER);
    }

    
    private static void cleanUpTool(ThreadLocal<?> targetLocal) {
        
        targetLocal.remove();
    }
}