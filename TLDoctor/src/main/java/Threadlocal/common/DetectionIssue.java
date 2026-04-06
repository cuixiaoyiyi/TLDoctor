package Threadlocal.common;


// 报警结果封装类
public class DetectionIssue {
    public String type;        // 误用类型
    public String message;     // 详细描述
    public String filePath;    // 文件路径
    public int lineNumber;     // 所在行号

    public DetectionIssue(String type, String message, String filePath, int lineNumber) {
        this.type = type;
        this.message = message;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s at %s:%d", type, message, filePath, lineNumber);
    }
}
