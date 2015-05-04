// Copyright 2007-2014 Metaio GmbH. All rights reserved.
package com.metaio.sdk;

import java.io.File;
import java.util.Date;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.metaio.R;
import com.metaio.cloud.plugin.util.MetaioCloudUtils;
import com.metaio.sdk.fragments.ShareScreenshotFragment;
import com.metaio.sdk.jni.ByteBuffer;
import com.metaio.sdk.jni.GestureHandler;
import com.metaio.sdk.jni.IARELInterpreterCallback;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.Vector3d;

/**
 * This is the base activity that can be used to load and run AREL scenes
 */
public class ARELActivity extends ARViewActivity
{

	/**
	 * Intent extra key for the AREL scene file path (File object). Append this to getPackageName(),
	 * e.g.
	 * <p>
	 * <code>intent.putExtra(getPackageName()+ARELActivity.INTENT_EXTRA_AREL_SCENE, filepath);</code>
	 */
	public static final String INTENT_EXTRA_AREL_SCENE = ".AREL_SCENE";

	/**
	 * Gesture handler
	 */
	protected GestureHandlerAndroid mGestureHandler;
	/**
	 * AREL WebView where AREL HTML and JavaScript is displayed and executed
	 */
	protected WebView mWebView;

	/**
	 * This object is the main interface to AREL
	 */
	protected ARELInterpreterAndroidJava mARELInterpreter;

	/**
	 * Default ARELInterpreter callback
	 */
	private IARELInterpreterCallback mARELCallback;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		MetaioDebug.log("ARELActivity.onCreate");

		// create the AREL interpreter and its callback
		mARELInterpreter = new ARELInterpreterAndroidJava();
		IARELInterpreterCallback callback = getARELInterpreterCallback();
		if (callback != null)
			mARELInterpreter.registerCallback(callback);
		else
			MetaioDebug.log(Log.WARN, "No ARELInterpreterCallback registered!");

		// create AREL WebView
		mWebView = new WebView(this);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		MetaioDebug.log("ARELActivity.onStart");

