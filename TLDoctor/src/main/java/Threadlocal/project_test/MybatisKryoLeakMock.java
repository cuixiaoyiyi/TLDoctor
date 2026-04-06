package Threadlocal.project_test;

/**
 * 模拟 MyBatis Redis Cache #351 枚举单例引发的 ClassLoader 泄漏 (由 QiuYucheng 报告)
 * 核心特征：利用 Enum 实现单例，内部维护 ThreadLocal 但缺乏 remove，极具迷惑性。
 */
public enum MybatisKryoLeakMock {

    
    INSTANCE;

    
    public static class Kryo {
        
    }

    
    
    private final ThreadLocal<Kryo> kryos = new ThreadLocal<Kryo>() {
        @Override
        protected Kryo initialValue() {
            return new Kryo();
        }
    };

    
    public Object serialize(Object object) {
        Kryo kryo = kryos.get(); 
        
        return "serialized_bytes";
        
    }

    public static void main(String[] args) {
        
        MybatisKryoLeakMock.INSTANCE.serialize(new Object());
        System.out.println("Serialization done, but Kryo instance is permanently leaked in ThreadLocalMap!");
    }
}