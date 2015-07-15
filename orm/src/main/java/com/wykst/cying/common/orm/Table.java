package com.wykst.cying.common.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Created by Cying on 2015/6/29.
 * <p>
 *     被{@code @Table}注解的类对应数据库的一张表，所有表的表名（忽略大小写）都不能相同,
 *     该类不能是{@code private}的，且若该类是内部类，则它必须是{@code static}的。
 * <p>
 *     该类所有被{@link Column},{@link Key},{@link NotNull},{@link Unique}
 *     注解的属性对应数据表的一列，每列的列名（忽略大小写）不能相同，否则编译失败。<br />
 * <p>
 *     每列对应的属性都不能是{@code static}或{@code private}或{@code final}，否则会编译失败。
 *
 * @see Column
 * @see Key
 * @see NotNull
 * @see Unique
 */
@Retention(CLASS)
@Target(TYPE)
public @interface Table {
	/**
	 * 设置表的表名，所有表的表名都不能相同，忽略大小写形式。若相同会编译失败。
	 *
	 * @return the table name
	 */
	String value() default "";

	/**
	 * 设置数据库名称，默认值为{@link DatabaseConfiguration#DEFAULT_DATABASE_NAME}
	 *
	 * @return 数据库名称
	 */
	String database() default DatabaseConfiguration.DEFAULT_DATABASE_NAME;
}
