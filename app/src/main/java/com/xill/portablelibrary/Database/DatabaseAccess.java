package com.xill.portablelibrary.Database;

import android.content.Context;
import android.util.Log;

import com.xill.portablelibrary.Database.IsbnRegistryDB.IsbnDbHelper;
import com.xill.portablelibrary.Database.ScannedDB.CacheDbHelper;
import com.xill.portablelibrary.Database.UserDB.ChangesDbHelper;
import com.xill.portablelibrary.Database.UserDB.UserDbHelper;

import java.io.File;

/**
 * Created by Sami on 7/25/2017.
 */

public class DatabaseAccess {

	private UserDbHelper userdb;
	private CacheDbHelper cachedb;
	private IsbnDbHelper isbndb;
	private ChangesDbHelper changesdb;
	private static Context context = null;
	private static DatabaseAccess instance = null;

	private DatabaseAccess(Context context) {
		// make sure app folder is present
		Log.v("DEBUG", "==== " + DbAccessInterface.DB_PATH + " =====");
		File appFolder = new File(DbAccessInterface.DB_PATH);
		try {
			if(appFolder.mkdir()) {
				Log.v(DatabaseAccess.class.getName(), "App folder created.");
			} else {
				Log.v(DatabaseAccess.class.getName(), "Failed to create app folder.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}


		userdb = new UserDbHelper(context);
		cachedb = new CacheDbHelper(context);
		isbndb = new IsbnDbHelper(context);
		changesdb = new ChangesDbHelper(context);
	}

	public static void initialize(Context context) {
		DatabaseAccess.context = context;
	}

	public static DatabaseAccess get() {
		if(instance == null) {
			instance = new DatabaseAccess(context);
		}
		return instance;
	}

	public DbAccessInterface getUserDb() {
		return userdb;
	}
	public DbAccessInterface getCacheDb() { return cachedb; }
	public DbAccessInterface getIsbnDb() { return isbndb; }
	public DbAccessInterface getChangesDb() { return changesdb; }

}
