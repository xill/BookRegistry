package com.xill.portablelibrary.util;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Created by Sami on 6/23/2018.
 */

public class ConnUtils {

	public static boolean hasInternet(Context context) {
		ConnectivityManager cm =
				(ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return (activeNetwork != null &&
				activeNetwork.isConnectedOrConnecting());
	}

	public static boolean hasWifi(Context context) {
		WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON

			WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

			if( wifiInfo.getNetworkId() == -1 ){
				return false; // Not connected to an access point
			}
			return true; // Connected to an access point
		}
		else {
			return false; // Wi-Fi adapter is OFF
		}
	}

}
