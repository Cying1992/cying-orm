package com.cying.common.orm.internal;

import com.cying.common.orm.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.HashMap;
import java.util.Map;

import static com.cying.common.orm.internal.ORMProcessor.*;


/**
 * User: Cying
 * Date: 2015/7/9
 * Time: 14:36
 */
public class TableClass {

	private TypeElement entityElement;
	private String packageName;
	private String entityClassName;
	private String tableName;
	private String daoClassName;
	private String primaryKeyColumnName;
	private String primaryKeyFieldName;
	private boolean hasPrimaryKey;
	private Map<String, ColumnField> columnFieldMap;
	private String createTableSQL;

	public TableClass(TypeElement entityElement) {
		checkValid(entityElement);

		this.entityElement = entityElement;
		this.packageName = getPackageNameOf(this.entityElement);
		this.entityClassName = findEntityClassName(entityElement, packageName);
		this.daoClassName = entityClassName.replace(".", "$") + SUFFIX;
		this.tableName = findTableName(entityElement);
		this.columnFieldMap = new HashMap<>();

		prepareAllColumns(entityElement);
		prepareCreateTableSQL();
	}


	private String findEntityClassName(TypeElement type, String packageName) {
		int packageLen = packageName.length() + 1;
		return type.getQualifiedName().toString().substring(packageLen);
	}

	private String findTableName(Element type) {
		String tableName = type.getAnnotation(Table.class).value().toLowerCase();
		if (tableName.isEmpty()) {
			tableName = type.getSimpleName().toString().toLowerCase();
		}
		return tableName;
	}

	static void checkValid(TypeElement entityElement) {
		isNotClassType(Table.class, entityElement);
		isClassInaccessibleViaGeneratedCode(entityElement);
		isBindingInWrongPackage(Table.class, entityElement);
	}


	private void preparePrimaryKey(VariableElement fieldElement) {
		if (hasPrimaryKey) {
			error(fieldElement, "@Class (%s) :@Field (%s) :table '%s' already has the primary key '%s'",
					entityClassName, fieldElement.getSimpleName(),
					tableName, primaryKeyColumnName);
		} else {
			TypeMirror typeMirror = fieldElement.asType();
			primaryKeyFieldName = fieldElement.getSimpleName().toString();
			primaryKeyColumnName = primaryKeyFieldName.toLowerCase();
			hasPrimaryKey = true;
			String fieldClassName = getFieldClassNameOf(fieldElement);
			if (typeMirror.getKind() != TypeKind.LONG && !fieldClassName.equals(Long.class.getCanonicalName())) {
				error(fieldElement, "@Class (%s) :@Field (%s) :the primary key must be long or Long",
						entityClassName, primaryKeyFieldName);
			}
			checkKeyWord(fieldElement, primaryKeyColumnName, entityClassName, primaryKeyFieldName);

			if (columnFieldMap.containsKey(primaryKeyColumnName)) {
				error(fieldElement, "column '%s' is already exists ", primaryKeyColumnName);
			}

		}
	}

	private void prepareNormalColumn(VariableElement fieldElement) {
		ColumnField columnField = new ColumnField(fieldElement);
		String columnName = columnField.getColumnName();
		if (columnName.equalsIgnoreCase(primaryKeyColumnName) || columnFieldMap.containsKey(columnName)) {
			error(fieldElement, "column '%s' is already exists ", columnName);
		}
		columnFieldMap.put(columnName, columnField);
	}

	private void prepareAllColumns(TypeElement entityElement) {

		for (VariableElement fieldElement : ElementFilter.fieldsIn(entityElement.getEnclosedElements())) {
				if (isAnnotationPresent(Key.class, fieldElement)) {
					if (!isFieldInaccessibleViaGeneratedCode(entityElement, fieldElement)) {
						preparePrimaryKey(fieldElement);
					}

				} else if (isAnnotationPresent(Column.class, fieldElement) || isAnnotationPresent(NotNull.class, fieldElement) || isAnnotationPresent(Unique.class, fieldElement)) {
					if (!isFieldInaccessibleViaGeneratedCode(entityElement, fieldElement)) {
						prepareNormalColumn(fieldElement);
					}
				}
		}
	}


