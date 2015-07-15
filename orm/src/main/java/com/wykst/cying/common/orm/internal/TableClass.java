package com.wykst.cying.common.orm.internal;

import com.wykst.cying.common.orm.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * User: Cying
 * Date: 2015/7/9
 * Time: 14:36
 */
public class TableClass {

	static final String PARAM_SQL = "SQL";
	static final String PARAM_TABLE = "TABLE_NAME";
	static final String PARAM_DATABASE = "DATABASE_NAME";

	private TypeElement entityElement;
	private String packageName;
	private String entityClassName;
	private String databaseName;
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
		this.packageName = ORMProcessor.getPackageNameOf(this.entityElement);
		this.entityClassName = findEntityClassName(entityElement, packageName);
		this.daoClassName = entityClassName.replace(".", "$") + ORMProcessor.SUFFIX;
		this.tableName = findTableName(entityElement);
		this.databaseName = findDatabaseName(entityElement);
		this.columnFieldMap = new LinkedHashMap<>();

		prepareAllColumns(entityElement);
		prepareCreateTableSQL();
	}


	private String findEntityClassName(TypeElement type, String packageName) {
		int packageLen = packageName.length() + 1;
		return type.getQualifiedName().toString().substring(packageLen);
	}

	private String findTableName(Element type) {
		String tableName = type.getAnnotation(Table.class).value();
		if (tableName.isEmpty()) {
			tableName = type.getSimpleName().toString();
		}
		return tableName;
	}

	private String findDatabaseName(Element type) {
		return type.getAnnotation(Table.class).database();
	}

	static void checkValid(TypeElement entityElement) {
		ORMProcessor.isNotClassType(Table.class, entityElement);
		ORMProcessor.isClassInaccessibleViaGeneratedCode(entityElement);
		ORMProcessor.isBindingInWrongPackage(Table.class, entityElement);
	}


	private void preparePrimaryKey(VariableElement fieldElement) {
		if (hasPrimaryKey) {
			ORMProcessor.error(fieldElement, "@Class (%s) :@Field (%s) :table '%s' already has the primary key '%s'",
					entityClassName, fieldElement.getSimpleName(),
					tableName, primaryKeyColumnName);
		} else {
			TypeMirror typeMirror = fieldElement.asType();
			primaryKeyFieldName = fieldElement.getSimpleName().toString();
			primaryKeyColumnName = fieldElement.getAnnotation(Key.class).value().trim().toLowerCase();
			if (primaryKeyColumnName.isEmpty()) {
				primaryKeyColumnName = primaryKeyFieldName.toLowerCase();
			}

			hasPrimaryKey = true;
			String fieldClassName = ORMProcessor.getFieldClassNameOf(fieldElement);
			if (typeMirror.getKind() != TypeKind.LONG && !fieldClassName.equals(Long.class.getCanonicalName())) {
				ORMProcessor.error(fieldElement, "@Class (%s) :@Field (%s) :the primary key must be long or Long",
						entityClassName, primaryKeyFieldName);
			}
			ORMProcessor.checkKeyWord(fieldElement, primaryKeyColumnName, entityClassName, primaryKeyFieldName);

			if (columnFieldMap.containsKey(primaryKeyColumnName)) {
				ORMProcessor.error(fieldElement, "column '%s' is already exists ", primaryKeyColumnName);
			}

		}
	}

	private void prepareNormalColumn(VariableElement fieldElement) {
		ColumnField columnField = new ColumnField(fieldElement);
		String columnName = columnField.getColumnName();
		if (columnName.equalsIgnoreCase(primaryKeyColumnName) || columnFieldMap.containsKey(columnName)) {
			ORMProcessor.error(fieldElement, "column '%s' is already exists ", columnName);
		}
		columnFieldMap.put(columnName, columnField);
	}

	private void prepareAllColumns(TypeElement entityElement) {

		for (VariableElement fieldElement : ElementFilter.fieldsIn(entityElement.getEnclosedElements())) {
			if (ORMProcessor.isAnnotationPresent(Key.class, fieldElement)) {
				if (!ORMProcessor.isFieldInaccessibleViaGeneratedCode(entityElement, fieldElement)) {
					preparePrimaryKey(fieldElement);
				}

			} else if (ORMProcessor.isAnnotationPresent(Column.class, fieldElement) || ORMProcessor.isAnnotationPresent(NotNull.class, fieldElement) || ORMProcessor.isAnnotationPresent(Unique.class, fieldElement)) {
				if (!ORMProcessor.isFieldInaccessibleViaGeneratedCode(entityElement, fieldElement)) {
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
				.append("import com.wykst.cying.common.orm.BaseDao;\n")
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

		builder.append(brewGetStaticPart());
		builder.append(brewCursorToEntity());
		builder.append(brewEntityToValues());
		builder.append(brewGetTableName())
				.append(brewGetDatabaseName())
				.append(brewGetTableSQL())
				.append(brewGetIndentityName())
				.append(brewGetIndentity());

		builder.append("}\n");
		return builder.toString();
	}

	private String brewGetStaticPart() {
		StringBuilder builder = new StringBuilder();
		builder.append("    public static final String ")
				.append(PARAM_SQL)
				.append(" =\"")
				.append(createTableSQL).append("\";\n");

		builder.append("    public static final String ")
				.append(PARAM_TABLE)
				.append(" =\"")
				.append(tableName).append("\";\n");

		builder.append("    public static final String ")
				.append(PARAM_DATABASE)
				.append(" =\"")
				.append(databaseName).append("\";\n");

		builder.append("    static {\n")
				.append("       saveGenerateData(")
				.append(PARAM_DATABASE)
				.append(",")
				.append(PARAM_SQL)
				.append(");\n")
				.append("    }\n");
		return builder.toString();
	}

	private String brewGetTableSQL() {
		return "    @Override public String getTableSQL() { return " + PARAM_SQL + "; }\n";
	}

	private String brewGetTableName() {
		return "    @Override public String getTableName() { return " + PARAM_TABLE + "; }\n";
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

	private String brewGetDatabaseName() {
		return "    @Override public String getDatabaseName() { return " + PARAM_DATABASE + "; }\n";
	}

}
