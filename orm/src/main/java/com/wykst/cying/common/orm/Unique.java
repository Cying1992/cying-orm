package com.wykst.cying.common.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Created by Cying on 2015/6/29.
 * <p>
 * ��{@code @Unique}ע������Դ���ñ��һ�С������е�UniqueԼ��
 *
 * @see Table
 * @see Column
 * @see Key
 * @see NotNull
 */
@Retention(CLASS)
@Target(FIELD)
public @interface Unique {
}
