package cn.zhg.preprocessor.demo;

import  cn.zhg.preprocessor.inter.CompilerConstant;

public class SimpleService implements CompilerConstant {
    public void sayHello() {
        System.out.println("--------------");
        System.out.println(__LINE__);
        System.out.println("Hello");
        System.out.println(__CLASS_NAME__);
        System.out.println(__COLUMN__);
        System.out.println(__FILE_NAME__);
        System.out.println(__FILE_PATH__);
        System.out.println(__METHOD_NAME__);
        System.out.println(__LOCATION__);
    }
}
