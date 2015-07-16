package com.wykst.cying.common.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * ��{@code @NotNull}ע������Դ���ñ��һ�С������ֶε�Not Null���Լ���nullֵ���⴦�����
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
	 * ��{@code @NotNull}ע�������ֵΪ {@code null}ʱ,�� {@code null}ֵ���⴦��Ĳ���
	 * <p>
	 * ������{@link NullValueStrategy#NONE}ʱ���򲻶�nullֵ���κ����⴦��
	 * </p>
	 * <p>
	 * <p>
	 * ������{@link NullValueStrategy#DEFAULT}ʱ:
	 * <p>
	 * <br />����ע�������ֵ�����ͣ�{@link Integer},{@link Float},{@link Long},{@link Double},{@link java.math.BigDecimal}������nullֵ�ᱻ�滻Ϊ{@code 0}ֵ;
	 * <br />����ע������������ͣ�{@link java.util.Date},{@link java.sql.Timestamp},{@link java.util.Calendar}������nullֵ�ᱻ�滻Ϊ��ǰʱ��;
	 * <br />����ע�����{@link String}���ͣ��ᱻ�滻Ϊ���ַ���{@code ""};
	 * <br />����ע���ǵ�{@code byte[]}���ͣ���nullֵ�ᱻ�滻Ϊ{@code new byte[0]};
	 * <br />����ע�����{@link Boolean}���ͣ���nullֵ�ᱻ�滻Ϊ{@code false}.
	 * <p>
	 * </p>
	 *
	 * @return {@code null}ֵ����Ĳ���
	 */
	NullValueStrategy value() default NullValueStrategy.NONE;
}
