package com.xill.portablelibrary.Database.IsbnRegistryDB;

import android.database.Cursor;

import com.xill.portablelibrary.Crawler.EntryObject;

/**
 * Created by Sami on 1/4/2018.
 */

public class IsbnPendingDao {
	public final static String TABLE_NAME = "isbnPending";
	public final static String COL_1 = "ISBN";
	public final static String COL_2 = "TIME";

	public static String TABLE_CREATE_QUERY = "create table " + TABLE_NAME +
			" (" +
			COL_1 + " INTEGER PRIMARY KEY, " +
			COL_2 + " INTEGER NOT NULL)";
}
