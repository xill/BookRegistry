package com.xill.portablelibrary.Camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.xill.portablelibrary.Camera.processors.BarcodeProcessor;
import com.xill.portablelibrary.Camera.util.CompareArea;
import com.xill.portablelibrary.Crawler.EntryObject;
import com.xill.portablelibrary.Crawler.WorldCatCrawler;
import com.xill.portablelibrary.Database.DatabaseAccess;
import com.xill.portablelibrary.Database.DatabaseUtils;
import com.xill.portablelibrary.Database.IsbnRegistryDB.IsbnDbHelper;
import com.xill.portablelibrary.R;
import com.xill.portablelibrary.util.ConnUtils;
import com.xill.portablelibrary.util.DownloadManager;
import com.xill.portablelibrary.util.ViewUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Sami on 7/22/2017.
 */

public class CameraActivity extends Activity {
	/* layout items */
	private AutoFitTextureView mTextureView = null;
	private Button infoBtn = null;
	private Button ownedBtn = null;

	/* camera device misc */
	private CameraDevice mCameraDevice = null;
	private CaptureRequest.Builder mPreviewRequestBuilder = null;
	private CaptureRequest mPreviewRequest = null;
	private CameraCaptureSession mCaptureSession = null;
	private String mCameraId = "";

	/* handle barcode processing */
	private BarcodeProcessor processor = null;

	// threads handling the preview.
	private HandlerThread mBackgroundThread;
	private Handler mBackgroundHandler;
	private ImageReader mImageReader;

	/* misc */
	private final String LOG_ID = CameraActivity.class.getName();
	private Activity activity = null;
	private Size mPreviewSize;
	private int mSensorOrientation;
	private String isbn = null;

	private SharedPreferences preferences = null;
	private boolean hasDlWithoutWifi = false;
	private boolean hasAutoDl = false;

	private String WIFI_DL_KEY = "wifilessDownload";
	private String AUTO_DL_KEY = "autoDownload";

	/* to prevent the app from exiting before closing the camera. */
	private Semaphore mCameraOpenCloseLock = new Semaphore(1);

	private static final int MAX_PREVIEW_WIDTH = 3840;
	private static final int MAX_PREVIEW_HEIGHT = 2160;

	private static final int REQUEST_CAMERA_PERMISSION = 87878;

	private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

		@Override
		public void onOpened(@NonNull CameraDevice cameraDevice) {
			Log.v("CAMERA_ACTIVITY","camera open");
			// This method is called when the camera is opened.  We start camera preview here.
			mCameraOpenCloseLock.release();
			mCameraDevice = cameraDevice;
			createCameraPreviewSession();
		}

		@Override
		public void onDisconnected(@NonNull CameraDevice cameraDevice) {
			mCameraOpenCloseLock.release();
			cameraDevice.close();
			mCameraDevice = null;
		}

