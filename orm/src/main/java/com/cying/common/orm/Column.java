package com.cying.common.orm;


import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Created by Cying on 2015/6/29.
 * <p>
 * 被{@code @Column}注解的属性代表该表的一列。可以设置该列的列名，列名默认值和属性名的小写形式相同。<br />
 *
 * @see Table
 * @see Key
 * @see NotNull
 * @see Unique
 */
@Retention(CLASS)
@Target(FIELD)
public @interface Column {
	/**
	 * Set the column name.If it return empty value,the column name will be equal to the field name.
	 * <br />设置列的列名，会忽略空字符并转换成小写形式。如果它返回空字符串，则字段名会和属性名的小写形式相同。
	 *
	 * @return the column name.列名
	 */
	String value() default "";
}
