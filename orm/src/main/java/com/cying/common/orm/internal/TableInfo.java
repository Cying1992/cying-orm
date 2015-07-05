package com.cying.common.orm.internal;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: Cying
 * Date: 15-7-4
 * Time: 下午1:34
 */
public class TableInfo {

    private Map<String, ColumnInfo> columnInfoMap = new LinkedHashMap<String, ColumnInfo>();

    private String tableName;
    private String entityClassName;
    private String primaryKeyColumnName;
    private String primaryKeyFieldName;
    private String createTableSQL;
    private String classPackageName;
    private String daoClassName;

    private boolean hasPrimaryKey;

    public TableInfo(String tableName,
            String classPackageName,String entityClassName, String daoClassName) {
        this.tableName=tableName;
        this.entityClassName = entityClassName;
        this.classPackageName = classPackageName;
        this.daoClassName = daoClassName;
    }

    public String getClassPackageName() {
        return classPackageName;
    }

    public String getEntityClassName() {
        return entityClassName;
    }

    public String getTableName() {
        return tableName;
    }

    String getFqcn() {
        return classPackageName + "." + daoClassName;
    }

    public void setPrimaryKeyFieldName(String primaryKeyFieldName) {
        this.primaryKeyFieldName = primaryKeyFieldName;
    }

    public void setPrimaryKeyColumnName(String primaryKeyColumnName){
      this.primaryKeyColumnName = primaryKeyColumnName;
        hasPrimaryKey=true;
    }

    public boolean hasPrimaryKey() {
        return hasPrimaryKey;
    }

    public String getPrimaryKeyColumnName() {
        return primaryKeyColumnName;
    }

    public String getPrimaryKeyFieldName() {
        return primaryKeyFieldName;
    }

    public void addColumn(ColumnInfo columnInfo) throws Exception{
        if (columnInfoMap.containsKey(columnInfo.getColumnName())){
              throw new IllegalArgumentException("column"+columnInfo.getColumnName()+" is already exists ");
        }
        columnInfoMap.put(columnInfo.getColumnName(), columnInfo);
    }

    public String getCreateTableSQL() {
        if (createTableSQL == null) {
            StringBuilder builder = new StringBuilder();
            builder.append(" CREATE TABLE ");
            builder.append(tableName);
            builder.append(" ( ");
            builder.append(primaryKeyColumnName);
            builder.append(" INTEGER PRIMARY KEY AUTOINCREMENT ");

            for (ColumnInfo columnInfo : columnInfoMap.values()) {
                builder.append(" , ");
                builder.append(columnInfo.getColumnSQL());
            }

            builder.append(" ) ");
            createTableSQL = builder.toString();
        }
        return createTableSQL;
    }

    String brewJava() {
        StringBuilder builder = new StringBuilder();
        builder.append("// Generated code from Cying-ORM. Do not modify!\n");
        builder.append("package ").append(classPackageName).append(";\n\n");
        builder.append("import android.content.ContentValues;\n");
        builder.append("import android.database.Cursor;\n");
        //builder.append("import com.cying.common.orm.ORMUtil;\n");
        builder.append("import com.cying.common.orm.BaseDao;\n\n");
        builder.append("public class ")
                .append(daoClassName)
                .append(" extends BaseDao<")
                .append(entityClassName)
                .append(">{\n\n");
        builder.append(brewGetCreateTable());
        builder.append(brewCursorToEntity());
        builder.append(brewEntityToValues());
        builder.append(brewGetTableName())
                .append(brewGetIndentity())
                .append(brewGetIndentityName())
                ;
        builder.append("}\n");
        return builder.toString();
    }

    String brewGetCreateTable() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append("    private static  String SQL=\"")
                .append(getCreateTableSQL()).append(";\";\n");
        builder.append("    static {\n  saveSQL(SQL);\n}\n");

//        builder.append(" @Override ");
//        builder.append("private  String getCreateTable(){\n");
//        builder.append("    return \"");
//        builder.append(getCreateTableSQL());
//        builder.append("\";\n}\n");
        return builder.toString();
    }

    String brewGetTableName() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append(" @Override ");
        builder.append("public String getTableName(){\n");
        builder.append("    return \"");
        builder.append(tableName);
        builder.append("\";\n}\n");
        return builder.toString();
    }

    String brewCursorToEntity() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n @Override public ")
                .append(entityClassName)
                .append(" cursorToEntity(Cursor cursor){\n     ")
                .append(entityClassName)
                .append(" entity=new ")
                .append(entityClassName)
                .append("();\n");

        //primary key
        builder.append("    entity.")
                .append(primaryKeyFieldName)
                .append("=cursor.getLong(cursor.getColumnIndex(\"")
                .append(primaryKeyColumnName).append("\"));\n");

        for (ColumnInfo columnInfo : columnInfoMap.values()) {
            builder.append(columnInfo.brewCursorToEntity("cursor", "entity"));
            builder.append("\n");
        }

        builder.append("    return entity;\n}\n");
        return builder.toString();
    }

    String brewEntityToValues() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n @Override public ContentValues entityToValues(")
                .append(entityClassName)
                .append(" entity){\n ContentValues values=new ContentValues();\n");

        for (ColumnInfo columnInfo : columnInfoMap.values()) {
            builder.append(columnInfo.brewEntityToValues("entity", "values"));
            builder.append("\n");
        }
        builder.append("  return values;\n}\n");
        return builder.toString();
    }

    public String brewGetIndentityName(){
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append(" @Override ");
        builder.append("public String getIndentityName(){\n");
        builder.append("    return \"");
        builder.append(primaryKeyColumnName);
        builder.append("\";\n}\n");
        return builder.toString();
    }

    public String brewGetIndentity(){
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append(" @Override ");
        builder.append("public long getIndentity(");
        builder.append(entityClassName).append(" entity){\n");
        builder.append("    return entity.");
        builder.append(primaryKeyFieldName).append(";\n}\n");

        return builder.toString();
    }

}

