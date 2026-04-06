package Threadlocal.project_test;

/**
 * 模拟 MyBatis Redis Cache #351 枚举单例引发的 ClassLoader 泄漏 (由 QiuYucheng 报告)
 * 核心特征：利用 Enum 实现单例，内部维护 ThreadLocal 但缺乏 remove，极具迷惑性。
 */
public enum MybatisKryoLeakMock {

    // 1. 枚举单例实例
    INSTANCE;

    // 2. 模拟 Kryo 序列化器这种重量级/自定义对象
    public static class Kryo {
        // 内部包含大量类描述符缓存等
    }

    // 3. 【致命隐患】：作为枚举的实例变量，它实际上是全局唯一的 (Effectively Static)
    // 开发者图省事，没有提供任何 clean() 或 remove() 方法
    private final ThreadLocal<Kryo> kryos = new ThreadLocal<Kryo>() {
        @Override
        protected Kryo initialValue() {
            return new Kryo();
        }
    };

    // 4. 业务调用的序列化入口
    public Object serialize(Object object) {
        Kryo kryo = kryos.get(); // 发生绑定
        // ... 执行序列化逻辑 ...
        return "serialized_bytes";
        // 结束时毫无清理动作！
    }

    public static void main(String[] args) {
        // 模拟 Tomcat 工作线程调用
        MybatisKryoLeakMock.INSTANCE.serialize(new Object());
        System.out.println("Serialization done, but Kryo instance is permanently leaked in ThreadLocalMap!");
    }
}