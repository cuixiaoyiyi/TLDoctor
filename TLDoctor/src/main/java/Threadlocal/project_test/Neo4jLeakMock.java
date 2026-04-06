package Threadlocal.project_test;

import java.util.HashMap;

/**
 * 模拟 Neo4j OGM #1395 类加载器泄漏漏洞 (由 QiuYucheng 报告)
 * 核心特征：在 Enum 中使用 static ThreadLocal.withInitial 缓存匿名内部类实例，引发绝对的 ClassLoader 锁定。
 */
public class Neo4jLeakMock {

    
    
    public static abstract class TypeReference<T> {
        protected TypeReference() {}
    }

    
    public enum DefaultParameterConversion {
        INSTANCE;

        
        
        
        
        
        private static final ThreadLocal<TypeReference<HashMap<String, Object>>> MAP_TYPE_REF =
                ThreadLocal.withInitial(() -> new TypeReference<HashMap<String, Object>>() {});

        public void convertParameters() {
            
            TypeReference<HashMap<String, Object>> ref = MAP_TYPE_REF.get();
            System.out.println("Processing with TypeReference: " + ref.getClass().getName());
        }
    }

    public static void main(String[] args) {
        DefaultParameterConversion.INSTANCE.convertParameters();
    }
}