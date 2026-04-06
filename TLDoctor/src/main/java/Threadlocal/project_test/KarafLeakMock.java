package Threadlocal.project_test;

/**
 * 模拟 Apache Karaf #2278 静态 ThreadLocal 引发的 OSGi 类加载器泄漏 (由 QiuYucheng 报告)
 * 核心特征：在静态工具类中使用 static ThreadLocal 缓存外部 Bundle 提供的对象，且不提供 remove 机制。
 */
public class KarafLeakMock {

    
    public static class DocumentBuilderFactory {
        public void setNamespaceAware(boolean awareness) {}
    }

    public static class TransformerFactory { }

    
    private static final ThreadLocal<DocumentBuilderFactory> DOCUMENT_BUILDER_FACTORY = new ThreadLocal<>();
    private static final ThreadLocal<TransformerFactory> TRANSFORMER_FACTORY = new ThreadLocal<>();

    
    public static DocumentBuilderFactory documentBuilder() {
        DocumentBuilderFactory dbf = DOCUMENT_BUILDER_FACTORY.get();
        if (dbf == null) {
            dbf = new DocumentBuilderFactory();
            dbf.setNamespaceAware(true);

            
            DOCUMENT_BUILDER_FACTORY.set(dbf);
        }
        return dbf;
    }

    public static void processXml() {
        
        DocumentBuilderFactory factory = documentBuilder();

        TransformerFactory tf = new TransformerFactory();
        TRANSFORMER_FACTORY.set(tf);
    }

    public static void main(String[] args) {
        processXml();
    }
}