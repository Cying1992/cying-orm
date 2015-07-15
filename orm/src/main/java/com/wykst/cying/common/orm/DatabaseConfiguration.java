package com.wykst.cying.common.orm;

import android.database.sqlite.SQLiteDatabase;

/**
 * User: Cying
 * Date: 2015/7/14
 * Time: 12:26
 */
public class DatabaseConfiguration {
	public interface DatabaseGradeListener {
		void onGradeChanged(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion);
	}

	//默认数据库名称
	public static final String DEFAULT_DATABASE_NAME = "db";

	static final DatabaseGradeListener NullGradeListener = new DatabaseGradeListener() {
		@Override
		public void onGradeChanged(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

		}
	};

	private String databaseName;
	private int databaseVersion;
	private DatabaseGradeListener downGradeListener;
	private DatabaseGradeListener upGradeListener;

	public DatabaseConfiguration() {
	}

	DatabaseGradeListener getDownGradeListener() {
		return downGradeListener == null ? NullGradeListener : downGradeListener;
	}

	DatabaseGradeListener getUpGradeListener() {
		return upGradeListener == null ? NullGradeListener : upGradeListener;
	}

	String getDatabaseName() {
		return databaseName == null ? DEFAULT_DATABASE_NAME : databaseName;
	}

	int getDatabaseVersion() {
		return databaseVersion < 1 ? 1 : databaseVersion;
	}

	/**
	 * 设置数据库名称，若不设置或为null，则默认值为{@link #DEFAULT_DATABASE_NAME}
	 *
	 * @param databaseName 数据库名称
	 * @return
	 */
	public DatabaseConfiguration setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
		return this;
	}

	/**
	 * 设置数据库版本，若版本号小于1，则取1
	 *
	 * @param databaseVersion 数据库版本号
	 * @return
	 */
	public DatabaseConfiguration setDatabaseVersion(int databaseVersion) {
		this.databaseVersion = databaseVersion;
		return this;
	}

	/**
	 * 设置数据库降级回调函数
	 *
	 * @param downGradeListener 降级回调函数
	 * @return
	 */
	public DatabaseConfiguration setDownGradeListener(DatabaseGradeListener downGradeListener) {
		this.downGradeListener = downGradeListener;
		return this;
	}

	/**
	 * 设置数据库升级回调函数
	 *
	 * @param upGradeListener 升级回调函数
	 * @return
	 */
	public DatabaseConfiguration setUpGradeListener(DatabaseGradeListener upGradeListener) {
		this.upGradeListener = upGradeListener;
		return this;
	}

}
