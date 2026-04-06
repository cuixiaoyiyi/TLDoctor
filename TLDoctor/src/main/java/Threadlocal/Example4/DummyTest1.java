package Threadlocal.Example4;

public class DummyTest1 {

    private final ThreadLocal<Runnable> taskContext = new ThreadLocal<>();

    public void processRequest() {
        // 【DataDog Issue #292 模拟】
        // 开发者为了图省事，直接 new 了一个匿名的 Runnable 塞进 ThreadLocal。
        // 虽然逻辑看起来没问题，但这个匿名的 Runnable 已经悄悄持有了 DummyTest.this 的强引用！
        taskContext.set(new Runnable() {
            @Override
            public void run() {
                System.out.println("Processing in isolated context...");
            }
        });
    }

    public void initContext() {
        // 【另一种常见的类型 IV 变体】
        // 使用 withInitial 传入匿名内部类（Supplier 的匿名实现），同样致命。
        ThreadLocal<String> configLoader = ThreadLocal.withInitial(new java.util.function.Supplier<String>() {
            @Override
            public String get() {
                return "DefaultConfig";
            }
        });
    }
}