package com.cying.orm.sample;

import com.cying.common.orm.ORMUtil;

import java.io.IOException;

/**
 * User: Cying
 * Date: 2015/7/5
 * Time: 20:12
 */
public class MainClass {

    public static void main(String[] args){
        try {
            ORMUtil.loadAllEntityClass("com.cying.orm.sample");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
       // ORMUtil.getDao(TestEntity.class);
        System.out.println(ORMUtil.getTotalSQL());
    }
}
