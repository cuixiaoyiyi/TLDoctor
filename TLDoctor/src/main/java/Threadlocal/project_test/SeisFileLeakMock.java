package Threadlocal.project_test;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * 模拟 seisFile #41 DecimalFormat 导致的 ClassLoader 泄漏 (由 QiuYucheng 报告)
 * 核心特征：静态上下文中，通过匿名内部类缓存重量级的 JDK 格式化组件。
 */
public class SeisFileLeakMock {

    // 1. 模拟 DataRecord 中的历史遗留代码
    // 使用匿名内部类重写 initialValue，且完全没有 remove 机制
    private static final ThreadLocal<DecimalFormat> decimalFormat = new ThreadLocal<DecimalFormat>() {
        @Override
        protected DecimalFormat initialValue() {
            return new DecimalFormat("#####.####", new DecimalFormatSymbols(Locale.US));
        }
    };

    // 2. 模拟业务调用逻辑
    public String oneLineSummary(double numSamples, double sampleRate) {
        // 调用 get() 触发 initialValue
        DecimalFormat df = decimalFormat.get();
        return "Samples/Rate: " + df.format(numSamples / sampleRate);
    }

    public static void main(String[] args) {
        SeisFileLeakMock mock = new SeisFileLeakMock();
        System.out.println(mock.oneLineSummary(100.0, 2.0));
    }
}