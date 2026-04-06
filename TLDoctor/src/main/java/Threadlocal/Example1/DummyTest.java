package Threadlocal.Example1;

import java.text.Collator;
import java.text.SimpleDateFormat;

// 模拟 LWJGL 的 MemoryStack
class MemoryStack {}

public class DummyTest {

    // 1. InfluxDB Java 误用模式 (Issue #1019)
    private static final ThreadLocal<StringBuilder> CACHED_STRINGBUILDERS =
            ThreadLocal.withInitial(StringBuilder::new);

    // 2. Spotify API 误用模式 (Issue #450)
    private static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    // 3. Azure SDK 误用模式 (Issue #48018)
    private static final ThreadLocal<Collator> CACHED_COLLATOR =
            new ThreadLocal<>();

    // 4. LWJGL 3 误用模式 (Issue #1109)
    private static final ThreadLocal<MemoryStack> TLS_STACK =
            ThreadLocal.withInitial(MemoryStack::new);

    // 5. 正常的轻量级缓存 (作为防守测试，不应报警)
    private static final ThreadLocal<Integer> NORMAL_COUNTER =
            new ThreadLocal<>();

    // 【新增的清理方法】
    // 加上 static 修饰符，与静态变量保持一致
    public static void clearResource() {
        NORMAL_COUNTER.remove();
        CACHED_STRINGBUILDERS.remove();
    }
}