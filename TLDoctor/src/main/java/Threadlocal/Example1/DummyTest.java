package Threadlocal.Example1;

import java.text.Collator;
import java.text.SimpleDateFormat;


class MemoryStack {}

public class DummyTest {

    
    private static final ThreadLocal<StringBuilder> CACHED_STRINGBUILDERS =
            ThreadLocal.withInitial(StringBuilder::new);

    
    private static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    
    private static final ThreadLocal<Collator> CACHED_COLLATOR =
            new ThreadLocal<>();

    
    private static final ThreadLocal<MemoryStack> TLS_STACK =
            ThreadLocal.withInitial(MemoryStack::new);

    
    private static final ThreadLocal<Integer> NORMAL_COUNTER =
            new ThreadLocal<>();

    
    
    public static void clearResource() {
        NORMAL_COUNTER.remove();
        CACHED_STRINGBUILDERS.remove();
    }
}