package com.cying.common.orm;

/**
 * User: Cying
 * Date: 15-7-4
 * Time: 上午12:48
 */
public class SampleEntity implements Indentity {
    public String cd;

    @Override
    public long getIndentity() {
        return 0;
    }

    @Override
    public void setIndentity(long id) {

    }

    @Override
    public String getIndentityName() {
        return null;
    }
}
