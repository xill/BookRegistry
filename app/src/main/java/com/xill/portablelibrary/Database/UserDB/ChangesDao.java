package com.xill.portablelibrary.Database.UserDB;

import android.database.Cursor;

import com.xill.portablelibrary.Crawler.EntryObject;

/**
 * Created by Sami on 9/22/2018.
 */

public class ChangesDao {
	public static final String TABLE_NAME = "isbnDataChanges";
	public static final String COL_1 = "ISBN";
	public static final String COL_2 = "NAMES";
	public static final String COL_3 = "AUTHORS";
	public static final String COL_4 = "LANGUAGE";
	public static final String COL_5 = "PUBLISHERS";
	public static final String COL_6 = "NOTES";

	public static String TABLE_CREATE_QUERY = "create table " + TABLE_NAME +
			" (" +
			COL_1 + " INTEGER PRIMARY KEY, " +
			COL_2 + " TEXT, " +
			COL_3 + " TEXT, " +
			COL_4 + " TEXT, " +
			COL_5 + " TEXT, " +
			COL_6 + " TEXT " +
			")";

	public static EntryObject toEntryObject(Cursor cursor) {
		return toEntryObject(cursor, 0);
	}

	public static EntryObject toEntryObject(Cursor cursor, int index) {
		EntryObject res = new EntryObject();
		if(cursor.getCount() > 0) {
			cursor.moveToPosition(index);
			res.names = new String[]{cursor.getString(cursor.getColumnIndexOrThrow(COL_2))};
			res.authors = new String[]{cursor.getString(cursor.getColumnIndexOrThrow(COL_3))};
			res.languages = new String[]{cursor.getString(cursor.getColumnIndexOrThrow(COL_4))};
			res.publishers = new String[]{cursor.getString(cursor.getColumnIndexOrThrow(COL_5))};
			res.notes = new String[]{cursor.getString(cursor.getColumnIndexOrThrow(COL_6))};
		}

		return res;
	}
}
