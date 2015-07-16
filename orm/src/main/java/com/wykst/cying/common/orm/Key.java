package com.wykst.cying.common.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * User: Cying
 * Date: 15-7-4
 * Time: 下午4:38
 * <p>
 * The table's primary key.Each table class must have only one Key-annotated Field which is long or Long type;
 * <p>
 * 被{@code @Key}注解的属性代表该表的主键，它会忽略在该属性上的{@link Column},{@link NotNull},{@link Unique}三个注解。
 * 每个表对应的实体类都必须有且只有一个被{@code @Key}注解的属性，
 * 同时它必须是{@code long}或者{@link Long}类型的。
 *
 * @see Table
 * @see Column
 * @see NotNull
 * @see Unique
 */
@Retention(CLASS)
@Target(FIELD)
public @interface Key {
	/**
	 * 设置主键列名名称，会忽略空字符并转换成小写形式。如果它返回空字符串，则字段名会和属性名的小写形式相同。
	 *
	 * @return the primary key name
	 */
	String value() default "";
}
