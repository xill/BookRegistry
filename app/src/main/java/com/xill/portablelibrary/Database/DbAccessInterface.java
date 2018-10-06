package com.xill.portablelibrary.Database;

import android.database.Cursor;
import android.os.Environment;

import java.io.File;

/**
 * Created by Sami on 7/27/2017.
 */

public interface DbAccessInterface<T> {
	public String APP_FOLDER = "BookRegistry";
	public String DB_PATH = File.separator + "sdcard"//Environment.getExternalStorageDirectory()
			+ File.separator + APP_FOLDER
			+ File.separator;

	public boolean setData(String id);
	public boolean setData(String id, T data);
	public Cursor getData(String id);
	public Cursor getAll();
	public Cursor getAll(String id);
	public boolean removeData(String id);
}
