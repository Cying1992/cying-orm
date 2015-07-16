package com.wykst.cying.common.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * 被{@code @NotNull}注解的属性代表该表的一列。设置字段的Not Null属性及对null值特殊处理策略
 *
 * @see #value()
 * @see Table
 * @see Column
 * @see Key
 * @see Unique
 */
@Retention(CLASS)
@Target(FIELD)
public @interface NotNull {
	/**
	 * 被{@code @NotNull}注解的属性值为 {@code null}时,对 {@code null}值特殊处理的策略
	 * <p>
	 * 当返回{@link NullValueStrategy#NONE}时，则不对null值作任何特殊处理。
	 * </p>
	 * <p>
	 * <p>
	 * 当返回{@link NullValueStrategy#DEFAULT}时:
	 * <p>
	 * <br />若被注解的是数值类类型（{@link Integer},{@link Float},{@link Long},{@link Double},{@link java.math.BigDecimal}），则null值会被替换为{@code 0}值;
	 * <br />若被注解的是日期类型（{@link java.util.Date},{@link java.sql.Timestamp},{@link java.util.Calendar}），则null值会被替换为当前时间;
	 * <br />若被注解的是{@link String}类型，会被替换为空字符串{@code ""};
	 * <br />若被注解是的{@code byte[]}类型，则null值会被替换为{@code new byte[0]};
	 * <br />若被注解的是{@link Boolean}类型，则null值会被替换为{@code false}.
	 * <p>
	 * </p>
	 *
	 * @return {@code null}值处理的策略
	 */
	NullValueStrategy value() default NullValueStrategy.NONE;
}
