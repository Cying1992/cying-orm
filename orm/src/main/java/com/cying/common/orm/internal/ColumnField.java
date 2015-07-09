package com.cying.common.orm.internal;

import com.cying.common.orm.Column;
import com.cying.common.orm.NotNull;
import com.cying.common.orm.NullValueStrategy;
import com.cying.common.orm.Unique;

import javax.lang.model.element.VariableElement;

import static com.cying.common.orm.internal.FieldType.ColumnType;
import static com.cying.common.orm.internal.FieldType.CursorType;
import static com.cying.common.orm.internal.ORMProcessor.*;

/**
 * User: Cying
 * Date: 2015/7/9
 * Time: 14:36
 */
public class ColumnField {

	private String columnName;

	private String fieldName;

	private ColumnType columnType;

	private CursorType cursorType;

	private boolean columnNotNull, columnUnique;

	private String columnSQL;

	private String beforeConvertCursor, afterConvertCursor;
	private String beforeConvertValues, afterConvertValues;

	private VariableElement entityFieldElement;

	private String fieldClassName;

	private boolean isEnum;

	private NullValueStrategy nullValueStrategy;

	public ColumnField(VariableElement entityFieldElement) {
		this.entityFieldElement = entityFieldElement;
		this.fieldClassName = getFieldClassNameOf(entityFieldElement);
		this.isEnum = isEnum(entityFieldElement);

		prepareName();
		prepareType();
		prepareColumnSQL();
	}

	private void prepareName() {
		fieldName = entityFieldElement.getSimpleName().toString();
		Column column = entityFieldElement.getAnnotation(Column.class);
		if (column == null) {
			columnName = fieldName.toLowerCase();
		} else {
			columnName = column.value().toLowerCase();
			columnNotNull = column.notNull();
			columnUnique = column.unique();
		}

		checkKeyWord(entityFieldElement, columnName, entityFieldElement.getEnclosingElement().getSimpleName().toString(), fieldName);


		if (isAnnotationPresent(NotNull.class,entityFieldElement)) {
			NotNull notNull = entityFieldElement.getAnnotation(NotNull.class);
			columnNotNull = true;
			nullValueStrategy=notNull.value();
		}

		columnUnique = isAnnotationPresent(Unique.class, entityFieldElement);

	}

	private void prepareType() {
		FieldType fieldType = isEnum ? FieldType.ENUM : FieldType.getFieldType(fieldClassName);
		if (fieldType == FieldType.NULL) {
			error(entityFieldElement, "not support the field which type is %s", fieldClassName);
		}
		cursorType = CursorType.getPrefixType(fieldType);
		columnType = ColumnType.getColumnType(fieldType);
		prepareToken(fieldType);
	}

	private String convertToFirstUpperCase(FieldType fieldType){
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
				if(columnNotNull){
					beforeConvertValues="Boolean.valueOf(convertNullValue(";
					afterConvertValues=nullValueStrategy==NullValueStrategy.DEFAULT?",NullValueStrategy.DEFAULT))":",NullValueStrategy.NONE))";
				}
				break;

			case INTEGER:
			case LONG:
			case FLOAT:
			case DOUBLE:
				if(columnNotNull){
					beforeConvertValues=convertToFirstUpperCase(fieldType)+".valueOf(convertNullValue(";
					afterConvertValues=nullValueStrategy==NullValueStrategy.DEFAULT?",NullValueStrategy.DEFAULT))":",NullValueStrategy.NONE))";
				}
				break;


			case BOOLEAN_TYPE:
				beforeConvertCursor = "\"1\".equals(";
				afterConvertCursor = ")?true:false";
				break;

			case DATE:
			case TIMESTAMP:
			case CALENDAR:
				beforeConvertValues = "convertTimeToLong(";
				afterConvertValues = nullValueStrategy==NullValueStrategy.DEFAULT?",NullValueStrategy.DEFAULT)":",NullValueStrategy.NONE)";
				beforeConvertCursor = "convertLongTo" +convertToFirstUpperCase(fieldType) + "(";
				afterConvertCursor = ")";
				break;

			case ENUM:
				beforeConvertCursor = "convertStringToEnum(" + fieldClassName + ".class,";
				afterConvertCursor = ")";
				beforeConvertValues = "convertEnumToString(";
				afterConvertValues = ")";
				break;

			case BIG_DECIMAL:
				cursorType = CursorType.STRING;
				beforeConvertCursor = "convertStringToBigDecimal(";
				afterConvertCursor = ")";
				beforeConvertValues = "convertBigDecimalToString(";
				afterConvertValues = ")";
				break;
		}

	}

	private void prepareColumnSQL() {
		StringBuilder builder = new StringBuilder();
		builder.append("[").append(columnName);
		builder.append("] ");
		builder.append(columnType);
		builder.append(columnNotNull ? " NOT NULL" : "");
		builder.append(columnUnique ? " UNIQUE " : "");
		columnSQL = builder.toString();
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
