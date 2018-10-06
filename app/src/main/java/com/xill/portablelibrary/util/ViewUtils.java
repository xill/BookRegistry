package com.xill.portablelibrary.util;

import android.app.Activity;
import android.content.Intent;

import com.xill.portablelibrary.EntryViewer.EntryViewActivity;

/**
 * Created by Sami on 7/10/2018.
 */

public class ViewUtils {

	public static void launchEntryView(String isbn, Activity activity) {
		Intent i = new Intent(activity, EntryViewActivity.class);
		i.putExtra(EntryViewActivity.ISBN_KEY_PARAMETER,isbn);
		activity.startActivityForResult(i, 1);
	}

}
