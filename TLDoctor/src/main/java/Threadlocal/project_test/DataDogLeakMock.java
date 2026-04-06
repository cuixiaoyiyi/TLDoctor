package Threadlocal.project_test;

import java.text.NumberFormat;

/**
 * 模拟 DataDog java-dogstatsd-client #292 匿名内部类泄漏风险 (由 QiuYucheng 报告)
 * 核心特征：在静态上下文中，使用匿名内部类初始化 ThreadLocal，虽然不带 this$0，但依然存在类加载器绑定风险。
 */
public class DataDogLeakMock {

    
    
    protected static final ThreadLocal<NumberFormat> NUMBER_FORMATTER =
            new ThreadLocal<NumberFormat>() {
                @Override
                protected NumberFormat initialValue() {
                    NumberFormat format = NumberFormat.getInstance();
                    format.setGroupingUsed(false);
                    return format;
                }
            };

    
    public void processMetrics() {
        NumberFormat formatter = NUMBER_FORMATTER.get();
        System.out.println("Formatting metric: " + formatter.format(1024));
    }

    public static void main(String[] args) {
        DataDogLeakMock client = new DataDogLeakMock();
        client.processMetrics();
        
    }
}