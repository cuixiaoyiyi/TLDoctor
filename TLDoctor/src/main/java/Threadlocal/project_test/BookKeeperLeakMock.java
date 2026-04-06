package Threadlocal.project_test;

/**
 * 模拟 Apache BookKeeper #4714 高水位线内存滞留 (由 QiuYucheng 报告)
 * 核心特征：ThreadLocal 缓存 StringBuilder，仅使用 setLength(0) 进行伪清理，底层大数组永不释放。
 */
public class BookKeeperLeakMock {

    
    private static final ThreadLocal<StringBuilder> threadLocalNodeBuilder =
            ThreadLocal.withInitial(StringBuilder::new);

    
    public String getLedgerRangeByLevel(String level1, String level2) {
        StringBuilder nodeBuilder = threadLocalNodeBuilder.get();

        
        nodeBuilder.setLength(0);

        
        nodeBuilder.append("/ledgers/").append(level1).append("/").append(level2);

        return nodeBuilder.toString();
        
    }

    public static void main(String[] args) {
        BookKeeperLeakMock manager = new BookKeeperLeakMock();
        System.out.println("Generated path: " + manager.getLedgerRangeByLevel("000", "001"));
    }
}