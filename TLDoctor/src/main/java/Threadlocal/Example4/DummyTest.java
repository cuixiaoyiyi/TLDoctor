package Threadlocal.Example4;

import java.text.SimpleDateFormat;

public class DummyTest {

    // ------------------------------------------------------------------------
    // 【危险示例】类型 IV：典型的“死亡拥抱”
    // ------------------------------------------------------------------------
    // 开发者为了图方便，直接使用匿名内部类重写了 initialValue() 方法。
    // 这会在编译后生成一个 DummyTest$1.class，它隐式地持有外部类 DummyTest 的强引用。
    // 如果 DummyTest 是一个非单例的业务对象，它将因为 ThreadLocalMap 的生命周期而被泄漏。
    public final ThreadLocal<SimpleDateFormat> BAD_THREAD_LOCAL = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };


    // ------------------------------------------------------------------------
    // 【安全示例 1】使用 JDK 8 的 withInitial (推荐做法)
    // ------------------------------------------------------------------------
    // 使用 Lambda 表达式，不会生成持有外部类引用的非静态内部类实例，非常安全。
    public final ThreadLocal<SimpleDateFormat> GOOD_THREAD_LOCAL_1 = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));


    // ------------------------------------------------------------------------
    // 【安全示例 2】标准的普通实例化
    // ------------------------------------------------------------------------
    // 没有使用匿名内部类，安全。
    public final ThreadLocal<String> GOOD_THREAD_LOCAL_2 = new ThreadLocal<>();


    // ------------------------------------------------------------------------
    // 【干扰项示例】其他类的匿名内部类
    // ------------------------------------------------------------------------
    // 这个测试用来验证我们的检测器会不会“草木皆兵”，把普通的匿名内部类也当成 ThreadLocal 误用。
    public Runnable normalRunnable = new Runnable() {
        @Override
        public void run() {
            System.out.println("这是一个普通的匿名内部类，不该报警。");
        }
    };
}