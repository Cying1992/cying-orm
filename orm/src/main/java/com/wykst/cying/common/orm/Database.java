package com.wykst.cying.common.orm;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: Cying
 * Date: 2015/7/14
 * Time: 14:03
 */
public class Database {
	/**
	 * 默认的数据库名称
	 */

	SQLiteOpenHelper sqLiteOpenHelper;
	private SQLiteDatabase sqLiteDatabase;

	final List<String> sqlList;

	private AtomicInteger lock = new AtomicInteger();


	Database() {
		sqlList = new ArrayList<>();
	}


	SQLiteDatabase open() {
		if (lock.incrementAndGet() == 1) {
			sqLiteDatabase = sqLiteOpenHelper.getWritableDatabase();
		}
		return sqLiteDatabase;
	}

	void close() {
		if (lock.decrementAndGet() == 0) {
			sqLiteDatabase.close();
		}
	}

}
