package Threadlocal.Example1;

import java.text.SimpleDateFormat;

public class DummyTest3 {

    // 这是一个高危的重量级对象
    private static final ThreadLocal<SimpleDateFormat> FORMATTER =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    public void doBusinessLogic() {
        System.out.println("处理业务...");
        // 开发者很规范，业务处理完后调用了清理方法，并将 ThreadLocal 作为参数传入
        cleanUpTool(FORMATTER);
    }

    // 真正的清理逻辑被抽离到了另一个方法中
    private static void cleanUpTool(ThreadLocal<?> targetLocal) {
        // 在这里执行了 remove()
        targetLocal.remove();
    }
}