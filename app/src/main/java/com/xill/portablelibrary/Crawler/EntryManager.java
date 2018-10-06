package com.xill.portablelibrary.Crawler;

import android.database.Cursor;
import android.util.Log;

import com.xill.portablelibrary.Database.DatabaseAccess;
import com.xill.portablelibrary.Database.IsbnRegistryDB.IsbnDbHelper;
import com.xill.portablelibrary.Database.IsbnRegistryDB.IsbnRegistryDao;
import com.xill.portablelibrary.util.DownloadManager;

/**
 * Created by Sami on 3/17/2018.
 */

public class EntryManager {

	private static EntryManager _instance = null;
	private IsbnDbHelper dbHelper = null;
	private WorldCatCrawler crawler = null;

	private String dataUrlPattern = "http://www.worldcat.org/search?q=<isbn>&qt=results_page";
	private String replacePattern = "<isbn>";

	private EntryManager() {
		dbHelper = (IsbnDbHelper) DatabaseAccess.get().getIsbnDb();
		crawler = new WorldCatCrawler();
	}

	public static EntryManager get() {
		if(_instance == null) _instance = new EntryManager();
		return _instance;
	}

	public void updateEntry(String entryId) {
		// 1. check if entry in pending
		Cursor pendingCursor = dbHelper.getData(entryId);
		boolean inPending = pendingCursor.getCount() > 0;

		String data = null;
		try {
			// fetch entry data to parse.
			data = DownloadManager.getUrlAsString(dataUrlPattern.replace(replacePattern,entryId));
		} catch (Exception e) {}
		// check if data is valid.
		if(data != null && data.length() > 0) {
			// let data crawler parse the data string.
			EntryObject entry = crawler.getResultEntries(data);
			// save parsed data.
			dbHelper.setData(entryId, entry);
			// remove pending entry
			if(inPending) dbHelper.removeData(entryId);
		}
	}

	public EntryObject getEntry(String entryId) {
		Cursor registryCursor = dbHelper.getData(dbHelper.REGISTRY_KEY + entryId);
		EntryObject entry = new EntryObject();

		entry.names = registryCursor.getString(registryCursor.getColumnIndexOrThrow(IsbnRegistryDao.COL_3)).split(",");
		entry.authors = registryCursor.getString(registryCursor.getColumnIndexOrThrow(IsbnRegistryDao.COL_4)).split(",");
		entry.languages = registryCursor.getString(registryCursor.getColumnIndexOrThrow(IsbnRegistryDao.COL_5)).split(",");
		entry.publishers = registryCursor.getString(registryCursor.getColumnIndexOrThrow(IsbnRegistryDao.COL_6)).split(",");
		entry.thumbnail = registryCursor.getString(registryCursor.getColumnIndexOrThrow(IsbnRegistryDao.COL_7));

		return entry;
	}

	public String[] getEntryNames(String entryId) {
		EntryObject entry = getEntry(entryId);
		return entry.names;
	}

}
