package com.cying.common.orm;

/**
 * User: Cying
 * Date: 15-7-3
 * Time: 下午11:58
 */
public interface Indentity {
    long getIndentity();

    void setIndentity(long id);

    default String getIndentityName() {
        return "id";
    }
}
