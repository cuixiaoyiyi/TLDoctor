package Threadlocal.Example1;

import java.util.List;
import java.util.ArrayList;

// 模拟一个极其常见的自定义包装类
class MyRequestContext {
    public StringBuilder sqlBuilder = new StringBuilder(); // 危险对象藏在这里！
    public String userId;
}

public class DummyTest2 {

    // ==========================================
    // 【攻击测试 1：逃避漏洞 (包装类逃逸)】
    // ==========================================
    // V1.0 会漏报，V2.0 将击穿 MyRequestContext，把里面的 sqlBuilder 揪出来。
    private static final ThreadLocal<MyRequestContext> WRAPPER_CONTEXT = new ThreadLocal<>();

    // ==========================================
    // 【攻击测试 2：逃避漏洞 (动态集合)】
    // ==========================================
    // V1.0 会漏报。V2.0 会发现这是个 List，而且整个类里没有 clear() 或 remove()，果断报警。
    private static final ThreadLocal<List<String>> DATA_LIST = new ThreadLocal<>();

    // ==========================================
    // 【防守测试：误报消除 (防御性编程)】
    // ==========================================
    // V1.0 会无脑报警，V2.0 会感知到底下的 capacity 和 remove，判定为安全代码并放过。
    private static final ThreadLocal<StringBuilder> SAFE_BUILDER = ThreadLocal.withInitial(StringBuilder::new);

    public void processData() {
        StringBuilder sb = SAFE_BUILDER.get();
        sb.append("some simulated heavy data");

        // 【完美的防御性编程：InfluxDB #1019 修复方案】
        if (sb.capacity() > 64 * 1024) {
            SAFE_BUILDER.remove(); // 容量超标，直接切断引用，让大对象进年轻代被回收
        } else {
            sb.setLength(0); // 容量安全，复用对象
        }
    }
}