package com.cying.common.orm;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.cying.common.orm.internal.ORMProcessor;
import dalvik.system.DexFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.cying.common.orm.internal.ORMProcessor.ANDROID_PREFIX;
import static com.cying.common.orm.internal.ORMProcessor.JAVA_PREFIX;
import static com.cying.common.orm.internal.ORMProcessor.SUFFIX;

/**
 * User: Cying
 * Date: 15-7-3
 * Time: 下午3:28
 */
public class ORMUtil {
    public interface SQLiteCallback {
        void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion);

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion);
    }

    static SQLiteOpenHelper sqLiteOpenHelper;
    static SQLiteDatabase sqLiteDatabase;
    static AtomicInteger lock = new AtomicInteger();

    static Map<Class<?>, BaseDao<?>> baseDaoMap = new LinkedHashMap<>();
    static final StringBuilder sqlBuilder = new StringBuilder("ppp:");
    private static boolean debug = false;

    private static final String TAG = "Cying-ORM";

    /**
     * Control whether debug logging is enabled.
     */
    public static void setDebug(boolean debug) {
        ORMUtil.debug = debug;
    }

    public static void loadAllEntityClass(Class<?>... entityClass) {
        for (Class<?> cls : entityClass) {
            // Class.forName()
        }
    }

    private static String getSourcePath(Context context) throws PackageManager.NameNotFoundException {
        return context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir;
    }


    public static void loadAllEntityClass(String packageName) throws IOException, ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> urls = classLoader.getResources("");
        while (urls.hasMoreElements()) {
            String classDirectoryName = urls.nextElement().getFile();
            if (classDirectoryName.contains("bin") || classDirectoryName.contains("classes")) {
                String seperator = System.getProperty("file.separator");
                String packagePath = packageName.replace(".", seperator);
                File classDirectory = new File(classDirectoryName + seperator + packagePath);
                String suffix = SUFFIX + ".class";

                String classFilePath;
                if (classDirectory.listFiles() != null) {
                    for (File filePath : Arrays.asList(classDirectory.listFiles())) {
                        classFilePath = filePath.getPath();
                        if (classFilePath.endsWith(suffix)) {
                            classFilePath = classFilePath.substring(classFilePath.indexOf(packagePath), classFilePath.lastIndexOf(".class")).replace(seperator, ".");
                            System.out.println("path=" + classFilePath);
                            Class.forName(classFilePath);
                        }
                    }
                }
            }
        }
    }

    public static void loadAllEntityClass(Context context, String... packageNames) throws PackageManager.NameNotFoundException, IOException, ClassNotFoundException {
        for (String packageName : packageNames) {
            String path = getSourcePath(context);
            DexFile dexfile = null;
            try {
                dexfile = new DexFile(path);
                Enumeration<String> dexEntries = dexfile.entries();
                while (dexEntries.hasMoreElements()) {
                    String className = dexEntries.nextElement();
                    if (className.endsWith(SUFFIX)) Class.forName(className);
                }
            } catch (NullPointerException e) {
                loadAllEntityClass(packageName);
            } finally {
                if (null != dexfile) dexfile.close();
            }
        }

    }

    public static void init(Context context, String dbName, int dbVersion, final SQLiteCallback sqLiteCallback, String... allEntityPackages) {

        try {
            loadAllEntityClass(context, allEntityPackages);
            sqLiteOpenHelper = new SQLiteOpenHelper(context, dbName, null, dbVersion) {
                @Override
                public void onCreate(SQLiteDatabase sqLiteDatabase) {
                    sqLiteDatabase.execSQL(sqlBuilder.toString());
                }

                @Override
                public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
                    sqLiteCallback.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
                }

                @Override
                public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                    sqLiteCallback.onDowngrade(db, oldVersion, newVersion);
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("cann't init ORM .", e);
        }
    }

    public static void putSQL(String sql) {
        sqlBuilder.append(sql);
    }

    public static String getTotalSQL() {
        return sqlBuilder.toString();
    }

    public static <T> BaseDao<T> getDao(Class<T> entityClass) {
        try {
            BaseDao<T> baseDao = findDao(entityClass);
            return baseDao;
        } catch (Exception e) {
            throw new RuntimeException("Unable to find dao class for " + entityClass.getName(), e);
        }
    }

    static <T> BaseDao<T> findDao(Class<T> entityClass) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        BaseDao<T> baseDao = null;
        if (baseDaoMap.containsKey(entityClass)) {
            baseDao = (BaseDao<T>) baseDaoMap.get(entityClass);
        } else {
            String clsName = entityClass.getName();
            if (clsName.startsWith(ANDROID_PREFIX) || clsName.startsWith(JAVA_PREFIX)) {
                if (debug) Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
                return baseDao;
            }
            try {
                Class<?> entityDaoClass = Class.forName(clsName + ORMProcessor.SUFFIX);
                baseDao = (BaseDao<T>) entityDaoClass.newInstance();
                baseDaoMap.put(entityClass, baseDao);
                if (debug) Log.d(TAG, "HIT: Loaded dao class.");

            } catch (ClassNotFoundException e) {
                if (debug) Log.d(TAG, "Not found");
                throw e;
            }

        }
        return baseDao;
    }

    static synchronized SQLiteDatabase open() {
        if (lock.incrementAndGet() == 1) {
            sqLiteDatabase = sqLiteOpenHelper.getWritableDatabase();
        }
        return sqLiteDatabase;
    }

    static synchronized void close() {
        if (lock.decrementAndGet() == 0) {
            sqLiteDatabase.close();
        }
    }

}