		@Override
		public void onError(@NonNull CameraDevice cameraDevice, int error) {
			mCameraOpenCloseLock.release();
			killActivity();
		}

	};

	private final TextureView.SurfaceTextureListener mSurfaceTextureListener
			= new TextureView.SurfaceTextureListener() {

		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
			openCamera(width, height);
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
			configureTransform(width, height);
		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
			return true;
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture texture) {
		}

	};

	private CameraCaptureSession.CaptureCallback mCaptureCallback
			= new CameraCaptureSession.CaptureCallback() {};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_layout);

		activity = this;

		mTextureView = (AutoFitTextureView) findViewById(R.id.cameraTextureView);
		mTextureView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(processor != null) {
					String active = processor.getActive();
					if(active.length() > 0) {
						Intent data = new Intent();
						data.setData(Uri.parse(active));
						setResult(RESULT_OK,data);
						killActivity();
					}
				}
			}
		});
		infoBtn = (Button) findViewById(R.id.cameraPreviewInfo);
		ownedBtn = (Button) findViewById(R.id.cameraPreviewToggleOwned);

		infoBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// trigger entryview with isbn code if any.
				if(isbn != null && isbn.length() > 0) {
					ViewUtils.launchEntryView(isbn, activity);
				}
			}
		});
		ownedBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// update current ownership
				if(isbn != null && isbn.length() > 0) {
					boolean isOwned = DatabaseUtils.toggleOwnedState(isbn);
					if(processor != null) {
						processor.updateOwnedTf(isOwned);
					}
				}
			}
		});

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
				if(WIFI_DL_KEY.equals(s) || AUTO_DL_KEY.equals(s)) {
					updateProperties();
				}
			}
		});

		Log.v("CAMERA_ACTIVITY","activity created");
		updateProperties();
		onIsbnUpdate("");
	}

	private void createCameraPreviewSession() {
		try {
			SurfaceTexture texture = mTextureView.getSurfaceTexture();
			assert texture != null;

			// We configure the size of default buffer to be the size of camera preview we want.
			texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

			// This is the output Surface we need to start preview.
			Surface surface = new Surface(texture);

			// We set up a CaptureRequest.Builder with the output Surface.
			mPreviewRequestBuilder
					= mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			mPreviewRequestBuilder.addTarget(surface);

			// Here, we create a CameraCaptureSession for camera preview.
			mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
					new CameraCaptureSession.StateCallback() {

						@Override
						public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
							// The camera is already closed
							if (null == mCameraDevice) {
								return;
							}

							// When the session is ready, we start displaying the preview.
							mCaptureSession = cameraCaptureSession;
							try {
								// Auto focus should be continuous for camera preview.
								mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
										CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

								// Finally, we start displaying the camera preview.
								mPreviewRequest = mPreviewRequestBuilder.build();
								mCaptureSession.setRepeatingRequest(mPreviewRequest,
										mCaptureCallback, mBackgroundHandler);
							} catch (CameraAccessException e) {
								e.printStackTrace();
							}
						}

						@Override
						public void onConfigureFailed(
								@NonNull CameraCaptureSession cameraCaptureSession) {
							Log.e(LOG_ID,"Camera config failed.");
						}
					}, null
			);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		// response for camera permission request
		if(requestCode == REQUEST_CAMERA_PERMISSION) {
			if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// Permission was granted. Relaunch camera.
				openCamera(mTextureView.getWidth(), mTextureView.getHeight());
			}
		}
	}

	private void openCamera(int width, int height) {

		if(checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			Log.v("CAMERA_ACTIVITY", "NO CAMERA PERMISSION");

			// request user for camera permission.
			this.requestPermissions(new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA_PERMISSION);
			return;
		}

		setUpCameraOutputs(width, height);
		configureTransform(width, height);
		CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
		try {
			if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
				throw new RuntimeException("Time out waiting to lock camera opening.");
			}
			manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
			Log.v("CAMERA_ACTIVITY","camera should be up and running");
		} catch(Exception e) {
			Log.e("CAMERA_ACTIVITY","camera exception",e);
		}
	}

	private void closeCamera() {
		try {
			mCameraOpenCloseLock.acquire();
			if (null != mCaptureSession) {
				mCaptureSession.close();
				mCaptureSession = null;
			}
			if (null != mCameraDevice) {
				mCameraDevice.close();
				mCameraDevice = null;
			}
			if (null != mImageReader) {
				mImageReader.close();
				mImageReader = null;
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
		} finally {
			mCameraOpenCloseLock.release();
		}
	}

	private void setUpCameraOutputs(int width, int height) {
		CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
		try {
			for (String cameraId : manager.getCameraIdList()) {
				CameraCharacteristics characteristics
						= manager.getCameraCharacteristics(cameraId);

				// We don't use a front facing camera in this sample.
				Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
				if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
					continue;
				}

				StreamConfigurationMap map = characteristics.get(
						CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
				if (map == null) {
					continue;
				}

				// For still image captures, we use the largest available size.
				Size largest = Collections.max(
						Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
						new CompareArea());
				mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
						ImageFormat.JPEG, 2);

				// Find out if we need to swap dimension to get the preview size relative to sensor
				// coordinate.
				int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
				//noinspection ConstantConditions
				mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
				boolean swappedDimensions = false;
				switch (displayRotation) {
					case Surface.ROTATION_0:
					case Surface.ROTATION_180:
						if (mSensorOrientation == 90 || mSensorOrientation == 270) {
							swappedDimensions = true;
						}
						break;
					case Surface.ROTATION_90:
					case Surface.ROTATION_270:
						if (mSensorOrientation == 0 || mSensorOrientation == 180) {
							swappedDimensions = true;
						}
						break;
					default:
						Log.e(LOG_ID, "Display rotation is invalid: " + displayRotation);
				}

				Point displaySize = new Point();
				activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
				int rotatedPreviewWidth = width;
				int rotatedPreviewHeight = height;
				int maxPreviewWidth = displaySize.x;
				int maxPreviewHeight = displaySize.y;

				if (swappedDimensions) {
					rotatedPreviewWidth = height;
					rotatedPreviewHeight = width;
					maxPreviewWidth = displaySize.y;
					maxPreviewHeight = displaySize.x;
				}

				if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
					maxPreviewWidth = MAX_PREVIEW_WIDTH;
				}

				if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
					maxPreviewHeight = MAX_PREVIEW_HEIGHT;
				}

				// Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
				// bus' bandwidth limitation, resulting in gorgeous previews but the storage of
				// garbage capture data.
				mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
						rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
						maxPreviewHeight, largest);


				// if optimal is smaller than screen size use screen size instead.
				int pWidth = mPreviewSize.getWidth();
				int pHeight = mPreviewSize.getHeight();
				int dWidth = (displaySize.x > displaySize.y) ? displaySize.x : displaySize.y;
				int dHeight = (displaySize.x > displaySize.y) ? displaySize.y : displaySize.x;
				if(pWidth < dWidth) pWidth = dWidth;
				if(pHeight < dHeight) pHeight = dHeight;

				// We fit the aspect ratio of TextureView to the size of preview we picked.
				int orientation = getResources().getConfiguration().orientation;
				if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
					mTextureView.setAspectRatio(pWidth, pHeight);
				} else {
					mTextureView.setAspectRatio(pHeight, pWidth);
				}

				mCameraId = cameraId;
				Log.v(LOG_ID,"out is " + mCameraId);
				return;
			}
		} catch (CameraAccessException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			// Currently an NPE is thrown when the Camera2API is used but not supported
		}
	}

	/**
	 * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
	 * is at least as large as the respective texture view size, and that is at most as large as the
	 * respective max size, and whose aspect ratio matches with the specified value. If such size
	 * doesn't exist, choose the largest one that is at most as large as the respective max size,
	 * and whose aspect ratio matches with the specified value.
	 *
	 * @param choices           The list of sizes that the camera supports for the intended output
	 *                          class
	 * @param textureViewWidth  The width of the texture view relative to sensor coordinate
	 * @param textureViewHeight The height of the texture view relative to sensor coordinate
	 * @param maxWidth          The maximum width that can be chosen
	 * @param maxHeight         The maximum height that can be chosen
	 * @param aspectRatio       The aspect ratio
	 * @return The optimal {@code Size}, or an arbitrary one if none were big enough
	 */
	private Size chooseOptimalSize(Size[] choices, int textureViewWidth,
										  int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

		// Collect the supported resolutions that are at least as big as the preview Surface
		List<Size> bigEnough = new ArrayList<>();
		// Collect the supported resolutions that are smaller than the preview Surface
		List<Size> notBigEnough = new ArrayList<>();
		int w = aspectRatio.getWidth();
		int h = aspectRatio.getHeight();
		for (Size option : choices) {

			if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight) {
				if (option.getWidth() >= textureViewWidth &&
						option.getHeight() >= textureViewHeight) {
					bigEnough.add(option);
				} else {
					notBigEnough.add(option);
				}
			}
		}

		// Pick the smallest of those big enough. If there is no one big enough, pick the
		// largest of those not big enough.
		if (bigEnough.size() > 0) {
			return Collections.min(bigEnough, new CompareArea());
		} else if (notBigEnough.size() > 0) {
			return Collections.max(notBigEnough, new CompareArea());
		} else {
			return choices[0];
		}
	}

	/**
	 * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
	 * This method should be called after the camera preview size is determined in
	 * setUpCameraOutputs and also the size of `mTextureView` is fixed.
	 *
	 * @param viewWidth  The width of `mTextureView`
	 * @param viewHeight The height of `mTextureView`
	 */
	private void configureTransform(int viewWidth, int viewHeight) {
		if (null == mTextureView || null == mPreviewSize || null == activity) {
			return;
		}
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		Matrix matrix = new Matrix();
		RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
		RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
		float centerX = viewRect.centerX();
		float centerY = viewRect.centerY();
		if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
			bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
			matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
			float scale = Math.max(
					(float) viewHeight / mPreviewSize.getHeight(),
					(float) viewWidth / mPreviewSize.getWidth());
			matrix.postScale(scale, scale, centerX, centerY);
			matrix.postRotate(90 * (rotation - 2), centerX, centerY);
		} else if (Surface.ROTATION_180 == rotation) {
			matrix.postRotate(180, centerX, centerY);
		}
		mTextureView.setTransform(matrix);
	}

	/**
	 * Kill activity and close the camera.
	 */
	private void killActivity() {
		if(mCameraDevice != null)
			mCameraDevice.close();
		mCameraDevice = null;
		if (null != activity) {
			activity.finish();
		}
	}

	public void onResume() {
		super.onResume();
		startBackgroundThread();

		// When the screen is turned off and turned back on, the SurfaceTexture is already
		// available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
		// a camera and start preview from here (otherwise, we wait until the surface is ready in
		// the SurfaceTextureListener).
		if (mTextureView.isAvailable()) {
			openCamera(mTextureView.getWidth(), mTextureView.getHeight());
		} else {
			mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
		}
	}

	public void onPause() {
		super.onPause();
		closeCamera();
		stopBackgroundThread();
	}

	/**
	 * Starts a background thread and its {@link Handler}.
	 */
	private void startBackgroundThread() {
		mBackgroundThread = new HandlerThread("CameraBackground");
		mBackgroundThread.start();
		mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

		processor = new BarcodeProcessor(this, new BarcodeProcessor.BarcodeUpdateEvent() {
			@Override
			public void doUpdate(String isbn) {
				onIsbnUpdate(isbn);
			}
		});
		processor.start();
	}

	/**
	 * Stops the background thread and its {@link Handler}.
	 */
	private void stopBackgroundThread() {
		mBackgroundThread.quitSafely();
		try {
			mBackgroundThread.join();
			mBackgroundThread = null;
			mBackgroundHandler = null;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		processor.stop();
		processor = null;
	}

	private void onIsbnUpdate(final String isbn) {
		this.isbn = isbn;
		boolean visible = isbn != null && isbn.length() > 0;
		infoBtn.setVisibility(visible ? View.VISIBLE : View.GONE);
		ownedBtn.setVisibility(visible ? View.VISIBLE : View.GONE);

		// check if isbn data should be downloaded ?
		if(visible) {
			// check if currently allowed
			boolean allowedConn = ConnUtils.hasInternet(activity) && hasAutoDl;
			if(allowedConn && !hasDlWithoutWifi) {
				allowedConn = ConnUtils.hasWifi(activity);
			}

			if(allowedConn) {
				final IsbnDbHelper db = (IsbnDbHelper) DatabaseAccess.get().getIsbnDb();
				Cursor isbnCursor = db.getData(db.REGISTRY_KEY + isbn);
				// check if cursor has no data entries
				if(isbnCursor.getCount() == 0) {
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
							}
						}
					}).start();
				}
			}
		}
	}

	private void updateProperties() {;
		hasDlWithoutWifi = preferences.getBoolean(WIFI_DL_KEY,false);
		hasAutoDl = preferences.getBoolean(AUTO_DL_KEY,false);
		Log.v(getClass().getName(),"hasDlWithoutWifi " + hasDlWithoutWifi);
		Log.v(getClass().getName(),"hasAutoDl " + hasAutoDl);
	}

}
