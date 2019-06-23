package cn.zhg.preprocessor.inter;

/**
 * 预编译宏
 */
public interface CompilerConstant {
    /**
     * 行号
     */
    int __LINE__=-1;
    /**
     * 列号
     */
    int __COLUMN__=-1;
    /**
     * 方法名
     */
    String __METHOD_NAME__="(unknown)";
    /**
     * 类名
     */
    String __CLASS_NAME__="(unknown)";
    /**
     * 文件名
     */
    String __FILE_NAME__="(unknown)";
    /**
     * 当前位置
     */
    String __LOCATION__="";
    /**
     * 文件路径
     */
    String __FILE_PATH__="(unknown)";
}
