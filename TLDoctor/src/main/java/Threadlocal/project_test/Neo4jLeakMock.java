package Threadlocal.project_test;

import java.util.HashMap;

/**
 * 模拟 Neo4j OGM #1395 类加载器泄漏漏洞 (由 QiuYucheng 报告)
 * 核心特征：在 Enum 中使用 static ThreadLocal.withInitial 缓存匿名内部类实例，引发绝对的 ClassLoader 锁定。
 */
public class Neo4jLeakMock {

    // 1. 模拟 Jackson 的 TypeReference
    // 泛型擦除机制要求开发者必须通过匿名内部类 {} 来保留泛型类型，这就埋下了隐式引用的雷。
    public static abstract class TypeReference<T> {
        protected TypeReference() {}
    }

    // 2. 模拟 Neo4j 的 DefaultParameterConversion 枚举
    public enum DefaultParameterConversion {
        INSTANCE;

        // ⚠️ 漏洞核心：
        // 1. Enum 保证了 MAP_TYPE_REF 是绝对的静态 GC Root。
        // 2. withInitial 结合 Lambda (() ->) 生成了 InvokeDynamic。
        // 3. 内部的 new TypeReference<...>() {} 更是生成了一个匿名内部类实例。
        // 这一切都没有 remove() 来终结！
        private static final ThreadLocal<TypeReference<HashMap<String, Object>>> MAP_TYPE_REF =
                ThreadLocal.withInitial(() -> new TypeReference<HashMap<String, Object>>() {});

        public void convertParameters() {
            // 业务代码仅仅是 get() 出来用，没有 set 和 remove
            TypeReference<HashMap<String, Object>> ref = MAP_TYPE_REF.get();
            System.out.println("Processing with TypeReference: " + ref.getClass().getName());
        }
    }

    public static void main(String[] args) {
        DefaultParameterConversion.INSTANCE.convertParameters();
    }
}