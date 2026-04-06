package Threadlocal.Example1;

import java.util.List;
import java.util.ArrayList;


class MyRequestContext {
    public StringBuilder sqlBuilder = new StringBuilder(); 
    public String userId;
}

public class DummyTest2 {

    
    
    
    
    private static final ThreadLocal<MyRequestContext> WRAPPER_CONTEXT = new ThreadLocal<>();

    
    
    
    
    private static final ThreadLocal<List<String>> DATA_LIST = new ThreadLocal<>();

    
    
    
    
    private static final ThreadLocal<StringBuilder> SAFE_BUILDER = ThreadLocal.withInitial(StringBuilder::new);

    public void processData() {
        StringBuilder sb = SAFE_BUILDER.get();
        sb.append("some simulated heavy data");

        
        if (sb.capacity() > 64 * 1024) {
            SAFE_BUILDER.remove(); 
        } else {
            sb.setLength(0); 
        }
    }
}