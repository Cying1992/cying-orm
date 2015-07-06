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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.cying.common.orm.internal.ORMProcessor.*;

/**
 * User: Cying
 * Date: 15-7-3
 * Time: 下午3:28
 */
public class ORMUtil {
    public interface SQLiteCallback {
        void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion);

        void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion);
    }

    private static SQLiteOpenHelper sqLiteOpenHelper;
    private static SQLiteDatabase sqLiteDatabase;
    private static AtomicInteger lock = new AtomicInteger();

    private static final Map<Class<?>, BaseDao<?>> baseDaoMap = new ConcurrentHashMap<>();

    static final Set<String> daoClassNameSet=new LinkedHashSet<>();

    static final StringBuilder sqlBuilder = new StringBuilder();

    private static boolean debug = false;

    private static final String TAG = "Cying-ORM";

    /**
     * Control whether debug logging is enabled.
     */
    public static void setDebug(boolean debug) {
        ORMUtil.debug = debug;
    }


    private static String getSourcePath(Context context) throws PackageManager.NameNotFoundException {
        return context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir;
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
                String suffix = SUFFIX + ".class";

                String classFilePath;
                File[] files=classDirectory.listFiles();
                if (files!= null) {
                    for (File filePath :files) {
                        classFilePath = filePath.getPath();
                        if (classFilePath.endsWith(suffix)) {
                            classFilePath = classFilePath.substring(classFilePath.indexOf(packagePath), classFilePath.lastIndexOf(".class")).replace(seperator, ".");
                            daoClassNameSet.add(classFilePath);
                            Class.forName(classFilePath);
                        }
                    }
                }
            }
        }
    }

    private static void loadAllEntityClass(Context context, String... packageNames) throws PackageManager.NameNotFoundException, IOException, ClassNotFoundException {
        for (String packageName : packageNames) {
            String path = getSourcePath(context);
            DexFile dexfile = null;
            try {
                dexfile = new DexFile(path);
                Enumeration<String> dexEntries = dexfile.entries();
                while (dexEntries.hasMoreElements()) {
                    String className = dexEntries.nextElement();
                    if (className.endsWith(SUFFIX)) {
                        daoClassNameSet.add(className);
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

    static void saveCreateTableSQL(String sql) {
        sqlBuilder.append(sql);
    }

    public static String getAllTableSQL() {
        return sqlBuilder.toString();
    }

    public static <T> BaseDao<T> getDao(Class<T> entityClass) {
        try {
            if(!daoClassNameSet.contains(entityClass.getName()+SUFFIX)){
                 throw new Exception("Unable to find dao class in the given packages for "+entityClass.getName());
            }
            return findDao(entityClass);
        } catch (Exception e) {
            throw new RuntimeException("Unable to find dao class for " + entityClass.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> BaseDao<T> findDao(Class<T> entityClass) throws IllegalAccessException, InstantiationException, ClassNotFoundException,ClassCastException {
        BaseDao<T> baseDao;
        if (baseDaoMap.containsKey(entityClass)) {
            baseDao = (BaseDao<T>) baseDaoMap.get(entityClass);
        } else {
            String clsName = entityClass.getName();
            if (clsName.startsWith(ANDROID_PREFIX) || clsName.startsWith(JAVA_PREFIX)) {
                if (debug) Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
                return null;
            }
                Class<?> entityDaoClass = Class.forName(clsName + ORMProcessor.SUFFIX);
                baseDao = (BaseDao<T>) entityDaoClass.newInstance();
                baseDaoMap.put(entityClass, baseDao);
                if (debug) Log.d(TAG, "HIT: Loaded dao class.");
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
