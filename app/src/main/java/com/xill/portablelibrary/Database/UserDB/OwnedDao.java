package com.xill.portablelibrary.Database.UserDB;

/**
 * Created by Sami on 7/27/2017.
 */

public class OwnedDao {
	public static String TABLE_NAME = "owned";
	public static String COL_1 = "ISBN";
	public static String COL_2 = "TIME";

	public static String TABLE_CREATE_QUERY = "create table " + TABLE_NAME +
			" (" +
			COL_1 + " INTEGER PRIMARY KEY, " +
			COL_2 + " INTEGER NOT NULL)";
}
