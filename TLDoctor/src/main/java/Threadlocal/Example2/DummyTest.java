package Threadlocal.Example2;

class DoubleFormat {}
class PersonKryoSerializer {}

public class DummyTest {

    // ==========================================
    // 【危险示例 1：静态数字格式化器缓存】
    // 对应：Micrometer (Issue #7184)
    // ==========================================
    private static final ThreadLocal<DoubleFormat> DOUBLE_FORMAT = new ThreadLocal<>();

    // ==========================================
    // 【危险示例 2：静态序列化器】
    // 对应：Hazelcast (Issue #763)
    // ==========================================
    public static ThreadLocal<PersonKryoSerializer> serializerCache = ThreadLocal.withInitial(PersonKryoSerializer::new);

    // ==========================================
    // 【安全示例：非静态的实例级别 ThreadLocal】
    // 如果类的实例被回收，ThreadLocal 也会被回收，相对安全
    // ==========================================
    private final ThreadLocal<String> requestContext = new ThreadLocal<>();

    public void processTogglzState() {
        // ... 业务逻辑 ...

        // ==========================================
        // 【危险示例 3：致命的伪清理操作】
        // 对应：Togglz (Issue #1344)
        // ==========================================
        requestContext.set(null);
    }

    public void processSafeState() {
        // ... 业务逻辑 ...

        // 【安全示例：正确的结构化清理】
        requestContext.remove();
    }
}