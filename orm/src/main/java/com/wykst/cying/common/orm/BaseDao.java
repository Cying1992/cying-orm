package com.wykst.cying.common.orm;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;


/**
 * 数据库操作类，不要去继承和实现它，系统会根据所有被{@link Table}注解的类来自动生成它的子类。
 *
 * @param <T> the entity class
 */

public abstract class BaseDao<T> {

	protected static void saveGenerateData(String databaseName, String createTableSQL) {
		ORM.saveGenerateData(databaseName, createTableSQL);
	}


	protected SQLiteDatabase getDatabase() {
		return ORM.open(getDatabaseName());
	}

	protected void closeDatabase() {
		ORM.close(getDatabaseName());
	}

	protected static <E extends Enum<E>> String convertEnumToString(E e) {
		return e == null ? "" : e.name();
	}

	protected static <E extends Enum<E>> E convertStringToEnum(Class<E> enumClass, String enumName) {
		try {

			return enumName == null ? null : Enum.valueOf(enumClass, enumName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected static <H> H convertNullValue(H value, H defaultValue) {
		return value == null ? defaultValue : value;
	}

	protected static BigDecimal convertStringToBigDecimal(String val) {
		new Date();
		try {
			return val == null ? null : new BigDecimal(val);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected static String convertBigDecimalToString(BigDecimal num) {
		return num == null ? null : num.toString();
	}

	protected static Calendar convertLongToCalendar(Long millis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		return calendar;
	}

	protected static Date convertLongToDate(Long millis) {
		Date date = new Date();
		date.setTime(millis);
		return date;
	}

	protected static Timestamp convertLongToTimestamp(Long millis) {
		return new Timestamp(millis);
	}

	protected static Long convertTimeToLong(Date date) {
		return date == null ? 0 : date.getTime();
	}

	protected static Long convertTimeToLong(Calendar calendar) {
		return calendar == null ? 0 : calendar.getTimeInMillis();
	}

	protected static Long convertTimeToLong(Timestamp timestamp) {
		return timestamp == null ? 0 : timestamp.getTime();
	}

	protected abstract T cursorToEntity(Cursor cursor);

	protected abstract ContentValues entityToValues(T entity);

	public abstract String getTableName();

	public abstract String getTableSQL();

	public abstract String getIndentityName();

	public abstract String getDatabaseName();

	/**
	 * @param entity the entity
	 * @return the primary key value ,start from 1;
	 */
	public abstract Long getIndentity(T entity);

	private List<T> cursorToEntityList(Cursor cursor) {
		List<T> result = new ArrayList<>();
		T entity;
		try {
			while (cursor.moveToNext()) {
				entity = cursorToEntity(cursor);
				result.add(entity);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			cursor.close();
		}
		return result;
	}

	/**
	 * @return first inserted entity
	 */
	public T first() {
		String query = "SELECT * FROM " + getTableName() + " ORDER BY " + getIndentityName() + " ASC LIMIT 1";
		List<T> list = findWithQuery(query);
		if (list.isEmpty()) return null;
		return list.get(0);
	}

	public T first(String whereClause, String... whereArgs) {
		List<T> list = find(whereClause, whereArgs, null, getIndentityName() + " ASC ", "1");
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * @return last inserted entity
	 */
	public T last() {
		String query = "SELECT * FROM " + getTableName() + " ORDER BY " + getIndentityName() + " DESC LIMIT 1";
		List<T> list = findWithQuery(query);
		if (list.isEmpty()) return null;
		return list.get(0);
	}

	public T last(String whereClause, String... whereArgs) {
		List<T> list = find(whereClause, whereArgs, null, getIndentityName() + " DESC ", "1");
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * @return the entity list in the table
	 */
	public List<T> listAll() {
		return find(null, null, null, null, null);
	}


	public List<T> listAll(String orderBy) {
		return find(null, null, null, orderBy, null);
	}

	public List<T> listPage(int count, int pageIndex) {
		return listPage(count, pageIndex, null, null);
	}

	public List<T> listPage(int count, int pageIndex, String orderBy) {
		return listPage(count, pageIndex, orderBy, null);
	}

	/**
	 * list all row by id asc
	 *
	 * @param count     the returned item count
	 * @param pageIndex the page index ,start from 0
	 * @return the data list
	 */
	public List<T> listEarlierPage(int count, int pageIndex) {
		String orderBy = getIndentityName() + " ASC ";
		return listPage(count, pageIndex, orderBy, null);
	}

	/**
	 * list all row by id desc
	 *
	 * @param count
	 * @param pageIndex
	 * @return 结果列表
	 */
	public List<T> listLaterPage(int count, int pageIndex) {
		String orderBy = getIndentityName() + " DESC ";
		return listPage(count, pageIndex, orderBy, null);
	}

	/**
	 * @param count       item count each page
	 * @param pageIndex   page index ,start from 0
	 * @param orderBy
	 * @param whereClause
	 * @param whereArgs
	 * @return
	 */
	public List<T> listPage(int count, int pageIndex, String orderBy, String whereClause, String... whereArgs) {
		if (count < 1 || pageIndex < 0) {
			throw new IllegalArgumentException("count and pageIndex can not be less than 1");
		}
		long offset = pageIndex * count;
		String limit = offset + "," + count;
		return find(whereClause, whereArgs, null, orderBy, limit);
	}

	public T findById(Long id) {
		List<T> list = find(getIndentityName() + "=?", new String[]{String.valueOf(id)}, null, null, "1");
		if (list.isEmpty()) return null;
		return list.get(0);
	}

	public T findById(Integer id) {
		return findById(Long.valueOf(id));
	}

	public Iterator<T> findAsIterator(String whereClause, String... whereArgs) {
		return findAsIterator(whereClause, whereArgs, null, null, null);
	}

	public Iterator<T> findAsIterator(String whereClause, String[] whereArgs, String groupBy, String orderBy, String limit) {
		Cursor cursor = getDatabase().query(getTableName(), null, whereClause, whereArgs, groupBy, null, orderBy, limit);
		return new EntityIterator(cursor);
	}

	public List<T> find(String whereClause, String[] whereArgs, String groupBy, String orderBy, String limit) {

		Cursor cursor = getDatabase().query(getTableName(), null, whereClause, whereArgs, groupBy, null, orderBy, limit);
		List<T> list = cursorToEntityList(cursor);
		closeDatabase();
		return list;
	}

	public List<T> find(String whereClause, String... whereArgs) {
		return find(whereClause, whereArgs, null, null, null);
	}

	public List<T> findWithQuery(String query, String... arguments) {
		Cursor cursor = getDatabase().rawQuery(query, arguments);
		return cursorToEntityList(cursor);
	}


	/**
	 * If the id of this entity is null or less than 1 ,it will ignore the id and insert this entity directly;
	 * Otherwise it will insert or replace this entity according to whether the id is exists or not;
	 * 保存实体类的数据。若它违反Unique约束，
	 *
	 * @param entity
	 * @return
	 */
	public long save(T entity) {
		ContentValues values = entityToValues(entity);
		Long entityId = getIndentity(entity);
		long id;
		if (entityId != null && entityId < 1) {
			values.putNull(getIndentityName());
		}
		id = getDatabase().insertWithOnConflict(getTableName(), null, values, SQLiteDatabase.CONFLICT_REPLACE);
		closeDatabase();
		return id;
	}


	public boolean delete(T entity) {
		boolean result = getDatabase().delete(getTableName(), getIndentityName() + "=?",
				new String[]{String.valueOf(getIndentity(entity))}) == 1;
		closeDatabase();
		return result;
	}

	public int deleteAll() {
		return deleteAll(null);
	}

	public int deleteAll(String whereClause, String... whereArgs) {
		int result = getDatabase().delete(getTableName(), whereClause, whereArgs);
		closeDatabase();
		return result;
	}

	public long count() {
		return count(null);
	}

	public long count(String whereClause, String... whereArgs) {
		long result = -1;
		String filter = (whereClause == null || whereClause.trim().isEmpty()) ? "" : " where " + whereClause;
		SQLiteStatement sqliteStatement;
		try {
			sqliteStatement = getDatabase().compileStatement("SELECT count(1) FROM " + getTableName() + filter);
		} catch (SQLiteException e) {
			e.printStackTrace();
			return result;
		}
		if (whereArgs != null) {
			for (int i = whereArgs.length; i != 0; i--) {
				sqliteStatement.bindString(i, whereArgs[i - 1]);
			}
		}
		try {
			result = sqliteStatement.simpleQueryForLong();
		} finally {
			sqliteStatement.close();
			closeDatabase();
		}
		return result;

	}

	public void executeSQL(String query, String... arguments) {
		getDatabase().execSQL(query, arguments);
		closeDatabase();
	}

	class EntityIterator implements Iterator<T> {

		final Cursor cursor;

		public EntityIterator(Cursor cursor) {
			this.cursor = cursor;
		}

		@Override
		public boolean hasNext() {
			return cursor != null && !cursor.isClosed() && !cursor.isAfterLast();
		}

		@Override
		public T next() {
			T entity = null;
			if (cursor == null || cursor.isAfterLast()) {
				throw new NoSuchElementException();
			}
			if (cursor.isBeforeFirst()) {
				cursor.moveToFirst();
			}

			try {
				entity = cursorToEntity(cursor);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				cursor.moveToNext();
				if (cursor.isAfterLast()) {
					cursor.close();
					closeDatabase();
				}
			}
			return entity;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}


}
