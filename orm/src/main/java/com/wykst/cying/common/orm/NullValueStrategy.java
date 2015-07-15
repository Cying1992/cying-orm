package com.wykst.cying.common.orm;

import com.wykst.cying.common.orm.internal.FieldType;


/**
 * <p>
 * 对null值的特殊处理策略,
 *
 * @see NotNull
 */
public enum NullValueStrategy implements DefaultStratety {
	NONE {
		@Override
		public String getDefaultValue(FieldType fieldType) {
			return "null";
		}
	},
	DEFAULT {
		@Override
		public String getDefaultValue(FieldType fieldType) {
			switch (fieldType) {
				case BOOLEAN:
					return "false";

				case INTEGER:
				case LONG:
				case FLOAT:
				case DOUBLE:
					return "new " + convertToFirstUpperCase(fieldType) + "(0)";

				case STRING:
					return "\"\"";

				case DATE:
					return "new Date()";
				case TIMESTAMP:
					return "new Timestamp(System.currentTimeMillis())";
				case CALENDAR:
					return "Calendar.getInstance()";

				case BLOB:
					return "new byte[0])";

				case BIG_DECIMAL:
					return "new BigDecimal(\"0\"))";
			}
			return null;
		}
	};

	static String convertToFirstUpperCase(FieldType fieldType) {
		return fieldType.name().substring(0, 1) + fieldType.name().substring(1).toLowerCase();
	}
}
