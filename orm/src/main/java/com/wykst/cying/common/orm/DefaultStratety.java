package com.wykst.cying.common.orm;

import com.wykst.cying.common.orm.internal.FieldType;

/**
 * User: Cying
 * Date: 2015/7/14
 * Time: 16:33
 */
interface DefaultStratety {
	String getDefaultValue(FieldType fieldType);
}
