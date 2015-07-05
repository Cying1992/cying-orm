package com.cying.common.orm.internal;

import com.cying.common.orm.Column;
import com.cying.common.orm.NotNull;
import com.cying.common.orm.Unique;

import javax.lang.model.element.VariableElement;

/**
 * User: Cying
 * Date: 15-7-4
 * Time: 下午4:06
 */
public class ColumnInfo {

    static final String INTEGER = "INTEGER";
    static final String INTEGER_NULL = "INTEGER NULL";
    static final String BLOB = "BLOB";
    static final String FLOAT = "FLOAT";
    static final String TEXT = "TEXT";

    enum ColumnType {
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
    }

    enum PrefixType {
        STRING("String"),
        LONG("Long"),
        FLOAT("Float"),
        DOUBLE("Double"),
        INT("Int"),
        BLOB("Blob");

        private final String type;

        PrefixType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }


    }

    enum FieldType {

        NULL(""),

        //PrefixType.INTEGER
        BOOLEAN(Boolean.class.getCanonicalName()),
        BOOLEAN_TYPE(Boolean.TYPE.getCanonicalName()),
        INTEGER(Integer.class.getCanonicalName()),
        INTEGER_TYPE(Integer.TYPE.getCanonicalName()),
        LONG(Long.class.getCanonicalName()),
        LONG_TYPE(Long.TYPE.getCanonicalName()),

        //PrefixType.BLOB
        BLOB(byte[].class.getCanonicalName()),

        //PrefixType.FLOAT
        DOUBLE(Double.class.getCanonicalName()),
        DOUBLE_TYPE(Double.TYPE.getCanonicalName()),
        FLOAT(Float.class.getCanonicalName()),
        FLOAT_TYPE(Float.TYPE.getCanonicalName()),

        //PrefixType.STRING
        // CHARACTER(Character.TYPE.getCanonicalName()),
        STRING(String.class.getCanonicalName());


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

//            if (CHARACTER.toString().equals(field))
//                return CHARACTER;

            return NULL;
        }
    }

    private String columnName;

    private String fieldName;

    private ColumnType columnType;

    private VariableElement entityField;

    private PrefixType cursorType;

    private PrefixType valuesType;

    private boolean columnNotNull, columnUnique;

    private String columnSQL;

    private String beforeConvertCursor, afterConvertCursor;
    private String beforeConvertValues, afterConvertValues;

    private VariableElement fieldElement;

    private String fieldClassName;

    public ColumnInfo(VariableElement fieldElement, String fieldClassName) throws Exception {
        this.fieldElement = fieldElement;
        this.fieldClassName = fieldClassName;
        initName();
        initType();
        initColumnSQL();
    }

    private void initName() {
        fieldName = fieldElement.getSimpleName().toString();
        Column column = fieldElement.getAnnotation(Column.class);
        if (column == null) {
            columnName = fieldName.toLowerCase();
        } else {
            columnName = column.value().toLowerCase();
            columnNotNull = column.notNull();
            columnUnique = column.unique();
        }

        NotNull notNull = fieldElement.getAnnotation(NotNull.class);
        if (notNull != null) {
            columnNotNull = true;
        }

        Unique unique = fieldElement.getAnnotation(Unique.class);
        if (unique != null) {
            columnUnique = true;
        }
    }

    private void initType() {
        FieldType fieldType = FieldType.getFieldType(fieldClassName);

        if (fieldType == FieldType.NULL) {
            throw new IllegalArgumentException("not support the field which type is  " + fieldClassName);
        }
        //determine columnType
        switch (fieldType) {

            case BOOLEAN:
            case BOOLEAN_TYPE:
            case INTEGER:
            case INTEGER_TYPE:
            case LONG:
            case LONG_TYPE:
                columnType = ColumnType.INTEGER;
                break;

            case DOUBLE:
            case DOUBLE_TYPE:
            case FLOAT:
            case FLOAT_TYPE:
                columnType = ColumnType.FLOAT;
                break;

            case STRING:
                //case CHARACTER:
                columnType = ColumnType.TEXT;
                break;

            case BLOB:
                columnType = ColumnType.BLOB;
                break;


        }

        //determine cursorType
        beforeConvertCursor = "";
        afterConvertCursor = "";
        beforeConvertValues = "";
        afterConvertValues = "";
        switch (fieldType) {

            case BOOLEAN:
            case BOOLEAN_TYPE:
                cursorType = PrefixType.STRING;
                afterConvertCursor = ".equals(\"1\")?true:false";
                break;

//            case CHARACTER:
//                beforeConvertCursor = "(char)";
//                beforeConvertValues = "(";
//                afterConvertValues = ")+\"\"";
//                cursorType = PrefixType.INT;
//                break;

            case INTEGER:
            case INTEGER_TYPE:
                cursorType = PrefixType.INT;
                break;

            case LONG:
            case LONG_TYPE:
                cursorType = PrefixType.LONG;
                break;

            case DOUBLE:
            case DOUBLE_TYPE:
                cursorType = PrefixType.DOUBLE;
                break;

            case FLOAT:
            case FLOAT_TYPE:
                cursorType = PrefixType.FLOAT;
                break;


            case STRING:
                cursorType = PrefixType.STRING;
                break;

            case BLOB:
                cursorType = PrefixType.BLOB;
                break;

        }
    }

    private void initColumnSQL() {
        StringBuilder builder = new StringBuilder();
        builder.append(columnName);
        builder.append(" ");
        builder.append(columnType);
        builder.append(columnNotNull ? " NOT NULL " : " ");
        builder.append(columnUnique ? " UNIQUE " : " ");
        columnSQL = builder.toString();
    }

    public String getColumnType() {
        return columnType.toString();
    }

    public String getColumnSQL() {
        return columnSQL;
    }

    String getColumnName() {
        return columnName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String brewCursorToEntity(String cursorParamName, String entityParamName) {

        return  String.format("     %s.%s=%s%s.get%s(%s.getColumnIndex(\"%s\"))%s;",
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
        return  String.format("   %s.put(\"%s\",%s%s.%s%s);",
                valuesParamName,
                columnName,
                beforeConvertValues,
                entityParamName,
                fieldName,
                afterConvertValues);
    }

}
