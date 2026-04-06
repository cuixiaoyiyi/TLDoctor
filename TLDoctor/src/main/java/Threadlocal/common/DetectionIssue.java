package Threadlocal.common;



public class DetectionIssue {
    public String type;        
    public String message;     
    public String filePath;    
    public int lineNumber;     

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
