package Threadlocal.project_test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 模拟 Spotify Web API Java #450 漏洞 (由 QiuYucheng 报告并修复)
 * 核心特征：使用 static ThreadLocal 缓存非线程安全的 SimpleDateFormat，引发严重的内存与 ClassLoader 双重泄漏。
 */
public class SpotifyLeakMock {

    // 1. 模拟 SpotifyApi 中的危险静态字段
    // 开发者为了图省事，使用了 withInitial 和 Lambda 表达式
    private static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));

    // 2. 模拟暴露给外部调用的解析方法
    public static Date parseDefaultDate(String dateString) throws ParseException {
        // 只有 get() 和 parse()，全程没有 remove()
        return SIMPLE_DATE_FORMAT.get().parse(dateString);
    }

    public static void main(String[] args) {
        try {
            System.out.println("Parsed: " + parseDefaultDate("2023-01-01T12:00:00"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}