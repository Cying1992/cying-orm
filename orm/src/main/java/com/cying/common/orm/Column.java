package com.cying.common.orm;


import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Created by Cying on 2015/6/29.
 * <p>
 * ��{@code @Column}ע������Դ���ñ��һ�С��������ø��е�����������Ĭ��ֵ����������Сд��ʽ��ͬ��<br />
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
	 * <br />�����е�����������Կ��ַ���ת����Сд��ʽ����������ؿ��ַ��������ֶ��������������Сд��ʽ��ͬ��
	 *
	 * @return the column name.����
	 */
	String value() default "";
}
