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



	private static final DatabaseGradeListener NullGradeListener = new DatabaseGradeListener() {
		@Override
		public void onGradeChanged(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

		}
	};

	private String databaseName;
	private int databaseVersion;
	private DatabaseGradeListener downGradeListener;
	private DatabaseGradeListener upGradeListener;

	/**
	 * ����Ĭ�����ݿ�����
	 */
	public DatabaseConfiguration() {
	}

	/**
	 * ���ݿ�����
	 * @param databaseName ���ݿ����ƻ�������пհ��ַ�
	 */
	public DatabaseConfiguration(String databaseName){
		if(databaseName!=null){
			String trimName=databaseName.trim();
			if(!trimName.isEmpty()){
				this.databaseName=trimName;
			}
		}
	}

	DatabaseGradeListener getDownGradeListener() {
		return downGradeListener == null ? NullGradeListener : downGradeListener;
	}

	DatabaseGradeListener getUpGradeListener() {
		return upGradeListener == null ? NullGradeListener : upGradeListener;
	}

	String getDatabaseName() {
		return databaseName == null ? ORM.DEFAULT_DATABASE_NAME : databaseName;
	}

	int getDatabaseVersion() {
		return databaseVersion < 1 ? 1 : databaseVersion;
	}


	/**
	 * �������ݿ�汾�����汾��С��1����ȡ1
	 *
	 * @param databaseVersion ���ݿ�汾��
	 * @return
	 */
	public DatabaseConfiguration setDatabaseVersion(int databaseVersion) {
		this.databaseVersion = databaseVersion;
		return this;
	}

	/**
	 * �������ݿ⽵���ص�����
	 *
	 * @param downGradeListener �����ص�����
	 * @return
	 */
	public DatabaseConfiguration setDownGradeListener(DatabaseGradeListener downGradeListener) {
		this.downGradeListener = downGradeListener;
		return this;
	}

	/**
	 * �������ݿ������ص�����
	 *
	 * @param upGradeListener �����ص�����
	 * @return
	 */
	public DatabaseConfiguration setUpGradeListener(DatabaseGradeListener upGradeListener) {
		this.upGradeListener = upGradeListener;
		return this;
	}

}
