package com.xill.portablelibrary.History;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.xill.portablelibrary.Database.DatabaseAccess;
import com.xill.portablelibrary.Database.ScannedDB.CacheDbHelper;
import com.xill.portablelibrary.Database.ScannedDB.ScannedDao;
import com.xill.portablelibrary.EntryViewer.EntryViewActivity;
import com.xill.portablelibrary.R;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Sami on 7/29/2017.
 */

public class HistoryActivity extends AppCompatActivity {

	private ListView historyList = null;
	private Button clearSingleBtn = null;
	private Button clearAllBtn = null;
	private ImageButton historySearchBtn = null;
	private TextView historyActiveTf = null;
	private TextView historyActiveTimeTf = null;

	private String[] items = null;
	private String[] times = null;
	private ArrayList<String> listItems = null;
	private ArrayAdapter<String> listAdapter = null;
	private int selectedIndex = -1;

	private final String CLEAR_ALL_TITLE = "Clear all cached entries?";
	private final String CLEAR_ALL_TXT = "";
	private final String CLEAR_SINGLE_TITLE = "Clear selected entry?";
	private final String CLEAR_SINGLE_TXT = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.history_layout);

		historyActiveTimeTf = (TextView) findViewById(R.id.historyActiveTimeTf);
		historyActiveTimeTf.setText("");

		historyActiveTf = (TextView) findViewById(R.id.historyActiveTf);
		historyActiveTf.setText("");

		final Activity activity = this;

		historySearchBtn = (ImageButton) findViewById(R.id.historySearchBtn);
		historySearchBtn.setVisibility(View.GONE);
		historySearchBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(selectedIndex != -1) {
					String isbn = items[selectedIndex];
					Intent i = new Intent(activity, EntryViewActivity.class);
					i.putExtra(EntryViewActivity.ISBN_KEY_PARAMETER,isbn);
					startActivityForResult(i, 1);
				}
			}
		});

		// button used to clear single selected item
		clearSingleBtn = (Button) findViewById(R.id.clearHistorySingleBtn);
		clearSingleBtn.setVisibility(View.GONE);
		clearSingleBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(selectedIndex == -1) return;
				final int indToRemove = selectedIndex;

				// when dialog yes is selected.
				Runnable yesRunnable = new Runnable() {
					@Override
					public void run() {
						removeIndexOf(indToRemove);

						if(selectedIndex == indToRemove) {
							clearSelection();
						}
					}
				};
				// when dialog no is selected.
				Runnable noRunnable = new Runnable() {
					@Override
					public void run() {
						// do nothing
					}
				};

				getYesNoDialog(CLEAR_SINGLE_TITLE,CLEAR_SINGLE_TXT,yesRunnable,noRunnable);
			}
		});

		// button used to clear all history items
		clearAllBtn = (Button) findViewById(R.id.clearHistoryBtn);
		clearAllBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// when dialog yes is selected.
				Runnable yesRunnable = new Runnable() {
					@Override
					public void run() {
						clearSelection();
						clearAllItems();
					}
				};
				// when dialog no is selected.
				Runnable noRunnable = new Runnable() {
					@Override
					public void run() {
						// do nothing
					}
				};

				getYesNoDialog(CLEAR_ALL_TITLE,CLEAR_ALL_TXT,yesRunnable,noRunnable);
			}
		});

		historyList = (ListView) findViewById(R.id.historyList);
		historyList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		historyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
				// same index was clicked again.
				// clear selection
				if(selectedIndex == pos) {
					clearSelection();
				} else {
					selectedIndex = pos;
					clearSingleBtn.setVisibility(selectedIndex != -1 ? View.VISIBLE : View.GONE);
					historySearchBtn.setVisibility(selectedIndex != -1 ? View.VISIBLE : View.GONE);
					historyActiveTf.setText(listItems.get(selectedIndex));
					historyActiveTimeTf.setText(times[selectedIndex]);
				}
			}
		});

		updateItemsCache();
		initList();
	}

	private void initList() {
		listItems = new ArrayList<String>(Arrays.asList(items));
		listAdapter = new ArrayAdapter<String>(this, R.layout.list_item2,R.id.textItem, listItems);
		historyList.setAdapter(listAdapter);
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

	/**
	 * Clear a single cached entry from listview and database
	 *
	 * @param pos - index to remove
	 */
	private void removeIndexOf(int pos) {
		String value = listItems.get(pos);
		if(value != null && value.length() >  0) {
			value = value.trim();

			CacheDbHelper db = (CacheDbHelper) DatabaseAccess.get().getCacheDb();
			db.removeData(value);

			updateItemsCache();
			updateListItems();
		}
	}

	/**
	 * Clear all cached database entries
	 */
	private void clearAllItems() {
		CacheDbHelper db = (CacheDbHelper) DatabaseAccess.get().getCacheDb();
		for(String data : items) {
			db.removeData(data);
		}

		updateItemsCache();
		updateListItems();
	}

	private void updateItemsCache() {
		Cursor resultCursor = DatabaseAccess.get().getCacheDb().getAll();
		items = new String[resultCursor.getCount()];
		times = new String[resultCursor.getCount()];

		SimpleDateFormat dateFormat = new SimpleDateFormat("MM.d.yyyy HH:mm");
		Map<Long,String> valueMap = new HashMap<Long,String>();
		List<Long> timeList = new ArrayList<Long>();
		// get values
		for(int i = 0; i < resultCursor.getCount(); ++i) {
			resultCursor.moveToPosition(i);
			String isbn = resultCursor.getString(resultCursor.getColumnIndexOrThrow(ScannedDao.COL_1));
			long time = resultCursor.getLong(resultCursor.getColumnIndexOrThrow(ScannedDao.COL_2));
			valueMap.put(time,isbn);
			timeList.add(time);
		}
		// sort collections so latest is first.
		Collections.sort(timeList);
		Collections.reverse(timeList);

		for(int i = 0; i < timeList.size(); ++i) {
			long time = timeList.get(i);
			items[i] = valueMap.get(time);
			times[i] = dateFormat.format(new Date(time));
		}

		clearAllBtn.setVisibility(items.length > 0 ? View.VISIBLE : View.GONE);
		if(items.length == 0) {
			historyActiveTf.setText("No entries");
			historyActiveTimeTf.setText("");
		}
	}

	private void updateListItems() {
		listItems.clear();
		for(String value : items) {
			listItems.add(value);
		}
		listAdapter.notifyDataSetChanged();
	}

	private void clearSelection() {
		if(listItems.size() > 0) {
			historyActiveTf.setText("");
			historyActiveTimeTf.setText("");
		}
		clearSingleBtn.setVisibility(View.GONE);
		historySearchBtn.setVisibility(View.GONE);
		selectedIndex = -1;
		historyList.clearChoices();
		listAdapter.notifyDataSetChanged();
	}
}
