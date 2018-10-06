package com.xill.portablelibrary.Database.UserDB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.xill.portablelibrary.Crawler.EntryObject;
import com.xill.portablelibrary.Database.DbAccessInterface;

/**
 * Created by Sami on 9/22/2018.
 */

public class ChangesDbHelper extends SQLiteOpenHelper implements DbAccessInterface<Object> {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "changesdat.db";

	public ChangesDbHelper(Context context) {
		super(context,DB_PATH + DATABASE_NAME,null,DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL(ChangesDao.TABLE_CREATE_QUERY);
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
		deleteDatabase(sqLiteDatabase);
		onCreate(sqLiteDatabase);
	}

	public void deleteDatabase(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ChangesDao.TABLE_NAME);
	}

	@Override
	public boolean setData(String id) {
		return false;
	}

	@Override
	public boolean setData(String id, Object data) {
		EntryObject entry = (EntryObject) data;
		SQLiteDatabase sq = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(ChangesDao.COL_1,id);
		values.put(ChangesDao.COL_2, entry.names[0]);
		values.put(ChangesDao.COL_3, entry.authors[0]);
		values.put(ChangesDao.COL_4, entry.languages[0]);
		values.put(ChangesDao.COL_5, entry.publishers[0]);
		values.put(ChangesDao.COL_6, entry.notes[0]);
		long result = sq.insert(ChangesDao.TABLE_NAME,null, values);
		return result != -1l;
	}

	@Override
	public Cursor getData(String id) {
		SQLiteDatabase sq = getWritableDatabase();
		Cursor res = sq.rawQuery("Select * from " + ChangesDao.TABLE_NAME + " where " + ChangesDao.COL_1 + "=" + id, null);
		return res;
	}

	@Override
	public Cursor getAll() {
		SQLiteDatabase sq = getWritableDatabase();
		Cursor res = sq.rawQuery("Select * from " + ChangesDao.TABLE_NAME, null);
		return res;
	}

	@Override
	public Cursor getAll(String id) {
		return getAll();
	}

	@Override
	public boolean removeData(String id) {
		SQLiteDatabase sq = getWritableDatabase();
		return sq.delete(ChangesDao.TABLE_NAME,ChangesDao.COL_1 + "=" + id, null) > 0;
	}
}
