package com.xill.portablelibrary.Database.ScannedDB;

/**
 * Created by Sami on 1/4/2018.
 */

public class ScannedDao {
	public static String TABLE_NAME = "scanned";
	public static String COL_1 = "ISBN";
	public static String COL_2 = "TIME";

	public static String TABLE_CREATE_QUERY = "create table " + TABLE_NAME +
			" (" +
			COL_1 + " INTEGER PRIMARY KEY, " +
			COL_2 + " INTEGER NOT NULL)";
}
