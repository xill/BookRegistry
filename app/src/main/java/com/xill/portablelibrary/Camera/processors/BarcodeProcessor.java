package com.xill.portablelibrary.Camera.processors;

/**
 * Created by Sami on 7/24/2017.
 */

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseArray;
import android.view.TextureView;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.xill.portablelibrary.Database.DatabaseAccess;
import com.xill.portablelibrary.Database.DatabaseUtils;
import com.xill.portablelibrary.Database.UserDB.UserDbHelper;
import com.xill.portablelibrary.R;

/**
 * Handles bitmap to string barcode processing.
 */
public class BarcodeProcessor {
	/* Logging id to use. */
	private final String LOG_ID = BarcodeProcessor.class.getName();
	/* barcode api */
	private BarcodeDetector detector = null;
	/* Threading */
	private Thread processorThread = null;
	private boolean running = false;
	/* misc */
	private Activity mActivity = null;
	/* current active barcode */
	private String activeCode = "";

	private TextureView mTextureView = null;
	private TextView cameraPreviewText = null;
	private TextView cameraPreviewOwnedText = null;
	private BarcodeUpdateEvent listener = null;
	private SharedPreferences preferences = null;
	private Object LOCK = new Object();

	public BarcodeProcessor(Activity activity, BarcodeUpdateEvent listener) {
		mActivity = activity;
		this.listener = listener;

		mTextureView = (TextureView) mActivity.findViewById(R.id.cameraTextureView);
		cameraPreviewText = (TextView) mActivity.findViewById(R.id.cameraPreviewText);
		cameraPreviewText.getBackground().setAlpha(0);
		cameraPreviewOwnedText = (TextView) mActivity.findViewById(R.id.cameraPreviewOwnedText);
		preferences = PreferenceManager.getDefaultSharedPreferences(activity);

		// initialize barcode detector
		detector = new BarcodeDetector.Builder(mActivity).build();
		if(!detector.isOperational()) {
			Log.e("CAMERA_ACTIVITY", "detector failed to initialize.");
		}
	}

	public void start() {
		// already running
		if(running) return;

		running = true;
		processorThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// timestamp when previous active code was set.
				long lastUpdate = System.currentTimeMillis();
				// targeted delay between process frames.
				long baseDelay = 500;
				// minimum delay added between process frames.
				long minDelay = 100;

				while(running) {
					long delay = baseDelay;
					if(mTextureView != null) {
						Bitmap buffer = mTextureView.getBitmap();
						if(buffer != null) {
							long frameStart = System.currentTimeMillis();

							// process new image.
							final String code = processImage(buffer);
							long cur = System.currentTimeMillis();

							// check if code changed.
							// full code changes take effect immediately.
							// code clear take effect with a small delay.
							if(!code.equals(activeCode) && (cur-lastUpdate > 3*baseDelay || code.length() > 0)) {
								lastUpdate = cur;
								activeCode = code;
								mActivity.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										if(code.length() == 0) {
											cameraPreviewText.setText("");
											cameraPreviewOwnedText.setText("");
										} else {
											cameraPreviewText.setText("Code : " + code);

											if (preferences.getBoolean("autoOwned",false)) {
												DatabaseUtils.setOwnedState(code, true);
											}

											boolean isOwned = DatabaseUtils.isIsbnOwned(code);
											updateOwnedTf(isOwned);

											// push data to history
											DatabaseUtils.pushIsbnToHistory(code);
											// make sure data is in pending
											DatabaseUtils.pushPendingIsbn(code);
										}
										listener.doUpdate(code);
									}
								});
							}
							// code didn't change.
							if(code.equals(activeCode)) {
								lastUpdate = cur;
							}

							delay -= cur - frameStart;
							if(delay < minDelay) delay = minDelay;
						}
					}
					// wait for the next frame.
					synchronized (LOCK) {
						try {
							LOCK.wait(delay);
						} catch (Exception e) {}
					}
				}
			}
		});
		processorThread.start();
	}

	public void stop() {
		// not running
		if(processorThread == null || !running) return;

		// stop thread.
		running = false;
		synchronized (LOCK) {
			LOCK.notify();
		}
	}

	private String processImage(Bitmap bmp) {
		Frame frame = new Frame.Builder().setBitmap(bmp).build();
		SparseArray<Barcode> barcodes = detector.detect(frame);
		String out = "";
		Log.v(LOG_ID,"got " + barcodes.size());
		if(barcodes.size() > 0) {

			//String[] codes = new String[barcodes.size()];
			for(int i = 0; i < barcodes.size(); ++i) {
				int key = barcodes.keyAt(i);
				Barcode barcode = barcodes.get(key);
				String value = barcode.rawValue;
				// if code is ISBN-10 or ISBN-13
				if(value.length() == 10 || value.startsWith("978") || value.startsWith("979")) {
					out = value;
					break;
				}
			}
		}


		return out;
	}

	public String getActive() {
		return activeCode;
	}

	public void updateOwnedTf(boolean isOwned) {
		int color = mActivity.getResources().getColor(isOwned ? R.color.green : R.color.red,null);
		cameraPreviewOwnedText.setTextColor(color);
		cameraPreviewOwnedText.setText(isOwned ? "Owned" : "Not owned");
	}

	// helper class for when isbn changes
	public static abstract class BarcodeUpdateEvent {
		public abstract void doUpdate(String isbn);
	}
}
