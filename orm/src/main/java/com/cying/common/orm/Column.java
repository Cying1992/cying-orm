package com.cying.common.orm;


import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Created by Cying on 2015/6/29.
 */
@Retention(CLASS)
@Target(FIELD)
public @interface Column {
	String value();

	boolean unique() default false;

	boolean notNull() default false;
}
