package com.cying.common.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static com.cying.common.orm.NullValueStrategy.NONE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 *
 */
@Retention(CLASS)
@Target(FIELD)
public @interface NotNull {
	NullValueStrategy value() default NONE;
}
