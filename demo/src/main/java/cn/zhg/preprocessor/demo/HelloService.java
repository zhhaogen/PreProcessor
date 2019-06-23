package cn.zhg.preprocessor.demo;

import cn.zhg.preprocessor.inter.CompilerConstant;

public class HelloService implements CompilerConstant {
    private String field1 =__CLASS_NAME__;
    private String field2 =__CLASS_NAME__.toUpperCase();
    private String field3 ="aaa"+__FILE_NAME__+"aaa";
    public void sayHello() {
        System.out.println("field1字段: "+ field1);
        System.out.println("field2字段: "+ field2);
        System.out.println("field3字段: "+ field3);
        new InnerClass().sayHello();
       new Thread(()->{
           System.out.println("lambda 表达式:"+__LOCATION__);
       }).start();
        new InnerClass().sayMessage("abc");
    }
    private static class InnerClass{
        public void sayHello() {
            System.out.println("内部类:"+__LOCATION__);
            System.out.println(__LOCATION__+" ===");
            System.out.println("("+__LOCATION__+" )");
        }
        public void sayMessage(String __CLASS_NAME__) {
            //bug 不要使用__CLASS_NAME__作为参数名
            System.out.println("msg="+__CLASS_NAME__);
        }
    }
}
