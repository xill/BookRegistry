package com.xill.portablelibrary.Search;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.xill.portablelibrary.Database.DatabaseAccess;
import com.xill.portablelibrary.Database.IsbnRegistryDB.IsbnDbHelper;
import com.xill.portablelibrary.Database.IsbnRegistryDB.IsbnPendingDao;
import com.xill.portablelibrary.Database.IsbnRegistryDB.IsbnRegistryDao;
import com.xill.portablelibrary.Database.UserDB.ChangesDao;
import com.xill.portablelibrary.Database.UserDB.ChangesDbHelper;
import com.xill.portablelibrary.Database.UserDB.UserDbHelper;
import com.xill.portablelibrary.EntryViewer.EntryViewActivity;
import com.xill.portablelibrary.R;
import com.xill.portablelibrary.util.ViewUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Sami on 4/1/2018.
 */

public class SearchViewActivity extends AppCompatActivity {

	/* ui elements */

	private EditText searchBar = null;
	private ListView searchList = null;
	private TextView searchActiveTf = null;
	private ImageButton activeSearchBtn = null;

	/**
	 * {Map<String,String>} items to isbn code mapping
	 */
	private Map<String,String> nameToIsbn = null;
	/**
	 * {Map<String,boolean>} isbn to owned state mapping
	 */
	private Map<String,Boolean> isbnToOwned = null;
	/**
	 * {Map<String,String[]>} item name to list of all names
	 */
	private Map<String,String[]> nameToNames = null;
	/**
	 * {List<String>} all shown items, but not all possible.
	 */
	private List<String> listItems = null;
	/**
	 * {Array<String>} all possible items.
	 */
	private String[] items = null;

	private ArrayAdapter<String> listAdapter = null;
	/**
	 * {int} selection index, relative to listItems
	 */
	private int selectedIndex = -1;
	private boolean pendingRefresh = false;
	private SharedPreferences preferences = null;

	private final String KEY_SEARCH = "search_value_key";
	private final String SEARCH_TYPE_KEY = "searchType";
	private final String LANG_FILTER_KEY = "searchLangType";

	private enum SEARCH_TYPE_ENUM {
		SHOW_ALL,
		OWNED_ONLY,
		IGNORE_OWNED;

		public static SEARCH_TYPE_ENUM get(int i) {
			switch (i) {
				case 0: return SEARCH_TYPE_ENUM.SHOW_ALL;
				case 1: return SEARCH_TYPE_ENUM.OWNED_ONLY;
				case 2: return SEARCH_TYPE_ENUM.IGNORE_OWNED;
			}
			return SEARCH_TYPE_ENUM.SHOW_ALL;
		};
	};
	private SEARCH_TYPE_ENUM SEARCH_TYPE = SEARCH_TYPE_ENUM.SHOW_ALL;

	private enum LANG_FILTER_ENUM {
		SHOW_ALL,
		ENGLISH,
		JAPANESE,
		OTHER;

