package com.xill.portablelibrary.Database.ScannedDB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.xill.portablelibrary.Database.DbAccessInterface;

/**
 * Created by Sami on 1/4/2018.
 */

public class CacheDbHelper extends SQLiteOpenHelper implements DbAccessInterface<Object> {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "cache.db";

	public CacheDbHelper(Context context) {
		super(context, DB_PATH + DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL(ScannedDao.TABLE_CREATE_QUERY);
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
		deleteDatabase(sqLiteDatabase);
		onCreate(sqLiteDatabase);
	}

	public void deleteDatabase(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL("DROP TABLE IF EXITS " + ScannedDao.TABLE_NAME);
	}

	@Override
	public boolean setData(String id) {
		SQLiteDatabase sq = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(ScannedDao.COL_1,id);
		values.put(ScannedDao.COL_2, System.currentTimeMillis());
		long result = sq.insert(ScannedDao.TABLE_NAME,null, values);
		return result != -1l;
	}

	@Override
	public boolean setData(String id, Object data) {
		return setData(id);
	}

	@Override
	public Cursor getData(String id) {
		SQLiteDatabase sq = getWritableDatabase();
		Cursor res = sq.rawQuery("Select * from " + ScannedDao.TABLE_NAME + " where " + ScannedDao.COL_1 + "=" + id, null);
		return res;
	}

	@Override
	public Cursor getAll() {
		SQLiteDatabase sq = getWritableDatabase();
		Cursor res = sq.rawQuery("Select * from " + ScannedDao.TABLE_NAME, null);
		return res;
	}

	@Override
	public Cursor getAll(String id) {
		return getAll();
	}

	@Override
	public boolean removeData(String id) {
		SQLiteDatabase sq = getWritableDatabase();
		return sq.delete(ScannedDao.TABLE_NAME,ScannedDao.COL_1 + "=" + id, null) > 0;
	}
}
