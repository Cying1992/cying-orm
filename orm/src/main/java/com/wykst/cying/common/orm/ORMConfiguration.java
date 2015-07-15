package com.wykst.cying.common.orm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: Cying
 * Date: 2015/7/15
 * Time: 22:55
 */
public class ORMConfiguration {

	private ORMConfiguration() {
	}

	public static class Builder {

		private Context context;
		private Map<String, DatabaseConfiguration> databaseConfigurationMap;
		private Set<Class<?>> tableEntityClassSet;

		public Builder(Context context) {
			this.context = context;
			this.databaseConfigurationMap = new HashMap<>();
			this.tableEntityClassSet = new HashSet<>();
		}

		public Builder register(Class<?> cls) {
			if (cls != null) {
				this.tableEntityClassSet.add(cls);
			}
			return this;
		}

		public Builder addDatabase(DatabaseConfiguration configuration) {
			if (configuration != null) {
				String databaseName = configuration.getDatabaseName();
				if (databaseConfigurationMap.containsKey(databaseName)) {
					throw new IllegalArgumentException("already exist the database configuration for " + databaseName);
				} else {
					this.databaseConfigurationMap.put(databaseName, configuration);
				}

			}
			return this;
		}

		public ORMConfiguration build() {
			ORMConfiguration ormConfiguration = new ORMConfiguration();
			for (Class<?> cls : this.tableEntityClassSet) {
				try {
					String daoClassName=ORM.getDaoClassName(cls);
					ORM.mDaoClassNameSet.add(daoClassName);
					Class.forName(daoClassName);
				} catch (Exception e) {
					throw new IllegalArgumentException("Can't find the dao class for entity class '" + cls.getName() + "' ! Please check whether it's be annotated by @Table");
				}
			}

			for (final DatabaseConfiguration configuration : databaseConfigurationMap.values()) {

				String databaseName = configuration.getDatabaseName();
				if (!ORM.mDatabaseMap.containsKey(databaseName)) {
					throw new IllegalArgumentException("not exist the database which name is " + databaseName);
				}

				final Database database = ORM.mDatabaseMap.get(databaseName);
				database.sqLiteOpenHelper = new SQLiteOpenHelper(this.context, configuration.getDatabaseName(), null, configuration.getDatabaseVersion()) {
					@Override
					public void onCreate(SQLiteDatabase sqLiteDatabase) {
						for (String sql : database.sqlList) {
							sqLiteDatabase.execSQL(sql);
						}
					}

					@Override
					public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
						configuration.getUpGradeListener().onGradeChanged(sqLiteDatabase, oldVersion, newVersion);
					}

					@Override
					public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
						configuration.getDownGradeListener().onGradeChanged(db, oldVersion, newVersion);
					}
				};

			}

			return ormConfiguration;
		}

	}


}