		public static LANG_FILTER_ENUM get(int i) {
			switch (i) {
				case 0: return LANG_FILTER_ENUM.SHOW_ALL;
				case 1: return LANG_FILTER_ENUM.ENGLISH;
				case 2: return LANG_FILTER_ENUM.JAPANESE;
				case 3: return LANG_FILTER_ENUM.OTHER;
			}
			return LANG_FILTER_ENUM.SHOW_ALL;
		};
	};
	private LANG_FILTER_ENUM LANG_FILTER = LANG_FILTER_ENUM.SHOW_ALL;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.searchview_layout);
		final Activity activity = this;

		searchActiveTf = (TextView) findViewById(R.id.searchview_ActiveTf);
		searchActiveTf.setText("");

		searchBar = (EditText) findViewById(R.id.searchview_searchBar);
		searchBar.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

			@Override
			public void afterTextChanged(Editable editable) {
				if(listItems != null) {
					updateListItems();
					saveSearchValue();
				}
			}
		});

		activeSearchBtn = (ImageButton) findViewById(R.id.searchviewSearchBtn);
		activeSearchBtn.setVisibility(View.GONE);
		activeSearchBtn.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view) {
				// launch selected entryview
				if(selectedIndex != -1) {
					String name = listItems.get(selectedIndex);
					String isbn = nameToIsbn.get(name);
					// check if isbn code only entry
					if(name.equals(isbn)) {
						pendingRefresh = true;
					}
					ViewUtils.launchEntryView(isbn, activity);
				}
			}
		});

		searchList = (ListView) findViewById(R.id.searchviewList);
		searchList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		searchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
				// same index was clicked again.
				// clear selection
				if(selectedIndex == pos) {
					clearSelection();
				} else {
					selectedIndex = pos;
					activeSearchBtn.setVisibility(selectedIndex != -1 ? View.VISIBLE : View.GONE);
					searchActiveTf.setText(listItems.get(selectedIndex));
				}
			}
		});

		preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

				// searchType
				// searchLangType
				// wifilessDownload
				// autoDownload

				if(SEARCH_TYPE_KEY.equals(s) || LANG_FILTER_KEY.equals(s)) {
					updatePreferences();
				}
			}
		});

		// update search states
		loadSearchValue();
		updateItemCache();
		initList();
		updateListItems();
		updatePreferences();
		clearSelection();
	}

	/**
	 * Update shown search list
	 */
	private void updatePreferences() {
		int ind = Integer.parseInt(preferences.getString(SEARCH_TYPE_KEY,"0"));
		SEARCH_TYPE_ENUM type = SEARCH_TYPE_ENUM.get(ind);

		ind = Integer.parseInt(preferences.getString(LANG_FILTER_KEY,"0"));
		LANG_FILTER_ENUM langType = LANG_FILTER_ENUM.get(ind);

		// search preference changed ?
		if(type != SEARCH_TYPE || langType != LANG_FILTER) {
			SEARCH_TYPE = type;
			LANG_FILTER = langType;
			updateListItems();
		}
	}

	protected void onActivityResult (int requestCode, int resultCode, Intent data){

		boolean refresh = false;
		// did EntryView ask for something ?
		if(data != null) {
			String resp = data.getData().toString();
			// search list should be updated
			if("refresh=true".equals(resp)) refresh = true;
		}

		// returned to search view. update state in case it changed.
		if(pendingRefresh || refresh) {
			String name = listItems.get(selectedIndex);
			String isbn = nameToIsbn.get(name);

			// update list and clear previous now false selection
			updateItemCache();
			updateListItems();
			clearSelection();

			// find new selection index
			for(Map.Entry<String, String> e : nameToIsbn.entrySet()) {
				if(e.getValue().equals(isbn)) {
					name = e.getKey();
					break;
				}
			}
			// update search list selection
			boolean entryPresent = false;
			for(int i = 0; i < listItems.size(); ++i) {
				if(listItems.get(i).equals(name)) {
					entryPresent = true;
					searchList.setItemChecked(i, true);
					break;
				}
			}
			// update text when entry is still present
			if(entryPresent) searchActiveTf.setText(name);
		}

		pendingRefresh = false;
	}

	private void initList() {
		listItems = new ArrayList<String>(Arrays.asList(items));
		listAdapter = new ArrayAdapter<String>(this, R.layout.list_item2,R.id.textItem, listItems);
		searchList.setAdapter(listAdapter);
	}

	/**
	 * Clear search selection
	 */
	private void clearSelection() {
		if(listItems.size() > 0) {
			searchActiveTf.setText("");
		}
		activeSearchBtn.setVisibility(View.GONE);
		selectedIndex = -1;
		searchList.clearChoices();
		listAdapter.notifyDataSetChanged();
	}

	/**
	 * Test if search string matches
	 */
	private boolean searchMatches(String[] names, String searchText) {
		for(String s : names) {
			String ss = s.toLowerCase();
			if(ss.contains(searchText)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Update shown list of items.
	 */
	private void updateListItems() {

		listItems.clear();
		// get search text
		String searchText = searchBar.getText()
				.toString().trim();
		// go through all possible items
		for(String value : items) {
			// is text ok to be shown?
			boolean isOk = (searchText.length() == 0 || searchMatches(nameToNames.get(value), searchText.toLowerCase()));

			String isbn = nameToIsbn.get(value);
			if(isOk) {
				char c = isbn.charAt(3);
				switch (LANG_FILTER) {
					case ENGLISH: { isOk = (c == '0' || c == '1'); break; }
					case JAPANESE: { isOk = c == '4'; break; }
					case OTHER: { isOk = c != '0' && c != '1' && c != '4'; break; }
				}
			}

			if(isOk) {
				// check for search filtering
				boolean isOwned = isbnToOwned.get(isbn);
				switch (SEARCH_TYPE) {
					case OWNED_ONLY: { isOk = isOwned; break; }
					case IGNORE_OWNED: { isOk = !isOwned; break; }
				}
				// still ok ?
				if(isOk) {
					listItems.add(value);
				}
			}
		}
		listAdapter.notifyDataSetChanged();
	}

	/**
	 * Update knowledge of all possible entries
	 */
	private void updateItemCache() {
		// get entries from database
		IsbnDbHelper db = (IsbnDbHelper) DatabaseAccess.get().getIsbnDb();
		ChangesDbHelper cdb = (ChangesDbHelper) DatabaseAccess.get().getChangesDb();
		Cursor registryCursor = db.getAll(db.REGISTRY_KEY);
		Cursor pendingCursor = db.getAll(db.PENDING_KEY);
		//Cursor changedCursor = ((ChangesDbHelper) DatabaseAccess.get().getChangesDb()).getAll();

		nameToIsbn = new HashMap<String, String>();
		nameToNames = new HashMap<String, String[]>();
		isbnToOwned = new HashMap<String, Boolean>();
		List<String> nameList = new ArrayList<String>();

		// get data on owned entries
		UserDbHelper udb = (UserDbHelper) DatabaseAccess.get().getUserDb();

		// go through isbn only entries
		for(int i = 0; i < pendingCursor.getCount(); ++i) {
			pendingCursor.moveToPosition(i);
			String isbnCode = pendingCursor.getString(pendingCursor.getColumnIndexOrThrow(IsbnPendingDao.COL_1));
			String name = null;
			List<String> names = new ArrayList<String>();

			Cursor changesCursor = cdb.getData(isbnCode);
			// use user changed name if available.
			if (changesCursor.getCount() > 0) {
				changesCursor.moveToPosition(0);
				String[] changedNames = changesCursor.getString(changesCursor.getColumnIndexOrThrow(ChangesDao.COL_2)).split("\n");//[0].trim();
				name = changedNames[0].trim();

				for (String s : changedNames) {
					names.add(s.trim());
				}
			}

			// no valid name in changes data. use isbn as a backup
			if (name == null || name.length() == 0) {
				name = isbnCode;
				names.add(name);
			}

			nameList.add(name);
			nameToIsbn.put(name,isbnCode);
			nameToNames.put(name, names.toArray(new String[0]));

			// check if isbn code is marked owned
			Cursor res = udb.getData(isbnCode);
			isbnToOwned.put(isbnCode,res.getCount() > 0);
		}

		// go through entries with additional data
		for(int i = 0; i< registryCursor.getCount(); ++i) {
			registryCursor.moveToPosition(i);
			String isbnCode = registryCursor.getString(registryCursor.getColumnIndexOrThrow(IsbnRegistryDao.COL_1));
			String[] namesData = registryCursor.getString(registryCursor.getColumnIndexOrThrow(IsbnRegistryDao.COL_3)).split(IsbnRegistryDao.ARRAY_DELIM);
			List<String> names = new ArrayList<String>();
			for (String s : namesData) {
				names.add(s);
			}

			Cursor changesCursor = cdb.getData(isbnCode);
			String name = namesData[0].trim();
			// find first non kanji name
			for (String s : names) {
				if(s.length() > 0) {
					char c = s.charAt(0);
					if((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
						name = s.trim();
						break;
					}
				}
			}
			// name field is empty, use isbn instead
			if (name.length() == 0) {
				name = isbnCode;
			}

			// use user changed name if available.
			if (changesCursor.getCount() > 0) {
				changesCursor.moveToPosition(0);
				String[] changedNames = changesCursor.getString(changesCursor.getColumnIndexOrThrow(ChangesDao.COL_2)).split("\n");//[0].trim();
				String nameCandidate = changedNames[0].trim();

				boolean hasMatch = false;
				// check standard names for a candidate match.
				// if matched, candidate is ignored.
				for (String s : names) {
					if (nameCandidate.equals(s)) {
						hasMatch = true;
						break;
					}
				}
				// add changed names to names list
				for(String s : changedNames) {
					String ts = s.trim();
					if (!names.contains(ts)) names.add(ts);
				}
				// no duplicate names? use changed.
				if (!hasMatch) name = nameCandidate;
			}

			nameList.add(name);
			nameToIsbn.put(name, isbnCode);
			nameToNames.put(name, names.toArray(new String[0]));

			// check if isbn code is marked owned
			Cursor res = udb.getData(isbnCode);
			isbnToOwned.put(isbnCode,res.getCount() > 0);
		}

		Collections.sort(nameList);
		items = nameList.toArray(new String[0]);
	}

	/**
	 * Save search to use it between activities
	 */
	private void saveSearchValue() {
		String searchText = searchBar.getText()
				.toString().trim();

		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(KEY_SEARCH,searchText);
		editor.commit();
	}

	/**
	 * Load saved search value
	 */
	private void loadSearchValue() {
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		String value = sharedPref.getString(KEY_SEARCH,"");
		searchBar.setText(value);
	}

}
