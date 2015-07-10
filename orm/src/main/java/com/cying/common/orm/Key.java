package com.cying.common.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * User: Cying
 * Date: 15-7-4
 * Time: 下午4:38
 */
@Retention(CLASS)
@Target(FIELD)
public @interface Key {
	String value() default "";
}
