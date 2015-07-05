package com.cying.common.orm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.atomic.AtomicInteger;

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
    static AtomicInteger lock=new AtomicInteger();

    public static void init(Context context, String dbName, int dbVersion, final SQLiteCallback sqLiteCallback) {
        sqLiteOpenHelper = new SQLiteOpenHelper(context, dbName, null, dbVersion) {
            @Override
            public void onCreate(SQLiteDatabase sqLiteDatabase) {

            }

            @Override
            public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
                sqLiteCallback.onUpgrade(sqLiteDatabase,oldVersion,newVersion);
            }

            @Override
            public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                sqLiteCallback.onDowngrade(db, oldVersion, newVersion);
            }
        };
    }

    static synchronized SQLiteDatabase open(){
        if(lock.incrementAndGet()==1){
            sqLiteDatabase=sqLiteOpenHelper.getWritableDatabase();
        }
        return sqLiteDatabase;
    }

    static synchronized void close(){
        if(lock.decrementAndGet()==0){
           sqLiteDatabase.close();
        }
    }

}
