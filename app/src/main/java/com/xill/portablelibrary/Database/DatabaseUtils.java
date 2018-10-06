package com.xill.portablelibrary.Database;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.xill.portablelibrary.Database.IsbnRegistryDB.IsbnDbHelper;
import com.xill.portablelibrary.Database.ScannedDB.CacheDbHelper;
import com.xill.portablelibrary.Database.UserDB.UserDbHelper;

/**
 * Created by Sami on 7/1/2018.
 */

public class DatabaseUtils {

	/**
	 * Helper function for getting isbn owned state.
	 *
	 * @param isbn - isbn code to check.
	 * @return - True if isbn is considered owned. False otherwise.
	 */
	public static boolean isIsbnOwned(String isbn) {
		boolean isOwned = false;
		if(isbn != null && isbn.length() > 0) {
			UserDbHelper userdb = (UserDbHelper) DatabaseAccess.get().getUserDb();
			Cursor res = userdb.getData(isbn);
			isOwned = (res.getCount() > 0);
			res.close();
		}

		return isOwned;
	}

	public static void pushIsbnToHistory(String isbn) {
		// add code to cache
		CacheDbHelper cachedb = (CacheDbHelper) DatabaseAccess.get().getCacheDb();
		Cursor cacheRes = cachedb.getData(isbn);
		// remove previous entry
		if(cacheRes.getCount() > 0) {
			cachedb.removeData(isbn);
		}
		// add code back in
		cachedb.setData(isbn);
		cacheRes.close();
	}

	public static void pushPendingIsbn(String isbn) {
		// add code to isbn pending
		IsbnDbHelper isbndb = (IsbnDbHelper) DatabaseAccess.get().getIsbnDb();
		isbndb.setData(isbn);
	}

	public static boolean toggleOwnedState(String isbn) {
		UserDbHelper udb = (UserDbHelper) DatabaseAccess.get().getUserDb();
		Cursor res = udb.getData(isbn);
		boolean isOwned = (res.getCount() > 0);
		if(isOwned) {
			udb.removeData(isbn);
		} else {
			udb.setData(isbn);
		}
		return !isOwned;
	}

	public static void setOwnedState(String isbn, boolean state) {
		UserDbHelper udb = (UserDbHelper) DatabaseAccess.get().getUserDb();
		udb.removeData(isbn);

		if (state) {
			udb.setData(isbn);
		}
	}


}
