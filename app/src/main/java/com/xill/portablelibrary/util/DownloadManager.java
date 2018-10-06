package com.xill.portablelibrary.util;

import android.util.Log;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Created by Sami on 3/17/2018.
 */

public class DownloadManager {

	public static String getUrlAsString(String requestURL) throws Exception {
		try (Scanner scanner = new Scanner(new URL(requestURL).openStream(),"utf-8"))
		{
			scanner.useDelimiter("\\A");
			return scanner.hasNext() ? scanner.next() : "";
		}
	}

	public static String getRawIsbnDataAsString(String isbn) {
		String wcatPattern = "http://www.worldcat.org/search?q=<isbn>&qt=results_page";
		String result = null;
		try {
			// fetch result page as string.
			result = DownloadManager.getUrlAsString(wcatPattern.replace("<isbn>",isbn));
		} catch (Exception e) {
			Log.e(DownloadManager.class.getName(), "Entry fetch failed.", e);
		}
		return result;
	}

}
