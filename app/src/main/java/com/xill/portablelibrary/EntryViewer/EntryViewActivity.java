package com.xill.portablelibrary.EntryViewer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.xill.portablelibrary.Crawler.EntryObject;
import com.xill.portablelibrary.Crawler.WorldCatCrawler;
import com.xill.portablelibrary.Database.DatabaseAccess;
import com.xill.portablelibrary.Database.DatabaseUtils;
import com.xill.portablelibrary.Database.IsbnRegistryDB.IsbnDbHelper;
import com.xill.portablelibrary.Database.IsbnRegistryDB.IsbnRegistryDao;
import com.xill.portablelibrary.Database.UserDB.ChangesDao;
import com.xill.portablelibrary.Database.UserDB.ChangesDbHelper;
import com.xill.portablelibrary.Database.UserDB.UserDbHelper;
import com.xill.portablelibrary.R;
import com.xill.portablelibrary.util.ConnUtils;
import com.xill.portablelibrary.util.DownloadManager;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Sami on 3/24/2018.
 */

public class EntryViewActivity extends AppCompatActivity {
	public static String ISBN_KEY_PARAMETER = "isbn_key";

	private final String CLEAR_TITLE = "Clear all data?";
	private final String CLEAR_TXT = "";

	private TextView headerTF = null;
	private TextView timeTF = null;
	private TextView namesTF = null;
	private TextView authorTF = null;
	private TextView languageTF = null;
	private TextView tagsTF = null;
	private TextView publishersTF = null;
	private TextView notesTF = null;
	private TextView ownedTF = null;

	private Button reloadBtn = null;
	private Button deleteBtn = null;
	private Button ownedBtn = null;
	private Button editBtn = null;
	private Button cancelEditBtn = null;
	private Button confirmEditBtn = null;

	private boolean hasDlWithoutWifi = false;
	private String WIFI_DL_KEY = "wifilessDownload";

