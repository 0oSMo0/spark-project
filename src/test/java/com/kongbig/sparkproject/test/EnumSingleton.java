package com.kongbig.sparkproject.test;

/**
 * Describe: 单例模式(双重检测,枚举实现)
 * Author:   kongbig
 * Data:     2018/3/7 15:36.
 */
public class EnumSingleton {
    public static void main(String[] args) {
        Singleton2 one = Singleton2.INSTANCE;
        Singleton2 two = Singleton2.INSTANCE;
        System.out.println(one.toString());
        one.test();
        one.setString("i am one");
        System.out.println(one == two);// true
        System.out.println(two.getString());
    }
}

enum Singleton2 {
    INSTANCE;

    private String string = name();
    // private String string; 上面这句也可以这样写，这样string就没有默认的值

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public void test() {
        System.out.println("测试开始...");
    }
}
