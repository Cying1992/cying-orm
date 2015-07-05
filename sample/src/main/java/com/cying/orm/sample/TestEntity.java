package com.cying.orm.sample;

import com.cying.common.orm.*;

/**
 * User: Cying
 * Date: 2015/7/5
 * Time: 14:22
 */
@Table
public class TestEntity implements Indentity{
    @Key long id;
    @Key long myid;

    String fuck;
    @NotNull @Unique
    int my;
    double age;
    float phone;
    boolean b;
    byte[] content;

    public long getIndentity() {
        return 0;
    }

    public void setIndentity(long id) {

    }

    public String getIndentityName() {
        return null;
    }
}
