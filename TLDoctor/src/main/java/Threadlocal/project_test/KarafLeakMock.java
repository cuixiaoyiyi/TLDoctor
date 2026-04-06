package Threadlocal.project_test;

/**
 * 模拟 Apache Karaf #2278 静态 ThreadLocal 引发的 OSGi 类加载器泄漏 (由 QiuYucheng 报告)
 * 核心特征：在静态工具类中使用 static ThreadLocal 缓存外部 Bundle 提供的对象，且不提供 remove 机制。
 */
public class KarafLeakMock {

    // 1. 模拟来自外部 OSGi Bundle 的 XML 解析器工厂类 (代表一个会被动态加载和卸载的类)
    public static class DocumentBuilderFactory {
        public void setNamespaceAware(boolean awareness) {}
    }

    public static class TransformerFactory { }

    // 2. 漏洞核心：使用静态 ThreadLocal 无限期缓存这些对象
    private static final ThreadLocal<DocumentBuilderFactory> DOCUMENT_BUILDER_FACTORY = new ThreadLocal<>();
    private static final ThreadLocal<TransformerFactory> TRANSFORMER_FACTORY = new ThreadLocal<>();

    // 3. 模拟工具类的方法
    public static DocumentBuilderFactory documentBuilder() {
        DocumentBuilderFactory dbf = DOCUMENT_BUILDER_FACTORY.get();
        if (dbf == null) {
            dbf = new DocumentBuilderFactory();
            dbf.setNamespaceAware(true);

            // 危险动作：注入到线程上下文中，但整个类没有任何地方调用 remove()
            DOCUMENT_BUILDER_FACTORY.set(dbf);
        }
        return dbf;
    }

    public static void processXml() {
        // 模拟业务调用
        DocumentBuilderFactory factory = documentBuilder();

        TransformerFactory tf = new TransformerFactory();
        TRANSFORMER_FACTORY.set(tf);
    }

    public static void main(String[] args) {
        processXml();
    }
}