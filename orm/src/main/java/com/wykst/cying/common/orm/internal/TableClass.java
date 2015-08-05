package com.wykst.cying.common.orm.internal;

import com.wykst.cying.common.orm.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * User: Cying
 * Date: 2015/7/9
 * Time: 14:36
 */
class TableClass {

	private static final String PARAM_SQL = "SQL";
	private static final String PARAM_TABLE = "TABLE_NAME";
	private static final String PARAM_DATABASE = "DATABASE_NAME";

	private final String packageName;
	private final String entityClassName;
	private final String databaseName;
	private final String tableName;
	private final String daoClassName;
	private String primaryKeyColumnName;
	private String primaryKeyFieldName;
	private boolean hasPrimaryKey;
	private final Map<String, ColumnField> columnFieldMap;
	private String createTableSQL;
	private String  entityClassQualifiedName;

	//保存字段类型是表的实体类的ColumnField
	private List<ColumnField> tableEntityColumnFieldList;

	public TableClass(TypeElement entityElement) {
		checkValid(entityElement);
		this.entityClassQualifiedName=entityElement.getQualifiedName().toString();
		this.packageName = ORMProcessor.getPackageNameOf(entityElement);
		this.entityClassName = findEntityClassName(entityElement, packageName);
		this.daoClassName = entityClassName.replace(".", "$") + ORMProcessor.SUFFIX;
		this.tableName = findTableName(entityElement);
		this.databaseName = findDatabaseName(entityElement);
		this.columnFieldMap = new LinkedHashMap<>();
		this.tableEntityColumnFieldList=new ArrayList<>();

		prepareAllColumns(entityElement);
		prepareCreateTableSQL();
	}


	private String findEntityClassName(TypeElement type, String packageName) {
		int packageLen = packageName.length() + 1;
		return type.getQualifiedName().toString().substring(packageLen);
	}

	private String findTableName(Element type) {
		String tableName = type.getAnnotation(Table.class).value().trim();
		if (tableName.isEmpty()) {
			tableName = type.getSimpleName().toString();
		}
		return tableName;
	}

	private String findDatabaseName(Element type) {
		return type.getAnnotation(Table.class).database().trim();
	}

	private static void checkValid(TypeElement entityElement) {
		ORMProcessor.isNotClassType(Table.class, entityElement);
		ORMProcessor.isClassInaccessibleViaGeneratedCode(entityElement);
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
		ColumnField columnField = new ColumnField(fieldElement,this.entityClassQualifiedName);
		String columnName = columnField.getColumnName();
		if (columnName.equalsIgnoreCase(primaryKeyColumnName) || columnFieldMap.containsKey(columnName)) {
			ORMProcessor.error(fieldElement, "column '%s' is already exists ", columnName);
		}
		columnFieldMap.put(columnName, columnField);
		if(columnField.isTableEntityClass()){
			tableEntityColumnFieldList.add(columnField);
		}
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
				.append("import com.wykst.cying.common.orm.ORM;\n")
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
				.append(brewGetIdentityName())
				.append(brewGetIdentity())
				.append(brewSetIdentity())
				.append(brewSaveCascade())
				.append(brewDeleteCascade());


		builder.append("}\n");
		return builder.toString();
	}

	/**
	 * 生成静态部分的代码，数据库名
	 * @return
	 */
	private String brewGetStaticPart() {
		StringBuilder builder = new StringBuilder();

		//创建表的语句
		builder.append("    public static final String ")
				.append(PARAM_SQL)
				.append(" =\"")
				.append(createTableSQL).append("\";\n");

		//表名
		builder.append("    public static final String ")
				.append(PARAM_TABLE)
				.append(" =\"")
				.append(tableName).append("\";\n");

		//数据库名称
		builder.append("    public static final String ")
				.append(PARAM_DATABASE)
				.append(" = ");
		if(databaseName.isEmpty()){
			//数据库名称为空用默认数据库名
			builder.append("getDefaultDatabaseName();\n");
		}else{
			builder.append("\"");
			builder.append(databaseName);
			builder.append("\";\n");
		}

		//将生成的数据库名称和创建表的语句收集起来以便创建数据库和表
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

	private String brewSaveCascade(){
		if(!tableEntityColumnFieldList.isEmpty()){
			StringBuilder builder = new StringBuilder();
			builder.append("    @Override public long saveCascade(");
			builder.append(entityClassName).append(" entity) {\n         long toRet = super.saveCascade(entity);\n");
			for(ColumnField columnField:tableEntityColumnFieldList){

				  builder .append("         if(entity.")
						  .append(columnField.getFieldName())
						  .append("!=null){")
						  .append(" ORM.getDao(")
						  .append(columnField.getFieldClassName())
						  .append(".class).saveCascade(")
						  .append("entity.")
						  .append(columnField.getFieldName())
						  .append("); }\n");
			}

			builder.append("         return toRet;\n    }\n");

			return builder.toString();
		}
		return "";
	}

	private String brewDeleteCascade(){
		if(!tableEntityColumnFieldList.isEmpty()){
			StringBuilder builder = new StringBuilder();
			builder.append("    @Override public boolean deleteCascade(");
			builder.append(entityClassName).append(" entity) {\n        boolean toRet = super.deleteCascade(entity);\n");
			for(ColumnField columnField:tableEntityColumnFieldList){

				builder .append("        if(entity.")
						.append(columnField.getFieldName())
						.append("!=null){")
						.append(" ORM.getDao(")
						.append(columnField.getFieldClassName())
						.append(".class).deleteCascade(")
						.append("entity.")
						.append(columnField.getFieldName())
						.append("); }\n");
			}
			builder.append("        return toRet;\n    }\n");

			return builder.toString();
		}
		return "";
	}

	private String brewGetIdentityName() {
		StringBuilder builder = new StringBuilder();
		builder.append("    @Override public String getIdentityName() { ");
		builder.append("    return \"");
		builder.append(primaryKeyColumnName);
		builder.append("\"; }\n");
		return builder.toString();
	}

	private String brewGetIdentity() {
		StringBuilder builder = new StringBuilder();
		builder.append("    @Override public Long getIdentity(");
		builder.append(entityClassName).append(" entity) { return entity.");
		builder.append(primaryKeyFieldName).append("; }\n");

		return builder.toString();
	}

	private String brewSetIdentity(){
		StringBuilder builder = new StringBuilder();
		builder.append("    @Override public void setIdentity(");
		builder.append(entityClassName).append(" entity,Long value) { entity.");
		builder.append(primaryKeyFieldName).append("=value; }\n");

		return builder.toString();
	}

	private String brewGetDatabaseName() {
		return "    @Override public String getDatabaseName() { return " + PARAM_DATABASE + "; }\n";
	}

}
