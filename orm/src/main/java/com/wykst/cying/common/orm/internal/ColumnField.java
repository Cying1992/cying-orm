package com.wykst.cying.common.orm.internal;

import com.wykst.cying.common.orm.Column;
import com.wykst.cying.common.orm.NotNull;
import com.wykst.cying.common.orm.NullValueStrategy;
import com.wykst.cying.common.orm.Unique;

import javax.lang.model.element.VariableElement;

/**
 * User: Cying
 * Date: 2015/7/9
 * Time: 14:36
 */
class ColumnField {

	private String columnName;

	private String fieldName;

	private FieldType.ColumnType columnType;

	private FieldType.CursorType cursorType;

	private boolean columnNotNull, columnUnique;

	private String columnSQL;

	private String beforeConvertCursor, afterConvertCursor;
	private String beforeConvertValues, afterConvertValues;

	private final VariableElement entityFieldElement;

	private final String fieldClassName;

	private final boolean isEnum;

	private NullValueStrategy nullValueStrategy;

	public ColumnField(VariableElement entityFieldElement) {
		this.entityFieldElement = entityFieldElement;
		this.fieldClassName = ORMProcessor.getFieldClassNameOf(entityFieldElement);
		this.isEnum = ORMProcessor.isEnum(entityFieldElement);

		prepareName();
		prepareType();
		prepareColumnSQL();
	}

	private void prepareName() {
		fieldName = entityFieldElement.getSimpleName().toString();

		if (ORMProcessor.isAnnotationPresent(Column.class, entityFieldElement)) {
			columnName = entityFieldElement.getAnnotation(Column.class).value().trim();
			if (columnName.isEmpty()) {
				columnName = fieldName;
			}
		} else {
			columnName = fieldName;
		}

		ORMProcessor.checkKeyWord(entityFieldElement, columnName, entityFieldElement.getEnclosingElement().getSimpleName().toString(), fieldName);

		if (ORMProcessor.isAnnotationPresent(NotNull.class, entityFieldElement)) {
			columnNotNull = true;
			nullValueStrategy = entityFieldElement.getAnnotation(NotNull.class).value();
		} else {
			columnNotNull = false;
			nullValueStrategy = NullValueStrategy.NONE;
		}

		columnUnique = ORMProcessor.isAnnotationPresent(Unique.class, entityFieldElement);

	}

	private void prepareType() {
		FieldType fieldType = isEnum ? FieldType.ENUM : FieldType.getFieldType(fieldClassName);
		if (fieldType == FieldType.NULL) {
			ORMProcessor.error(entityFieldElement, "not support the field which type is %s", fieldClassName);
		}
		cursorType = FieldType.CursorType.getPrefixType(fieldType);
		columnType = FieldType.ColumnType.getColumnType(fieldType);
		prepareToken(fieldType);
	}

	private static String convertToFirstUpperCase(FieldType fieldType) {
		return fieldType.name().substring(0, 1) + fieldType.name().substring(1).toLowerCase();
	}


	private void prepareToken(FieldType fieldType) {
		beforeConvertCursor = "";
		afterConvertCursor = "";
		beforeConvertValues = "";
		afterConvertValues = "";
		switch (fieldType) {

			case BOOLEAN:
				beforeConvertCursor = "\"1\".equals(";
				afterConvertCursor = ")?true:false";
				break;

			case BOOLEAN_TYPE:
				beforeConvertCursor = "\"1\".equals(";
				afterConvertCursor = ")?true:false";
				break;

			case DATE:
			case TIMESTAMP:
			case CALENDAR:
				beforeConvertValues = "convertTimeToLong(";
				afterConvertValues = ")";

				beforeConvertCursor = "convertLongTo" + convertToFirstUpperCase(fieldType) + "(";
				afterConvertCursor = ")";
				break;


			case ENUM:
				beforeConvertCursor = "convertStringToEnum(" + fieldClassName + ".class,";
				afterConvertCursor = ")";
				beforeConvertValues = "convertEnumToString(";
				afterConvertValues = ")";
				break;

			case BIG_DECIMAL:
				cursorType = FieldType.CursorType.STRING;
				beforeConvertCursor = "convertStringToBigDecimal(";
				afterConvertCursor = ")";
				beforeConvertValues = "convertBigDecimalToString(";
				afterConvertValues = ")";
				break;
		}

		//replace null value with default value
		if (columnNotNull && nullValueStrategy != NullValueStrategy.NONE) {
			String defaultValue = nullValueStrategy.getDefaultValue(fieldType);
			if (defaultValue != null) {
				beforeConvertValues += "convertNullValue(";
				afterConvertValues = "," + defaultValue + ")" + afterConvertValues;
			}
		}

	}

	private void prepareColumnSQL() {
		columnSQL = "[" + columnName + "] " + columnType + (columnNotNull ? " NOT NULL" : "") + (columnUnique ? " UNIQUE " : "");
	}

	String getColumnName() {
		return columnName;
	}

	String getColumnSQL() {
		return columnSQL;
	}

	public String brewCursorToEntity(String cursorParamName, String entityParamName) {

		return String.format("%s.%s=%s%s.get%s(%s.getColumnIndex(\"%s\"))%s;",
				entityParamName,
				fieldName,
				beforeConvertCursor,
				cursorParamName,
				cursorType,
				cursorParamName,
				columnName,
				afterConvertCursor);
	}

	public String brewEntityToValues(String entityParamName, String valuesParamName) {
		return String.format("%s.put(\"%s\",%s%s.%s%s);",
				valuesParamName,
				columnName,
				beforeConvertValues,
				entityParamName,
				fieldName,
				afterConvertValues);
	}


}
