package Threadlocal.Example4;



import java.util.HashMap;
import java.util.Map;

public class DummyTest2 {

    private final ThreadLocal<Runnable> threadContext = new ThreadLocal<>();
    private final Map<String, Runnable> normalMap = new HashMap<>();

    // 非静态内部类！
    class MyNamedTask implements Runnable {
        @Override
        public void run() {}
    }

    public void testMethod() {
        // ==========================================
        // 【防守测试：消除误报】
        // ==========================================
        // 这只是一个普通的 Map，恰好方法名也叫 set/put。
        // 工具不应该报警，因为它不是 ThreadLocal！
        // 备注：如果用你自己实现的带有 set 方法的类，V3.0 也能认出来不是 ThreadLocal。
        normalMap.put("key", new Runnable() {
            @Override
            public void run() {}
        });

        // ==========================================
        // 【攻击测试 1：攻克漏报 (变量追踪)】
        // ==========================================
        // 开发者没有直接把 new 写在参数里，而是先赋值给了一个变量。
        Runnable tempTask = new Runnable() {
            @Override
            public void run() {}
        };
        // V3.0 会顺藤摸瓜，沿着 tempTask 找到上面的匿名内部类并报警！
        threadContext.set(tempTask);

        // ==========================================
        // 【攻击测试 2：攻克漏报 (具名非静态内部类)】
        // ==========================================
        // 开发者规规矩矩地写了一个命名的内部类 MyNamedTask。
        // 但因为它没有加 static，它依然会造成“死亡拥抱”。
        // V3.0 会去查找 MyNamedTask 的定义，发现是非静态内部类并报警！
        threadContext.set(new MyNamedTask());
    }
}