	private Context context;
	private String isbn = null;
	private SharedPreferences preferences = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry_view_layout);
		context = this;

		// get tf's
		headerTF = (TextView) findViewById(R.id.entryViewHeader);
		timeTF = (TextView) findViewById(R.id.entryViewTimeStamp);
		namesTF = (TextView) findViewById(R.id.entryViewNames);
		authorTF = (TextView) findViewById(R.id.entryViewAuthors);
		languageTF = (TextView) findViewById(R.id.entryViewLanguages);
		tagsTF = (TextView) findViewById(R.id.entryViewTags);
		publishersTF = (TextView) findViewById(R.id.entryViewPublishers);
		notesTF = (TextView) findViewById(R.id.entryViewNotes);
		ownedTF = (TextView) findViewById(R.id.entryViewOwnedState);

		reloadBtn = (Button) findViewById(R.id.entryViewReloadBtn);
		deleteBtn = (Button) findViewById(R.id.entryViewDeleteBtn);
		ownedBtn = (Button) findViewById(R.id.entryViewOwnedToggleBtn);
		editBtn = (Button) findViewById(R.id.entryViewEditBtn);
		cancelEditBtn = (Button) findViewById(R.id.entryViewCancelEditBtn);
		confirmEditBtn = (Button) findViewById(R.id.entryViewConfirmEditBtn);

		// remove tf temp texts.
		headerTF.setText("");
		timeTF.setText("");
		namesTF.setText("");
		authorTF.setText("");
		languageTF.setText("");
		tagsTF.setText("");
		publishersTF.setText("");
		notesTF.setText("");

		// editviews are currently not editable.
		setEditEnabled(false);

		// make tf's multiline
		namesTF.setSingleLine(false);
		authorTF.setSingleLine(false);
		languageTF.setSingleLine(false);
		tagsTF.setSingleLine(false);
		publishersTF.setSingleLine(false);
		notesTF.setSingleLine(false);

		reloadBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(ConnUtils.hasWifi(context)) {
					reloadEntry();
				}
			}
		});
		deleteBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// when dialog yes is selected.
				Runnable yesRunnable = new Runnable() {
					@Override
					public void run() {
						clearEntry();
					}
				};
				// when dialog no is selected.
				Runnable noRunnable = new Runnable() {
					@Override
					public void run() {
						// do nothing
					}
				};

				getYesNoDialog(CLEAR_TITLE,CLEAR_TXT,yesRunnable,noRunnable);
			}
		});
		editBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setEditEnabled(true);
			}
		});
		cancelEditBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setEditEnabled(false);
				resetTfValues();
			}
		});
		confirmEditBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setEditEnabled(false);
				saveTfValues();
			}
		});

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
				if(WIFI_DL_KEY.equals(s)) {
					updateProperties();
				}
			}
		});

		// make sure configured properties are up to date.
		updateProperties();

		isbn = getIntent().getStringExtra(ISBN_KEY_PARAMETER);
		headerTF.setText(isbn);
		final IsbnDbHelper db = (IsbnDbHelper) DatabaseAccess.get().getIsbnDb();
		Cursor isbnCursor = db.getData(db.REGISTRY_KEY + isbn);
		if(isbnCursor.getCount() > 0) {
			Log.v(this.getClass().getName(), "Entry " + isbn + " found");
			updateTFs(isbnCursor);
		} else {
			Log.v(this.getClass().getName(), "Entry " + isbn + " not found");

			boolean allowedConn = ConnUtils.hasInternet(context);
			if(allowedConn && !hasDlWithoutWifi) {
				allowedConn = ConnUtils.hasWifi(context);
			}

			if(allowedConn) {
				Log.v(getClass().getName(),"Starting download for isbn " + isbn);
				// no previous entry.
				// start networking on secondary thread.
				new Thread(new Runnable() {
					@Override
					public void run() {
						String result = DownloadManager.getRawIsbnDataAsString(isbn);

						// if something was actually received.
						if(result != null) {
							// parse received data.
							WorldCatCrawler crawler = new WorldCatCrawler();
							EntryObject entry = crawler.getResultEntries(result);
							// update database
							IsbnDbHelper helper = (IsbnDbHelper) DatabaseAccess.get().getIsbnDb();
							helper.removeData(isbn);
							helper.setData(isbn, entry);
							// get updated data
							final Cursor isbnCursor2 = db.getData(db.REGISTRY_KEY + isbn);
							// update texts on ui thread.
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									updateTFs(isbnCursor2);
								}
							});

						}
						// failed. just exit activity.
						else {
							finish();
						}
					}
				}).start();
			}
			// update tf's anyway, there might be notes to show.
			else {
				updateTFs(null);
			}
		}

		// setup ownership related items and entries
		updateOwnedState();

		// handle owned toggling
		ownedBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				toggleOwnedState();
			}
		});
	}

	private void setEditEnabled(boolean enabled) {
		namesTF.setEnabled(enabled);
		authorTF.setEnabled(enabled);
		languageTF.setEnabled(enabled);
		tagsTF.setEnabled(enabled);
		publishersTF.setEnabled(enabled);
		notesTF.setEnabled(enabled);

		editBtn.setVisibility(enabled ? View.GONE : View.VISIBLE);
		cancelEditBtn.setVisibility(enabled ? View.VISIBLE : View.GONE);
		confirmEditBtn.setVisibility(enabled ? View.VISIBLE : View.GONE);

	}

	private void toggleOwnedState() {
		boolean isOwned = DatabaseUtils.toggleOwnedState(isbn);
		markDataAsUpdated();
		updateOwnedTf(isOwned);
	}

	private void updateOwnedTf(boolean isOwned) {
		ownedTF.setText("" + isOwned);
		int color = getResources().getColor(isOwned ? R.color.green : R.color.red,null);
		ownedTF.setTextColor(color);
	}

	private void updateOwnedState(UserDbHelper udb) {
		Cursor res = udb.getData(isbn);
		boolean isOwned = (res.getCount() > 0);
		updateOwnedTf(isOwned);
	}

	private void updateOwnedState() {
		UserDbHelper udb = (UserDbHelper) DatabaseAccess.get().getUserDb();
		updateOwnedState(udb);
	}

	private void resetTfValues() {
		// go back to previously saved values.
		final IsbnDbHelper db = (IsbnDbHelper) DatabaseAccess.get().getIsbnDb();
		Cursor isbnCursor = db.getData(db.REGISTRY_KEY + isbn);
		updateTFs(isbnCursor);
	}

	private void saveTfValues() {
		// save current tf values to changesdb
		EntryObject entry = new EntryObject();
		entry.names = new String[]{namesTF.getText().toString()};
		entry.authors = new String[]{authorTF.getText().toString()};
		entry.languages = new String[]{languageTF.getText().toString()};
		entry.publishers = new String[]{publishersTF.getText().toString()};
		entry.notes = new String[]{notesTF.getText().toString()};

		ChangesDbHelper changesDb = (ChangesDbHelper) DatabaseAccess.get().getChangesDb();
		changesDb.removeData(isbn);
		changesDb.setData(isbn, entry);

		markDataAsUpdated();
	}

	private void updateTFs(Cursor isbnCursor) {
		ChangesDbHelper changesDb = (ChangesDbHelper) DatabaseAccess.get().getChangesDb();
		EntryObject changesEntry = ChangesDao.toEntryObject(changesDb.getData(isbn));

		// update tf texts
		EntryObject entry = null;
		if (isbnCursor != null) {
			entry = IsbnRegistryDao.toEntryObject(isbnCursor);
		} else {
			entry = new EntryObject();
		}

		timeTF.setText(entry.timestamp != 0 ? new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(entry.timestamp)) : "");
		namesTF.setText(hasData(changesEntry.names) ? changesEntry.names[0] : join(entry.names));
		authorTF.setText(hasData(changesEntry.authors) ? changesEntry.authors[0] : join(entry.authors));
		languageTF.setText(hasData(changesEntry.languages) ? changesEntry.languages[0] : join(entry.languages));
		tagsTF.setText("TODO tags"); // TODO
		notesTF.setText(hasData(changesEntry.notes) ? changesEntry.notes[0] : join(entry.notes));

		publishersTF.setText(hasData(changesEntry.publishers) ? changesEntry.publishers[0] : join(entry.publishers));
	}

	private boolean hasData(String[] arr) {
		return arr != null && arr.length > 0 && arr[0].trim().length() > 0;
	}

	private String join(String[] arr) {
		StringBuilder builder = new StringBuilder();
		if (arr != null) {
			for(int i = 0; i < arr.length; ++i) {
				if(i > 0) builder.append("\n");
				builder.append(arr[i]);
			}
		}
		return builder.toString();
	}

	private void getYesNoDialog(String title, String promptTxt, final Runnable yesCB, final Runnable noCB) {
		new AlertDialog.Builder(this)
				.setTitle(title)
				.setMessage(promptTxt)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {
						yesCB.run();
					}})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						noCB.run();
					}
				}).show();
	}

	private void clearEntry() {

		final IsbnDbHelper db = (IsbnDbHelper) DatabaseAccess.get().getIsbnDb();
		db.removeRegistryEntry(isbn); // first removes any resolved data.
		db.removeData(isbn); // second removes isbn reference.

		final UserDbHelper ownedDb = (UserDbHelper) DatabaseAccess.get().getUserDb();
		ownedDb.removeData(isbn);

		Intent data = new Intent();
		data.setData(Uri.parse("refresh=true"));
		setResult(RESULT_OK, data);

		// no point in sticking around.
		finish();
	}

	private void reloadEntry() {
		final IsbnDbHelper db = (IsbnDbHelper) DatabaseAccess.get().getIsbnDb();
		// remove resolved data.
		db.removeRegistryEntry(isbn);
		// make sure isbn is in pending
		db.setData(isbn);

		// reload activity
		finish();
		startActivity(getIntent());
	}

	private void updateProperties() {;
		hasDlWithoutWifi = preferences.getBoolean(WIFI_DL_KEY,false);
		Log.v(getClass().getName(),"hasDlWithoutWifi " + hasDlWithoutWifi);
	}

	/**
	 * Data has changed. Make sure search view knows to update.
	 */
	private void markDataAsUpdated() {
		Intent data = new Intent();
		data.setData(Uri.parse("refresh=true"));
		setResult(RESULT_OK, data);
	}

}
