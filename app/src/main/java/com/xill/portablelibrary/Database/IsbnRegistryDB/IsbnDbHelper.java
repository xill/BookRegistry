package com.xill.portablelibrary.Database.IsbnRegistryDB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.xill.portablelibrary.Crawler.EntryObject;
import com.xill.portablelibrary.Database.DbAccessInterface;

/**
 * Created by Sami on 3/11/2018.
 */

public class IsbnDbHelper extends SQLiteOpenHelper implements DbAccessInterface<EntryObject> {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "isbn.db";

	public final String REGISTRY_KEY = "registry:";
	public final String PENDING_KEY = "pending:";

	public IsbnDbHelper(Context context) {
		super(context,DB_PATH + DATABASE_NAME,null,DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL(IsbnRegistryDao.TABLE_CREATE_QUERY);
		sqLiteDatabase.execSQL(IsbnPendingDao.TABLE_CREATE_QUERY);
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
		deleteDatabase(sqLiteDatabase);
		onCreate(sqLiteDatabase);
	}

	public void deleteDatabase(SQLiteDatabase sqLiteDatabase) {
		sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + IsbnRegistryDao.TABLE_NAME);
		sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + IsbnPendingDao.TABLE_NAME);
	}

	@Override
	public boolean setData(String id) {
		Cursor pendingCursor = getData(PENDING_KEY+id);
		Cursor registryCursor = getData(REGISTRY_KEY+id);
		// check if id is already received.
		// no point in doing it again.
		boolean inPending = pendingCursor.getCount() > 0;
		boolean inRegistry = registryCursor.getCount() > 0;
		// handle if missing from both.
		if(!(inPending || inRegistry)) {
			SQLiteDatabase sq = getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(IsbnPendingDao.COL_1,id);
			values.put(IsbnPendingDao.COL_2, System.currentTimeMillis());
			long result = sq.insert(IsbnPendingDao.TABLE_NAME,null, values);
			return result != -1l;
		}
		return false;
	}

	@Override
	public boolean setData(String id, EntryObject data) {
		// add data to registry
		SQLiteDatabase sq = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(IsbnRegistryDao.COL_1, id);
		values.put(IsbnRegistryDao.COL_2, System.currentTimeMillis());
		values.put(IsbnRegistryDao.COL_3, join(IsbnRegistryDao.ARRAY_DELIM, data.names));
		values.put(IsbnRegistryDao.COL_4, join(IsbnRegistryDao.ARRAY_DELIM, data.authors));
		values.put(IsbnRegistryDao.COL_5, join(IsbnRegistryDao.ARRAY_DELIM, data.languages));
		values.put(IsbnRegistryDao.COL_6, join(IsbnRegistryDao.ARRAY_DELIM, data.publishers));
		values.put(IsbnRegistryDao.COL_7, data.thumbnail);
		long result = sq.insert(IsbnRegistryDao.TABLE_NAME,null, values);
		return result != -1l;
	}

	@Override
	/**
	 * Get data from pending primarily.
	 */
	public Cursor getData(String id) {
		String idStr = id.replace(REGISTRY_KEY,"").replace(PENDING_KEY,"");
		String idCol = "";
		String tableName = "";
		if(id.startsWith(REGISTRY_KEY)) {
			tableName = IsbnRegistryDao.TABLE_NAME;
			idCol = IsbnRegistryDao.COL_1;
		} else {
			tableName = IsbnPendingDao.TABLE_NAME;
			idCol = IsbnPendingDao.COL_1;
		}
		SQLiteDatabase sq = getWritableDatabase();
		Cursor res = sq.rawQuery("Select * from " + tableName + " where " + idCol + "=" + idStr, null);
		return res;
	}

	@Override
	public Cursor getAll() {
		SQLiteDatabase sq = getWritableDatabase();
		Cursor res = sq.rawQuery("Select * from " + IsbnRegistryDao.TABLE_NAME, null);
		return res;
	}

	@Override
	public Cursor getAll(String id) {
		if(id.contains(REGISTRY_KEY))
			return getAll();
		else {
			SQLiteDatabase sq = getWritableDatabase();
			Cursor res = sq.rawQuery("Select * from " + IsbnPendingDao.TABLE_NAME, null);
			return res;
		}
	}

	@Override
	public boolean removeData(String id) {
		// remove data from pending first. then from registry.
		Cursor pendingCursor = getData(PENDING_KEY+id);
		String tableName = IsbnRegistryDao.TABLE_NAME;
		String colName = IsbnRegistryDao.COL_1;
		if(pendingCursor.getCount() > 0) {
			tableName = IsbnPendingDao.TABLE_NAME;
			colName = IsbnPendingDao.COL_1;
		}
		SQLiteDatabase sq = getWritableDatabase();
		return sq.delete(tableName,colName + "=" + id, null) > 0;
	}

	public boolean removeRegistryEntry(String id) {
		String tableName = IsbnRegistryDao.TABLE_NAME;
		String colName = IsbnRegistryDao.COL_1;
		SQLiteDatabase sq = getWritableDatabase();
		return sq.delete(tableName,colName + "=" + id, null) > 0;
	}

	private String join(String delim, String[] arr) {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < arr.length; ++i) {
			if(i > 0) builder.append(delim);
			builder.append(arr[i].trim());
		}
		return builder.toString();
	}
}
