package com.test.design.abstractFactory.color;

/**
 * @author zelei.fan
 * @date 2019/10/30 17:34
 */
public class Red implements Color {
    @Override
    public void fill() {
        System.out.println("this is red");
    }
}
