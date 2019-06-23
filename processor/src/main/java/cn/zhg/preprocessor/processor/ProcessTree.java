package cn.zhg.preprocessor.processor;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Element;
import java.io.File;
import java.util.Objects;

public class ProcessTree<T extends JCTree> {
    private   Element element;
    private T tree;
    private ProcessTree parent;
    private CompilationUnitTree unit;
    /**
     * 最上层类名
     */
    private String elementName;
    /**
     * 文件路径
     */
    private String filePath;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 当前位置类名
     */
    private String className;
    /**
     * 当前位置方法名
     */
    private String methodName;
    public ProcessTree(ProcessTree parent, T tree) {
        this.tree = tree;
        this.parent = parent;
        this.unit = parent.unit;
        this.element=parent.element;
        this.elementName=parent.elementName;
        filePath=parent.filePath;
        fileName=parent.fileName;
        className=parent.className;
        methodName=parent.methodName;
    }

    public ProcessTree(Element element, CompilationUnitTree unit, T tree) {
        this.parent = null;
        this.unit = unit;
        this.tree = tree;
        this.element=element;
        this.elementName=element.toString();
        filePath=unit.getSourceFile().getName();
        fileName=new File(filePath).getName();
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public ProcessTree getParent() {
        return parent;
    }

    public T getTree() {
        return tree;
    }

    public CompilationUnitTree getUnit() {
        return unit;
    }
    public long getLineNumber()
    {
        return unit.getLineMap().getLineNumber(tree.pos);
    }

    public long getColumnNumber() {
        return unit.getLineMap().getColumnNumber(tree.pos);
    }
}
