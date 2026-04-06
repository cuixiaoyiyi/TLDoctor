package Threadlocal.project_test;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * 模拟 Micrometer #7184 类加载器泄漏漏洞 (由 QiuYucheng 报告)
 * 核心特征：使用 static ThreadLocal.withInitial() 缓存 JDK 格式化工具，引发 WebAppClassLoader 泄漏。
 */
public class MicrometerLeakMock {

    
    private static final ThreadLocal<NumberFormat> DECIMAL_OR_NAN = ThreadLocal.withInitial(() -> {
        DecimalFormat df = new DecimalFormat("#.0");
        return df;
    });

    private static final ThreadLocal<DecimalFormat> DECIMAL = ThreadLocal.withInitial(() -> new DecimalFormat("#.0"));

    
    public String formatValue(double value) {
        
        return DECIMAL.get().format(value);
    }

    public static void main(String[] args) {
        MicrometerLeakMock mock = new MicrometerLeakMock();
        System.out.println(mock.formatValue(10.5));
    }
}