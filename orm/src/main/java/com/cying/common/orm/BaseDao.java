package com.cying.common.orm;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * User: Cying
 * Date: 15-7-3
 * Time: 下午11:39
 *
 * @param <T> 实体类类型
 */

public abstract class BaseDao<T> {

    protected static void saveSQL(String sql) {
        ORMUtil.putSQL(sql);
    }

    public abstract T cursorToEntity(Cursor cursor);

    public abstract ContentValues entityToValues(T entity);

    public abstract String getTableName();


    public abstract String getIndentityName();

    public abstract long getIndentity(T entity);

    public void save(T entity) {
        ORMUtil.open().insertWithOnConflict(getTableName(), null,
                entityToValues(entity), SQLiteDatabase.CONFLICT_REPLACE);
        ORMUtil.close();
    }

    public void delete(T entity) {
        ORMUtil.open().delete(getTableName(), getIndentityName() + "=?",
                new String[]{String.valueOf(getIndentity(entity))});
        ORMUtil.close();
    }

    public Iterator<T> findAsIterator(String whereCause, String... whereArgs) {
        Cursor cursor = ORMUtil.open().query(getTableName(), null, whereCause, whereArgs, null, null, null);
        return new EntityIterator(cursor);
    }

    protected class EntityIterator implements Iterator<T> {

        Cursor cursor;

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
