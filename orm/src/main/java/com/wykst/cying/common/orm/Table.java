package com.wykst.cying.common.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Created by Cying on 2015/6/29.
 * <p>
 *     ��{@code @Table}ע������Ӧ���ݿ��һ�ű����б�ı��������Դ�Сд����������ͬ,
 *     ���಻����{@code private}�ģ������������ڲ��࣬����������{@code static}�ġ�
 * <p>
 *     �������б�{@link Column},{@link Key},{@link NotNull},{@link Unique}
 *     ע������Զ�Ӧ���ݱ��һ�У�ÿ�е����������Դ�Сд��������ͬ���������ʧ�ܡ�<br />
 * <p>
 *     ÿ�ж�Ӧ�����Զ�������{@code static}��{@code private}��{@code final}����������ʧ�ܡ�
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
	 * ���ñ�ı��������б�ı�����������ͬ�����Դ�Сд��ʽ������ͬ�����ʧ�ܡ�
	 *
	 * @return the table name
	 */
	String value() default "";

	/**
	 * �������ݿ����ƣ�Ĭ��ֵΪ{@link DatabaseConfiguration#DEFAULT_DATABASE_NAME}
	 *
	 * @return ���ݿ�����
	 */
	String database() default DatabaseConfiguration.DEFAULT_DATABASE_NAME;
}
