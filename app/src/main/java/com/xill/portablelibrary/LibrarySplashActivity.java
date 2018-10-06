package com.xill.portablelibrary;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.xill.portablelibrary.Camera.CameraActivity;
import com.xill.portablelibrary.Database.DatabaseAccess;
import com.xill.portablelibrary.Database.DatabaseUtils;
import com.xill.portablelibrary.Database.IsbnRegistryDB.IsbnDbHelper;
import com.xill.portablelibrary.Database.ScannedDB.CacheDbHelper;
import com.xill.portablelibrary.Database.UserDB.UserDbHelper;
import com.xill.portablelibrary.History.HistoryActivity;
import com.xill.portablelibrary.Search.SearchViewActivity;
import com.xill.portablelibrary.preferences.PreferencesActivity;

import org.w3c.dom.Text;

public class LibrarySplashActivity extends AppCompatActivity {

	public static final String LOG_ID = "PORTABLE_LIBRARY";
	private static final int REQUEST_IMAGE_CAPTURE = 1;

	// shows picked isbn code
	private EditText codeTf = null;
	// shows owned status
	private TextView statusTf = null;
	// shows general statistics
	private TextView statsTf = null;

	private DatabaseAccess dbAccess = null;
	private Button clearBtn = null;

	private String statsBaseStr = "Owned : %n1%\nScanned : %n2%\nNo data : %n3%";
	private Activity activity = null;

	private static final int STORAGE_PERMISSION = 35312;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_library_splash);
		activity = this;

		codeTf = (EditText) findViewById(R.id.statusTxt);
		statusTf = (TextView) findViewById(R.id.infoText);
		Button scanBtn = (Button)findViewById(R.id.scanBtn);
		scanBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startCamera();
			}
		});

		Button searchBtn = (Button) findViewById(R.id.searchBtn);
		searchBtn.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view) {
				startSearch();
			}
		});

		Button historyBtn = (Button) findViewById(R.id.historyBtn);
		historyBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startHistory();
			}
		});

		Button settingsBtn = (Button) findViewById(R.id.settingsBtn);
		settingsBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startSettings();
			}
		});

		clearBtn = (Button) findViewById(R.id.clearBtn);
		clearBtn.setVisibility(View.GONE);
		clearBtn.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view) {
				clearCodeField();
			}
		});

		statsTf = (TextView) findViewById(R.id.statsTF);

		// disabled status text editing
		codeTf.setFocusable(false);
		codeTf.setFocusableInTouchMode(false);
		codeTf.setClickable(false);

		// clear text fields
		codeTf.setText("");
		statusTf.setText("");

		// make sure permissions are ok
		if (getPermissions()) {
			startDatabases();
		}
	}

	private void startCamera() {
		Intent i = new Intent(this, CameraActivity.class);
		startActivityForResult(i, 1);
	}

	private void startSearch() {
		Intent i = new Intent(this, SearchViewActivity.class);
		startActivityForResult(i, 1);
	}

	private void startHistory() {
		Intent i = new Intent(this, HistoryActivity.class);
		startActivityForResult(i, 1);
	}

	private void startSettings() {
		Intent i = new Intent(this, PreferencesActivity.class);
		startActivityForResult(i, 1);
	}

	private void clearCodeField() {
		codeTf.setText("");
		clearBtn.setVisibility(View.GONE);
		statusTf.setText("");
	}

	private void startDatabases() {
		DatabaseAccess.initialize(this);
		dbAccess = DatabaseAccess.get();

		updateStats();
	}

	private boolean getPermissions() {

		if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION);
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		// response for camera permission request
		if(requestCode == STORAGE_PERMISSION) {
			if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				if(dbAccess == null) startDatabases();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
			String code = data.getData().toString();
			codeTf.setText(code);

			boolean isOwned = DatabaseUtils.isIsbnOwned(code);
			statusTf.setText(isOwned ? "Owned" : "Not owned");

			// adjust add button visibility
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					clearBtn.setVisibility(View.VISIBLE);
				}
			});
		}
	}

	private void updateStats() {

		UserDbHelper userDb = (UserDbHelper) dbAccess.getUserDb();
		int ownedCount = userDb.getAll().getCount();

		IsbnDbHelper isbnDb = (IsbnDbHelper) dbAccess.getIsbnDb();
		int noDataCount = isbnDb.getAll("").getCount();
		int scannedCount = isbnDb.getAll().getCount() + noDataCount;

		// "Owned : %n1%\nScanned : %n2%\nNo data : %n3%"
		statsTf.setText(statsBaseStr.replace("%n1%",ownedCount + "")
				.replace("%n2%",scannedCount + "")
				.replace("%n3%",noDataCount + ""));
	}

	@Override
	protected void onResume() {
		if(dbAccess != null) updateStats();
		super.onResume();
	}

}
