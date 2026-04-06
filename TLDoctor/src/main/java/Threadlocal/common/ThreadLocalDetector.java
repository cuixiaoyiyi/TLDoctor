package Threadlocal.common;



import com.github.javaparser.ast.CompilationUnit;
import java.util.List;


public interface ThreadLocalDetector {
    /**
     * @param cu 对应一个 Java 源文件的抽象语法树 (AST)
     * @param filePath 当前扫描的文件路径
     * @return 发现的漏洞列表
     */
    List<DetectionIssue> analyze(CompilationUnit cu, String filePath);
}