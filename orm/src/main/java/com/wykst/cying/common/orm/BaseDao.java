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

	/**
	 * 将generate的代码数据保存起来
	 */
	protected static void saveGenerateData(String databaseName, String createTableSQL) {
		ORM.saveGenerateData(databaseName, createTableSQL);
	}

	protected SQLiteDatabase getDatabase() {
		return ORM.open(mMetaData.databaseName);
	}

	protected void closeDatabase() {
		ORM.close(mMetaData.databaseName);
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

	private MetaData mMetaData;

	protected final void init(String databaseName, String tableName, String identityName, boolean hasNestedColumn) {
		if (mMetaData == null) {
			mMetaData = new MetaData(databaseName, tableName, identityName, hasNestedColumn);
		}
	}

	protected final void init(String databaseName, String tableName, String identityName) {
		init(databaseName, tableName, identityName, false);
	}

	static class MetaData {
		private final String databaseName, tableName, identityName;
		//是否有嵌套的字段
		private final boolean hasNestedColumn;

		MetaData(String databaseName, String tableName, String identityName, boolean hasNestedColumn) {
			this.databaseName = databaseName == null ? ORM.DEFAULT_DATABASE_NAME : databaseName;
			this.tableName = tableName;
			this.identityName = identityName;
			this.hasNestedColumn = hasNestedColumn;
		}
	}

	/**
	 * @param entity the entity
	 * @return the primary key value
	 */
	protected abstract Long getIdentity(T entity);

	protected abstract void setIdentity(T entity, Long value);

	protected abstract T cursorToEntity(Cursor cursor, Map<Class, Map<Long, Object>> map);

	protected abstract ContentValues entityToValues(T entity);

	private List<T> cursorToEntityList(Cursor cursor) {
		List<T> result = new ArrayList<>();
		T entity;
		try {
			Map<Class, Map<Long, Object>> map=getCachedEntityMap();
			while (cursor.moveToNext()) {
				ORM.debugCursor(mMetaData.tableName, cursor);
				entity = cursorToEntity(cursor, map);
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
		String query = "SELECT * FROM " + mMetaData.tableName + " ORDER BY " + mMetaData.identityName + " ASC LIMIT 1";
		List<T> list = findWithQuery(query);
		if (list.isEmpty()) return null;
		return list.get(0);
	}

	public T first(String whereClause, String... whereArgs) {
		List<T> list = find(whereClause, whereArgs, null, mMetaData.identityName + " ASC ", "1");
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * @return last inserted entity
	 */
	public T last() {
		String query = "SELECT * FROM " + mMetaData.tableName + " ORDER BY " + mMetaData.identityName + " DESC LIMIT 1";
		List<T> list = findWithQuery(query);
		if (list.isEmpty()) return null;
		return list.get(0);
	}

	public T last(String whereClause, String... whereArgs) {
		List<T> list = find(whereClause, whereArgs, null, mMetaData.identityName + " DESC ", "1");
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
		String orderBy = mMetaData.identityName + " ASC ";
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
		String orderBy = mMetaData.identityName + " DESC ";
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
		if (id != null) {
			List<T> list = find(mMetaData.identityName + "=?", new String[]{String.valueOf(id)}, null, null, "1");
			if (list.isEmpty()) return null;
			return list.get(0);
		}
		return null;
	}

	public T findById(Integer id) {
		if (id == null) return null;
		return findById(Long.valueOf(id));
	}

	public Iterator<T> findAsIterator(String whereClause, String... whereArgs) {
		return findAsIterator(whereClause, whereArgs, null, null, null);
	}

	public Iterator<T> findAsIterator(String whereClause, String[] whereArgs, String groupBy, String orderBy, String limit) {
		Cursor cursor = getDatabase().query(mMetaData.tableName, null, whereClause, whereArgs, groupBy, null, orderBy, limit);
		return new EntityIterator(cursor);
	}

	public List<T> find(String whereClause, String[] whereArgs, String groupBy, String orderBy, String limit) {

		Cursor cursor = getDatabase().query(mMetaData.tableName, null, whereClause, whereArgs, groupBy, null, orderBy, limit);
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
	 * 按照规定，主键值必须>0，若<=0则视为插入数据库。若大于0，
	 * 判断要保存的数据行是否违反Unique约束，若违反Unique约束，则更新对应的数据行的无Unique约束的列的值。
	 * 若不违反Unique约束，则直接插入数据。
	 */
	public Long save(T entity) {
		if(entity==null) return null;
		ContentValues values = entityToValues(entity);
		ORM.debugContentValues(mMetaData.tableName, values);
		Long entityId = getIdentity(entity);
		long id;
		if (entityId != null && entityId < 1) {
			values.putNull(mMetaData.identityName);
		}
		id = getDatabase().insertWithOnConflict(mMetaData.tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
		setIdentity(entity, id);
		closeDatabase();
		return id;
	}


	/**
	 * 级联删除
	 *
	 * @param entity
	 * @return 是否删除成功
	 */
	public boolean delete(T entity) {
		if(entity==null) return false;
		if (getIdentity(entity) != null) {
			boolean result = getDatabase().delete(mMetaData.tableName, mMetaData.identityName + "=?",
					new String[]{String.valueOf(getIdentity(entity))}) == 1;
			closeDatabase();

			return result;
		}
		return false;
	}

	public int deleteAll() {
		return deleteAll(null);
	}

	public int deleteAll(String whereClause, String... whereArgs) {
		int result = getDatabase().delete(mMetaData.tableName, whereClause, whereArgs);
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
			sqliteStatement = getDatabase().compileStatement("SELECT count(1) FROM " + mMetaData.tableName + filter);
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
		final Map<Class, Map<Long, Object>> map;
		public EntityIterator(Cursor cursor) {
			this.cursor = cursor;
			this.map=getCachedEntityMap();
		}

		@Override
		public boolean hasNext() {
			return cursor != null && !cursor.isClosed() && !cursor.isAfterLast();
		}

		@Override
		public T next() {
			T entity = null;
			if (this.cursor == null || this.cursor.isAfterLast()) {
				throw new NoSuchElementException();
			}
			if (this.cursor.isBeforeFirst()) {
				this.cursor.moveToFirst();
			}

			try {
				ORM.debugCursor(mMetaData.tableName, cursor);
				entity = cursorToEntity(this.cursor, this.map);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				this.cursor.moveToNext();
				if (this.cursor.isAfterLast()) {
					this.cursor.close();
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


	private Map<Class, Map<Long, Object>> getCachedEntityMap() {
		if (mMetaData.hasNestedColumn) {
			return new HashMap<>();
		}
		return null;
	}



	protected final void innerSave(Long id, Object obj, Map<Class, Map<Long, Object>> map) {
		if (map != null) {
			Class cls = obj.getClass();
			Map<Long, Object> innerMap;
			if (map.containsKey(cls)) {
				innerMap = map.get(cls);
			} else {
				innerMap = new HashMap<>();
				map.put(cls, innerMap);
			}
			innerMap.put(id, obj);
		}
	}

	private T innerFindById(Long id, Map<Class, Map<Long, Object>> map) {
		T entity = null;
		Cursor cursor = getDatabase().query(mMetaData.tableName, null, mMetaData.identityName + "=?", new String[]{String.valueOf(id)}, null, null, null, "1");
		if (cursor != null && cursor.moveToNext()) {
			ORM.debugCursor(mMetaData.tableName, cursor);
			entity = cursorToEntity(cursor, map);
			cursor.close();
		}
		closeDatabase();
		return entity;
	}

	@SuppressWarnings("unchecked")
	protected final <E> E innerFind(Long id, Class<E> cls, Map<Class, Map<Long, Object>> map) {
		if (map == null||id==null||cls==null) return null;
		Map<Long, Object> innerMap;
		if (map.containsKey(cls)) {
			innerMap = map.get(cls);
			if (innerMap.containsKey(id)) {
				return (E) innerMap.get(id);
			} else {
				return ORM.getDao(cls).innerFindById(id, map);
			}
		} else {
			innerMap = new HashMap<>();
			map.put(cls, innerMap);
			return ORM.getDao(cls).innerFindById(id, map);
		}
	}

	protected final <E> E innerFind(Cursor cursor,String columnName,Class<E> cls, Map<Class, Map<Long, Object>> map){
		return innerFind(cursor.getLong(cursor.getColumnIndex(columnName)),cls,map);
	}

	@SuppressWarnings("unchecked")
	protected final <E> Long innerGetId(E entity){
		if(entity!=null){
			return ORM.getDao((Class<E>)entity.getClass()).getIdentity(entity);
		}
		return null;
	}

	protected final Long innerGetSelfId(T entity){
		if(entity!=null){
			return getIdentity(entity);
		}
		return null;
	}


}
