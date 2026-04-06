package Threadlocal.project_test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 模拟 Spotify Web API Java #450 漏洞 (由 QiuYucheng 报告并修复)
 * 核心特征：使用 static ThreadLocal 缓存非线程安全的 SimpleDateFormat，引发严重的内存与 ClassLoader 双重泄漏。
 */
public class SpotifyLeakMock {

    
    
    private static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));

    
    public static Date parseDefaultDate(String dateString) throws ParseException {
        
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