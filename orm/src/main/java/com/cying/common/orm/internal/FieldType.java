package com.cying.common.orm.internal;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * User: Cying
 * Date: 2015/7/9
 * Time: 20:55
 */
public enum FieldType {

	NULL(""),
	ENUM("enum"),

	//CursorType.INTEGER
	BOOLEAN(Boolean.class.getCanonicalName()),
	BOOLEAN_TYPE(Boolean.TYPE.getCanonicalName()),
	INTEGER(Integer.class.getCanonicalName()),
	INTEGER_TYPE(Integer.TYPE.getCanonicalName()),
	LONG(Long.class.getCanonicalName()),
	LONG_TYPE(Long.TYPE.getCanonicalName()),

	//CursorType.BLOB
	BLOB(byte[].class.getCanonicalName()),

	//CursorType.FLOAT
	DOUBLE(Double.class.getCanonicalName()),
	DOUBLE_TYPE(Double.TYPE.getCanonicalName()),
	FLOAT(Float.class.getCanonicalName()),
	FLOAT_TYPE(Float.TYPE.getCanonicalName()),

	//CursorType.STRING
	// CHARACTER(Character.TYPE.getCanonicalName()),
	STRING(String.class.getCanonicalName()),

	CALENDAR(Calendar.class.getCanonicalName()),
	DATE(Date.class.getCanonicalName()),
	TIMESTAMP(Timestamp.class.getCanonicalName()),

	BIG_DECIMAL(BigDecimal.class.getCanonicalName());


	private final String type;

	FieldType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return type;
	}

	static FieldType getFieldType(String field) {
		if (BOOLEAN.toString().equals(field))
			return BOOLEAN;

		if (BOOLEAN_TYPE.toString().equals(field))
			return BOOLEAN_TYPE;

		if (INTEGER.toString().equals(field))
			return INTEGER;

		if (INTEGER_TYPE.toString().equals(field))
			return INTEGER_TYPE;

		if (LONG.toString().equals(field))
			return LONG;

		if (LONG_TYPE.toString().equals(field))
			return LONG_TYPE;

		if (BLOB.toString().equals(field))
			return BLOB;

		if (DOUBLE.toString().equals(field))
			return DOUBLE;


		if (DOUBLE_TYPE.toString().equals(field))
			return DOUBLE;

		if (FLOAT.toString().equals(field))
			return FLOAT;

		if (FLOAT_TYPE.toString().equals(field))
			return FLOAT_TYPE;

		if (STRING.toString().equals(field))
			return STRING;

		if (DATE.toString().equals(field))
			return DATE;

		if (TIMESTAMP.toString().equals(field))
			return TIMESTAMP;

		if (CALENDAR.toString().equals(field))
			return CALENDAR;

		if (BIG_DECIMAL.toString().equals(field))
			return BIG_DECIMAL;

		return NULL;
	}

	enum ColumnType {
		NULL(""),
		INTEGER("INTEGER"),
		BLOB("BLOB"),
		FLOAT("FLOAT"),
		TEXT("TEXT");

		private final String type;

		ColumnType(String type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return type;
		}

		public static ColumnType getColumnType(FieldType fieldType) {
			switch (fieldType) {
				case BOOLEAN:
				case BOOLEAN_TYPE:
				case INTEGER:
				case INTEGER_TYPE:
				case LONG:
				case LONG_TYPE:
				case DATE:
				case TIMESTAMP:
				case CALENDAR:
					return INTEGER;

				case DOUBLE:
				case DOUBLE_TYPE:
				case FLOAT:
				case FLOAT_TYPE:
					return FLOAT;

				case STRING:
				case BIG_DECIMAL:
				case ENUM:
					//case CHARACTER:
					return TEXT;


				case BLOB:
					return BLOB;

				default:
					return NULL;
			}

		}
	}

	enum CursorType {
		NULL(""),
		STRING("String"),
		LONG("Long"),
		FLOAT("Float"),
		DOUBLE("Double"),
		INT("Int"),
		BLOB("Blob");

		private final String type;

		CursorType(String type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return type;
		}

		public static CursorType getPrefixType(FieldType fieldType) {
			switch (fieldType) {

				case BOOLEAN:
				case BOOLEAN_TYPE:
					return CursorType.STRING;

				case INTEGER:
				case INTEGER_TYPE:
					return CursorType.INT;

				case DATE:
				case TIMESTAMP:
				case CALENDAR:
				case LONG:
				case LONG_TYPE:
					return CursorType.LONG;

				case DOUBLE:
				case DOUBLE_TYPE:
					return CursorType.DOUBLE;

				case FLOAT:
				case FLOAT_TYPE:
					return CursorType.FLOAT;

				case ENUM:
				case BIG_DECIMAL:
				case STRING:
					return CursorType.STRING;

				case BLOB:
					return CursorType.BLOB;
				default:
					return NULL;

			}
		}

	}
}