		addContentView(mWebView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.MATCH_PARENT));

		// attach a WebView to the AREL interpreter and initialize it
		mARELInterpreter.initWebView(mWebView, this);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		MetaioDebug.log("ARELActivity.onResume");

		// Bring to front after resuming camera and GL surface
		mWebView.bringToFront();

		if (mGUIView != null)
		{
			mGUIView.bringToFront();
		}

		if (mARELInterpreter != null && mRendererInitialized)
		{
			mARELInterpreter.onResume();
		}

		// Resume WebView timers
		mWebView.resumeTimers();
	}

	@Override
	protected void onPause()
	{
		MetaioDebug.log("ARELActivity.onPause");
		if (mARELInterpreter != null && mRendererInitialized)
		{
			mARELInterpreter.onPause();
		}

		// Pause WebView timers
		mWebView.pauseTimers();
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		MetaioDebug.log("ARELActivity.onDestroy");

		try
		{
			mARELInterpreter.release();
			mARELInterpreter.delete();
			mARELInterpreter = null;
			mARELCallback.delete();
			mARELCallback = null;
			mRendererInitialized = false;
			mWebView.setOnTouchListener(null);
			mWebView = null;
			mGestureHandler.delete();
			mGestureHandler = null;
		}
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "Error releasing AREL resources");
			MetaioDebug.printStackTrace(Log.ERROR, e);
		}

		super.onDestroy();
	}

	@Override
	protected int getGUILayout()
	{
		return 0;
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler()
	{
		// Should not be used for AREL
		return null;
	}

	/**
	 * Get AREL interpreter callback handler. Note that the default interpreter calls
	 * {@link ARELActivity#loadARELScene()} in {@link IMetaioSDKCallback#onSDKReady()} callback.
	 *
	 * @return instance of class that implements IARELInterpreterCallback
	 */
	protected IARELInterpreterCallback getARELInterpreterCallback()
	{
		mARELCallback = new ARELInterpreterCallback();
		return mARELCallback;
	}

	@Override
	public void onDrawFrame()
	{
		// instead of metaioSDK.render, call ARELInterpreterAndroidJava.update()
		if (mRendererInitialized)
		{
			mARELInterpreter.update();
		}
	}

	@Override
	public void onSurfaceCreated()
	{
		super.onSurfaceCreated();
		MetaioDebug.log("ARELActivity.onSurfaceCreated");

		if (mGestureHandler == null)
		{
			// create gesture handler and initialize AREL interpreter
			mGestureHandler = new GestureHandlerAndroid(metaioSDK, GestureHandler.GESTURE_ALL, mWebView, mSurfaceView);
			mARELInterpreter.initialize(metaioSDK, mGestureHandler);
		}
		else
		{
			// Update reference to the GLSurfaceView
			mGestureHandler.setGLSurfaceView(mSurfaceView);
		}
	}

	@Override
	public void onSurfaceChanged(int width, int height)
	{
		super.onSurfaceChanged(width, height);
		MetaioDebug.log("ARELActivity.onSurfaceChanged: " + width + ", " + height);

		if (mRendererInitialized)
		{
			mARELInterpreter.onSurfaceChanged(width, height);
		}
	}

	@Override
	protected void loadContents()
	{
		// Should not be used for AREL
	}

	/**
	 * Load AREL scene
	 */
	protected void loadARELScene()
	{
		MetaioDebug.log("ARELActivity.loadARELScene");

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{

				final File filepath =
						(File)getIntent().getSerializableExtra(getPackageName() + INTENT_EXTRA_AREL_SCENE);
				if (filepath != null)
				{
					MetaioDebug.log("Loading AREL file: " + filepath.getPath());
					mARELInterpreter.loadARELFile(filepath);
				}
				else
				{
					MetaioDebug.log(Log.ERROR, "No AREL scene file passed to the intent");
				}

				// TODO: set custom radar properties
				mARELInterpreter.setRadarProperties(IGeometry.ANCHOR_TL, new Vector3d(0f), new Vector3d(1f));

				// show AREL webview and start handling touch events
				mWebView.setOnTouchListener(mGestureHandler);

			}
		});

	}

	@Override
	protected void onGeometryTouched(final IGeometry geometry)
	{
		MetaioDebug.log("MetaioSDKCallbackHandler.onGeometryTouched: " + geometry);
	}

	/**
	 * Called when share screen shot is requested
	 * 
	 * @param bitmap Screen shot
	 * @param saveToGalleryWithoutDialog true if screen should be directly saved to gallery
	 * @return true if handled
	 */
	protected boolean onScreenshot(Bitmap bitmap, boolean saveToGalleryWithoutDialog)
	{
		try
		{

			// Set your own name
			final String saveScreenshotAs = null;

			if (!saveToGalleryWithoutDialog)
			{
				// Write to external storage so the file is accessible by other applications
				final String cacheDirPath = new File(getExternalCacheDir(), "screenshots").getAbsolutePath();
				final String filename = "screenshot-" + DateFormat.format("yyyy-MM-dd-hh-mm-ss", new Date()) + ".jpg";
				final boolean result =
						MetaioCloudUtils.writeToFile(bitmap, CompressFormat.JPEG, 100, cacheDirPath, filename, false);

				if (result)
				{

					// Show share view
					final String path = new File(cacheDirPath, filename).getAbsolutePath();

					ShareScreenshotFragment fragment =
							ShareScreenshotFragment.newInstance(path, getString(R.string.LBL_SHARE_SCREENSHOT),
									getString(R.string.BTN_SAVE_SCREENSHOT), getString(R.string.LBL_SHARE_SCREENSHOT),
									saveScreenshotAs);

					// optionally, set a notification to display to the user
					fragment.setNotification(R.drawable.icon_placeholder, getString(R.string.MSGI_IMAGE_SAVED),
							getString(R.string.BTN_VIEW_IMAGE));

					// optionally, set some text to the intent that will be used to share the
					// screenshot
					Intent sharingIntent = new Intent();
					sharingIntent.putExtra(Intent.EXTRA_TEXT, "Check this screenshot!");
					sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "My cool screenshot made with my cool app");

					fragment.setSharingIntent(sharingIntent);

					fragment.show(getSupportFragmentManager(), "share_screenshot");
				}
				else
				{
					throw new Exception("onScreenshot: Failed to write screenshot to file");
				}
			}
			else
			{
				// Save the screenshot to the gallery directly
				ShareScreenshotFragment.saveScreenShot(bitmap, this, true, R.drawable.icon_placeholder,
						getString(R.string.MSGI_IMAGE_SAVED), getString(R.string.BTN_VIEW_IMAGE), saveScreenshotAs);
			}

		}
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "onScreenshot: Exception when saving screenshot");
			MetaioDebug.printStackTrace(Log.ERROR, e);
		}

		return true;
	}

	/**
	 * Default implementation of IARELInterpreterCallback
	 */
	class ARELInterpreterCallback extends IARELInterpreterCallback
	{
		@Override
		public void onSDKReady()
		{
			loadARELScene();
		}

		@Override
		public boolean shareScreenshot(ByteBuffer image, boolean saveToGalleryWithoutDialog)
		{
			byte[] bytearray = image.getBuffer();
			Bitmap bitmap = BitmapFactory.decodeByteArray(bytearray, 0, bytearray.length);
			final boolean result = onScreenshot(bitmap, saveToGalleryWithoutDialog);
			bitmap.recycle();
			bitmap = null;
			return result;
		}
	}
}
