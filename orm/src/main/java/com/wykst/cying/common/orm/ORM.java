package com.wykst.cying.common.orm;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.wykst.cying.common.orm.internal.ORMProcessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: Cying
 * Date: 2015/7/15
 * Time: 13:29
 */
public class ORM {
	/**
	 * �Ƿ��Ѿ���ʼ��
	 */
	private static boolean mIsInit = false;

	/**
	 * ����BaseDao���������
	 */
	static final Set<String> mDaoClassNameSet = new HashSet<>();

	/**
	 * ����Entity��������Ӧ��BaseDao��
	 */
	private static final Map<Class<?>, BaseDao<?>> mDaoMap = new ConcurrentHashMap<>();


	/**
	 * ����database�����Ϣ
	 */
	static final Map<String, Database> mDatabaseMap = new HashMap<>();

	private static boolean debug = false;

	private static final String TAG = "Cying-ORM";

	//Ĭ�����ݿ�����
	static String DEFAULT_DATABASE_NAME = "db.db";

	private ORM() {
	}

	static void saveGenerateData(String databaseName, String createTableSQL) {
		Database database;
		if (mDatabaseMap.containsKey(databaseName)) {
			database = mDatabaseMap.get(databaseName);
		} else {
			database = new Database();
			mDatabaseMap.put(databaseName, database);
		}
		database.sqlList.add(createTableSQL);
	}

	/**
	 * Control whether debug logging is enabled.
	 */
	static void setDebug(boolean debug) {
		ORM.debug = debug;
	}

	public static void init(ORMConfiguration configuration) {
		if (configuration == null) throw new IllegalArgumentException("The param ORMConfiguration can't be null!");
		if (mIsInit) throw new RuntimeException("Can't init ORM twice!");
		String databaseName;
		Database database;
		for (Map.Entry<String, Database> entry : mDatabaseMap.entrySet()) {
			databaseName = entry.getKey();
			database = entry.getValue();
			if (database.sqLiteOpenHelper == null) {
				throw new RuntimeException("The configuration for database '" + databaseName + "' is missing .");
			}
		}
		mIsInit = true;
	}

	private static void checkInit() {
		if (!mIsInit) throw new RuntimeException("You don't init the ORM! Please execute ORM.init method .");
	}

	static String getDaoClassName(Class<?> entityClass) {
		String clsName = entityClass.getName();
		String packageName = entityClass.getPackage().getName();
		String realClsName = packageName + "." + clsName.substring(packageName.length() + 1).replace(".", "$");
		return realClsName + ORMProcessor.SUFFIX;
	}

	/**
	 * ���ݱ��ʵ�����ҵ���Ӧ�����ݿ������
	 *
	 * @param entityClass the table entity class
	 * @param <T>         ʵ����
	 * @return ʵ�����Ӧ�����ݿ������
	 */
	public static <T> BaseDao<T> getDao(Class<T> entityClass) {
		checkInit();
		try {
			if (!mDaoClassNameSet.contains(getDaoClassName(entityClass))) {
				throw new RuntimeException("Unable to find dao class in the given packages for " + entityClass.getName());
			}
			return findDao(entityClass);
		} catch (Exception e) {
			throw new RuntimeException("Unable to find dao class for " + entityClass.getName(), e);
		}
	}

	/**
	 * ����ʵ�嵽���ݿ�
	 *
	 * @param entity Ҫ�����ʵ��
	 * @param <T>    ʵ������
	 * @return ���������������ֵ
	 */
	public static <T> long save(T entity) {
		BaseDao<T> baseDao = getDao(entity);
		return baseDao.save(entity);
	}

	/**
	 * ɾ�����������ݿ��ʵ��
	 *
	 * @param entity Ҫɾ����ʵ��
	 * @param <T>    ʵ������
	 * @return �Ƿ�ɾ���ɹ�
	 */
	public static <T> boolean delete(T entity) {
		BaseDao<T> baseDao = getDao(entity);
		return baseDao.delete(entity);
	}

	@SuppressWarnings("unchecked")
	private static <T> BaseDao<T> getDao(T entity) {
		return getDao((Class<T>) entity.getClass());
	}

	@SuppressWarnings("unchecked")
	private static <T> BaseDao<T> findDao(Class<T> entityClass) throws IllegalAccessException, InstantiationException, ClassNotFoundException, ClassCastException {
		BaseDao<T> baseDao;
		if (mDaoMap.containsKey(entityClass)) {
			baseDao = (BaseDao<T>) mDaoMap.get(entityClass);
		} else {
			String clsName = entityClass.getName();
			if (clsName.startsWith(ORMProcessor.ANDROID_PREFIX) || clsName.startsWith(ORMProcessor.JAVA_PREFIX)) {
				if (debug) Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
				return null;
			}
			Class<?> entityDaoClass = Class.forName(getDaoClassName(entityClass));
			baseDao = (BaseDao<T>) entityDaoClass.newInstance();
			mDaoMap.put(entityClass, baseDao);
			if (debug) Log.d(TAG, "HIT: Loaded dao class.");
		}
		return baseDao;
	}

	static synchronized SQLiteDatabase open(String databaseName) {

		return mDatabaseMap.get(databaseName).open();
	}

	static synchronized void close(String databaseName) {

		mDatabaseMap.get(databaseName).close();
	}

	static void debugCursor(String tableName,Cursor cursor){
		if(debug){
			StackTraceElement[] stackTraceElements=Thread.currentThread().getStackTrace();
			StringBuilder builder=new StringBuilder();
			printTableAndMethod(tableName, builder);
			builder.append("- Cursor: ");
			DatabaseUtils.dumpCurrentRow(cursor, builder);
			System.out.print(builder.toString());
			//Log.d(TAG, builder.toString());
		}
	}

	static void debugContentValues(String tableName,ContentValues contentValues){
		  if(debug){
			  StringBuilder builder=new StringBuilder();

			  printTableAndMethod(tableName, builder);
			  builder.append("-ContentValues: {\n");
			  for(String key:contentValues.keySet()){
				  builder.append("   "+key+'='+contentValues.getAsString(key)+"\n");
			  }
			  builder.append("}\n");
			  //Log.d(TAG, builder.toString());
			  System.out.print(builder.toString());
		  }
	}

	private static void printTableAndMethod(String tableName,StringBuilder sb){
		sb.append("Table( ")
				.append(tableName)
				.append(" )-");
		StackTraceElement[] stackTraceElements=Thread.currentThread().getStackTrace();
		if(stackTraceElements.length>3) {
			StackTraceElement element;
			for (int i = 3; i < stackTraceElements.length; ++i) {
				element=stackTraceElements[i];
				sb.append("-> Method: ")
						.append(element.getMethodName())
						.append("(line:")
						.append(element.getLineNumber())
						.append(")");
			}
		}
	}
}
