package com.wykst.cying.common.orm;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.wykst.cying.common.orm.internal.ORMProcessor;
import dalvik.system.DexFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: Cying
 * Date: 2015/7/15
 * Time: 13:29
 */
public class ORM {
	/**
	 * 是否已经初始化
	 */
	private static boolean mIsInit = false;

	/**
	 * 保存BaseDao子类的名称
	 */
	private static final Set<String> mDaoClassNameSet = new HashSet<>();

	/**
	 * 根据Entity类来找相应的BaseDao类
	 */
	private static final Map<Class<?>, BaseDao<?>> mDaoMap = new ConcurrentHashMap<>();


	/**
	 * 保存database相关信息
	 */
	private static final Map<String, Database> mDatabaseMap = new HashMap<>();

	private static boolean debug = false;

	private static final String TAG = "Cying-ORM";

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
	public static void setDebug(boolean debug) {
		ORM.debug = debug;
	}

	public static void init(Configuration configuration) {
		if (mIsInit) throw new RuntimeException("Can't init ORM twice!");
		String databaseName;
		Database database;
		for (Map.Entry<String, Database> entry : mDatabaseMap.entrySet()) {
			databaseName = entry.getKey();
			database = entry.getValue();
			if (database.sqLiteOpenHelper == null) {
				throw new RuntimeException("The configuration for database '" + databaseName + "' is missing, Please execute 'ORM.Configuration#addDatabase(DatabaseConfiguration)' .");
			}
		}
		mIsInit = true;
	}

	private static void checkInit() {
		if (!mIsInit) throw new RuntimeException("You don't init the ORM! Please execute ORM.init method .");
	}

	private static String getDaoClassName(Class<?> entityClass) {
		String clsName = entityClass.getName();
		String packageName = entityClass.getPackage().getName();
		String realClsName = packageName + "." + clsName.substring(packageName.length() + 1).replace(".", "$");
		return realClsName + ORMProcessor.SUFFIX;
	}

	/**
	 * 根据表的实体类找到相应的数据库操作类
	 *
	 * @param entityClass the table entity class
	 * @param <T>         实体类
	 * @return 实体类对应的数据库操作类
	 */
	public static <T> BaseDao<T> getDao(Class<T> entityClass) {
		checkInit();
		try {
			if (!mDaoClassNameSet.contains(getDaoClassName(entityClass))) {
				throw new Exception("Unable to find dao class in the given packages for " + entityClass.getName());
			}
			return findDao(entityClass);
		} catch (Exception e) {
			throw new RuntimeException("Unable to find dao class for " + entityClass.getName(), e);
		}
	}

	/**
	 * 保存实体到数据库
	 *
	 * @param entity 要保存的实体
	 * @param <T>    实体类型
	 * @return 插入的数据行主键值
	 */
	public static <T> long save(T entity) {
		BaseDao<T> baseDao = getDao(entity);
		return baseDao.save(entity);
	}

	/**
	 * 删除保存在数据库的实体
	 *
	 * @param entity 要删除的实体
	 * @param <T>    实体类型
	 * @return 是否删除成功
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


	private static void loadAllEntityClass(String packageName) throws IOException, ClassNotFoundException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		Enumeration<URL> urls = classLoader.getResources("");
		while (urls.hasMoreElements()) {
			String classDirectoryName = urls.nextElement().getFile();
			if (classDirectoryName.contains("bin") || classDirectoryName.contains("classes")) {
				String seperator = System.getProperty("file.separator");
				String packagePath = packageName.replace(".", seperator);
				File classDirectory = new File(classDirectoryName + seperator + packagePath);
				String suffix = ORMProcessor.SUFFIX + ".class";

				String classFilePath;
				File[] files = classDirectory.listFiles();
				if (files != null) {
					for (File filePath : files) {
						classFilePath = filePath.getPath();
						if (classFilePath.endsWith(suffix)) {
							classFilePath = classFilePath.substring(classFilePath.lastIndexOf(packagePath), classFilePath.lastIndexOf(".class")).replace(seperator, ".");
							mDaoClassNameSet.add(classFilePath);
							Class.forName(classFilePath);
						}
					}
				}
			}
		}
	}


	private static void loadAllEntityClass(Context context, Set<String> packageNames) throws PackageManager.NameNotFoundException, IOException, ClassNotFoundException {
		for (String packageName : packageNames) {
			String path = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir;
			DexFile dexfile = null;
			try {
				dexfile = new DexFile(path);
				Enumeration<String> dexEntries = dexfile.entries();
				while (dexEntries.hasMoreElements()) {
					String className = dexEntries.nextElement();
					if (className.endsWith(ORMProcessor.SUFFIX)) {
						mDaoClassNameSet.add(className);
						Class.forName(className);
					}
				}
			} catch (NullPointerException e) {
				loadAllEntityClass(packageName);
			} finally {
				if (null != dexfile) dexfile.close();
			}
		}

	}


	public static class Configuration {
		Set<String> entityPackages;
		Context context;
		Map<String, Database> databaseMap;

		public Configuration(Context context, String entityPackage, String... otherEntityPackage) {


			if (entityPackage == null) {
				throw new IllegalArgumentException("You must provide one package name which contains the table entities at least!");
			}

			this.context = context;
			this.entityPackages = new HashSet<>();
			this.databaseMap = new HashMap<>();

			if (otherEntityPackage != null) {
				Collections.addAll(this.entityPackages, otherEntityPackage);
			}
			try {
				loadAllEntityClass(this.context, this.entityPackages);
			} catch (Exception e) {
				throw new RuntimeException("Can't load all the entity class !", e);
			}

		}

		/**
		 * 添加数据库配置信息，必须是注解标注的数据库
		 *
		 * @param configuration 数据库配置信息
		 * @return
		 */
		public Configuration addDatabase(final DatabaseConfiguration configuration) {

			String databaseName = configuration.getDatabaseName();
			if (!mDatabaseMap.containsKey(databaseName)) {
				throw new IllegalArgumentException("not exist the database which name is " + databaseName);
			}

			final Database database = mDatabaseMap.get(databaseName);
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
			return this;
		}

	}

}
