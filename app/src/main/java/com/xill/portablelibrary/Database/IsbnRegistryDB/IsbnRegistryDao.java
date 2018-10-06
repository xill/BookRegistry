package com.xill.portablelibrary.Database.IsbnRegistryDB;

import android.database.Cursor;

import com.xill.portablelibrary.Crawler.EntryObject;

/**
 * Created by Sami on 1/4/2018.
 */

public class IsbnRegistryDao {
	public static final String TABLE_NAME = "isbnRegistry";
	public static final String COL_1 = "ISBN";
	public static final String COL_2 = "TIME";
	public static final String COL_3 = "NAMES";
	public static final String COL_4 = "AUTHORS";
	public static final String COL_5 = "LANGUAGE";
	public static final String COL_6 = "PUBLISHERS";
	public static final String COL_7 = "THUMBNAIL";

	public static String TABLE_CREATE_QUERY = "create table " + TABLE_NAME +
			" (" +
			COL_1 + " INTEGER PRIMARY KEY, " +
			COL_2 + " INTEGER NOT NULL, " +
			COL_3 + " TEXT, " +
			COL_4 + " TEXT, " +
			COL_5 + " TEXT, " +
			COL_6 + " TEXT, " +
			COL_7 + " BLOB " +
			")";

	/* String used to join data arrays as string */
	public static String ARRAY_DELIM = "#=#";

	public static EntryObject toEntryObject(Cursor cursor) {
		return toEntryObject(cursor, 0);
	}

	public static EntryObject toEntryObject(Cursor cursor, int index) {
		EntryObject res = new EntryObject();
		cursor.moveToPosition(index);
		if(cursor.getCount() > 0) {
			res.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_2));
			res.names = cursor.getString(cursor.getColumnIndexOrThrow(COL_3)).split(ARRAY_DELIM);
			res.authors = cursor.getString(cursor.getColumnIndexOrThrow(COL_4)).split(ARRAY_DELIM);
			res.languages = cursor.getString(cursor.getColumnIndexOrThrow(COL_5)).split(ARRAY_DELIM);
			res.publishers = cursor.getString(cursor.getColumnIndexOrThrow(COL_6)).split(ARRAY_DELIM);
			res.thumbnail = cursor.getString(cursor.getColumnIndexOrThrow(COL_7));
		}
		return res;
	}
}
