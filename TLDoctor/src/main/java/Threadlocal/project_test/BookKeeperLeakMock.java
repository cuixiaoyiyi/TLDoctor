package Threadlocal.project_test;

/**
 * 模拟 Apache BookKeeper #4714 高水位线内存滞留 (由 QiuYucheng 报告)
 * 核心特征：ThreadLocal 缓存 StringBuilder，仅使用 setLength(0) 进行伪清理，底层大数组永不释放。
 */
public class BookKeeperLeakMock {

    // 1. 模拟 BookKeeper 里的 ThreadLocal StringBuilder
    private static final ThreadLocal<StringBuilder> threadLocalNodeBuilder =
            ThreadLocal.withInitial(StringBuilder::new);

    // 2. 模拟拼接 Zookeeper 路径的业务方法
    public String getLedgerRangeByLevel(String level1, String level2) {
        StringBuilder nodeBuilder = threadLocalNodeBuilder.get();

        // 【致命的伪清理】：仅重置游标，不释放物理内存 (char[])
        nodeBuilder.setLength(0);

        // 模拟路径拼接
        nodeBuilder.append("/ledgers/").append(level1).append("/").append(level2);

        return nodeBuilder.toString();
        // 结束时没有 ThreadLocal.remove()，也没有容量阈值判断
    }

    public static void main(String[] args) {
        BookKeeperLeakMock manager = new BookKeeperLeakMock();
        System.out.println("Generated path: " + manager.getLedgerRangeByLevel("000", "001"));
    }
}