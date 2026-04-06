package Threadlocal.Example4;

import java.text.SimpleDateFormat;

public class DummyTest {

    
    
    
    
    
    
    public final ThreadLocal<SimpleDateFormat> BAD_THREAD_LOCAL = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };


    
    
    
    
    public final ThreadLocal<SimpleDateFormat> GOOD_THREAD_LOCAL_1 = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));


    
    
    
    
    public final ThreadLocal<String> GOOD_THREAD_LOCAL_2 = new ThreadLocal<>();


    
    
    
    
    public Runnable normalRunnable = new Runnable() {
        @Override
        public void run() {
            System.out.println("这是一个普通的匿名内部类，不该报警。");
        }
    };
}