	private void prepareCreateTableSQL() {
		StringBuilder builder = new StringBuilder();
		builder.append("CREATE TABLE [");
		builder.append(tableName);
		builder.append("] ([");
		builder.append(primaryKeyColumnName);
		builder.append("] INTEGER PRIMARY KEY AUTOINCREMENT");

		for (ColumnField columnField : columnFieldMap.values()) {
			builder.append(",");
			builder.append(columnField.getColumnSQL());
		}

		builder.append(");");
		createTableSQL = builder.toString();
	}

	String getTableName() {
		return tableName;
	}


	boolean hasPrimaryKey() {
		return hasPrimaryKey;
	}

	String getFqcn() {
		return packageName + "." + daoClassName;
	}

	String brewJava() {
		StringBuilder builder = new StringBuilder();
		builder.append("// Generated code from Cying-ORM. Do not modify!\n");

		builder.append("package ").append(packageName).append(";\n");

		//import
		builder.append("import android.content.ContentValues;\n")
				.append("import android.database.Cursor;\n")
				.append("import com.cying.common.orm.BaseDao;\n")
				.append("import com.cying.common.orm.NullValueStrategy;\n")
				.append("import java.math.BigDecimal;\n")
				.append("import java.sql.Timestamp;\n")
				.append("import java.util.Date;\n")
				.append("import java.util.Calendar;\n");

		//class
		builder.append("public class ")
				.append(daoClassName)
				.append(" extends BaseDao<")
				.append(entityClassName)
				.append("> {\n");

		builder.append(brewGetCreateTable());
		builder.append(brewCursorToEntity());
		builder.append(brewEntityToValues());
		builder.append(brewGetTableName())
				.append(brewGetTableSQL())
				.append(brewGetIndentityName())
				.append(brewGetIndentity());

		builder.append("}\n");
		return builder.toString();
	}

	private String brewGetCreateTable() {
		StringBuilder builder = new StringBuilder();
		builder.append("    private static String SQL=\"")
				.append(createTableSQL).append("\";\n");
		builder.append("    static {\n")
				.append("        saveSQL(SQL);\n")
				.append("    }\n");
		return builder.toString();
	}

	private String brewGetTableSQL() {
		return "    @Override public String getTableSQL() { return SQL; }\n";
	}

	private String brewGetTableName() {
		StringBuilder builder = new StringBuilder();
		builder.append("    @Override public String getTableName() { return \"");
		builder.append(tableName);
		builder.append("\"; }\n");
		return builder.toString();
	}

	private String brewCursorToEntity() {
		StringBuilder builder = new StringBuilder();
		builder.append("    @Override protected ")
				.append(entityClassName)
				.append(" cursorToEntity(Cursor cursor) {\n        ")
				.append(entityClassName)
				.append(" entity=new ")
				.append(entityClassName)
				.append("();\n");

		//primary key
		builder.append("        entity.")
				.append(primaryKeyFieldName)
				.append("=cursor.getLong(cursor.getColumnIndex(\"")
				.append(primaryKeyColumnName).append("\"));\n");

		for (ColumnField columnField : columnFieldMap.values()) {
			builder.append("        ");
			builder.append(columnField.brewCursorToEntity("cursor", "entity"));
			builder.append("\n");
		}

		builder.append("        return entity;\n    }\n");
		return builder.toString();
	}

	private String brewEntityToValues() {
		StringBuilder builder = new StringBuilder();
		builder.append("    @Override protected ContentValues entityToValues(")
				.append(entityClassName)
				.append(" entity) {\n        ContentValues values=new ContentValues();\n");

		//primary key
		builder.append("        values.put(\"")
				.append(primaryKeyColumnName)
				.append("\",entity.")
				.append(primaryKeyFieldName).append(");\n");

		for (ColumnField columnField : columnFieldMap.values()) {
			builder.append("        ");
			builder.append(columnField.brewEntityToValues("entity", "values"));
			builder.append("\n");
		}
		builder.append("        return values;\n    }\n");
		return builder.toString();
	}

	private String brewGetIndentityName() {
		StringBuilder builder = new StringBuilder();
		builder.append("    @Override public String getIndentityName() { ");
		builder.append("    return \"");
		builder.append(primaryKeyColumnName);
		builder.append("\"; }\n");
		return builder.toString();
	}

	private String brewGetIndentity() {
		StringBuilder builder = new StringBuilder();
		builder.append("    @Override public Long getIndentity(");
		builder.append(entityClassName).append(" entity) { return entity.");
		builder.append(primaryKeyFieldName).append("; }\n");

		return builder.toString();
	}

}
