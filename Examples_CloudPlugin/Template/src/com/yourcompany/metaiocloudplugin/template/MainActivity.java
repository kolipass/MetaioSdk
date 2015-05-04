package com.yourcompany.metaiocloudplugin.template;

import java.io.File;
import java.util.Date;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.metaio.cloud.plugin.MetaioCloudPlugin;
import com.metaio.cloud.plugin.util.MetaioCloudUtils;
import com.metaio.cloud.plugin.view.ARMetaioCloudPluginManager;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.MetaioWorldPOIManagerCallback;
import com.metaio.sdk.fragments.ShareScreenshotFragment;
import com.metaio.sdk.jni.DataSourceEvent;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.sdk.jni.TrackingValuesVector;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.SystemInfo;

public class MainActivity extends MetaioCloudPluginBaseActivity implements DialogInterface.OnDismissListener
{
	/**
	 * GUI overlay
	 */
	private RelativeLayout mGUIView;

	/**
	 * Progress bar view
	 */
	private ProgressBar progressView;

    /**
     * Load native libs required by the Metaio SDK
     */
    protected void loadNativeLibs() throws UnsatisfiedLinkError, RuntimeException
    {
        IMetaioSDKAndroid.loadNativeLibs();
        MetaioDebug.log(Log.INFO, "MetaioSDK libs loaded for " + SystemInfo.getDeviceABI() + " using "
                + com.metaio.sdk.jni.SystemInfo.getAvailableCPUCores() + " CPU cores");
    }

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		MetaioCloudPlugin.isDebuggable = BuildConfig.DEBUG;

		// TODO Set authentication if a private channel is used
		// MetaioCloudPlugin.setAuthentication("username", "password");

		// Ensure that initialize is called when this activity is started/recreated to initialize
		// all Cloud Plugin related settings, including setting the correct application identifier.
		// Call synchronously because super.onCreate may already have Cloud Plugin logic. Note
		// that startJunaio is already called in SplashActivity, but in case the application gets
		// restarted, or memory is low, or this activity is started directly without opening
		// SplashActivity, we have to make sure this is always called.
        try {
            loadNativeLibs();
        } catch (Exception e) {
            Utils.showErrorForCloudPluginResult(MetaioCloudPlugin.ERROR_CPU_NOT_SUPPORTED, this);
            return;
        }
		int result = MetaioCloudPlugin.initialize(null, getApplicationContext());

		// Window managed wake lock (no permissions, no accidentally kept on)
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Optionaly add GUI
		if (mGUIView==null)
			mGUIView = (RelativeLayout)getLayoutInflater().inflate(R.layout.arview, null);

		progressView = (ProgressBar)mGUIView.findViewById(R.id.progressBar);

		// Init the AREL webview. Pass a container if you want to use a ViewPager or Horizontal
		// Scroll View over the camera preview or the root view.
		mMetaioCloudPluginManager.initARELWebView(mGUIView);

		// Used to resume the camera preview
		mMetaioCloudPluginManager.mIsInLiveMode = true;

		if (result!=MetaioCloudPlugin.SUCCESS)
			Utils.showErrorForCloudPluginResult(result, this);
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		// add GUI layout
		if (mGUIView!=null)
			addContentView(mGUIView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		// comes from splash activity
		final int channelID = getIntent().getIntExtra(getPackageName()+".CHANNELID", -1);
		if (channelID>0)
		{
			// Clear the intent extra before proceeding
			getIntent().removeExtra(getPackageName()+".CHANNELID");
			mMetaioCloudPluginManager.setChannel(channelID);
		}
	}

	public void onScreenshot(Bitmap bitmap, boolean saveToGalleryWithoutDialog)
	{
		// this is triggered by calling takeScreenshot() or through AREL
		String filename = "junaio-"+DateFormat.format("yyyyMMdd-hhmmss", new Date())+".jpg";

		try
		{
			boolean result =
					MetaioCloudUtils.writeToFile(bitmap, CompressFormat.JPEG, 100, MetaioCloudPlugin.mCacheDir,
							filename, false);

			if (result)
			{
				if (!saveToGalleryWithoutDialog) // meaning share it
				{
					final String path = new File(MetaioCloudPlugin.mCacheDir, filename).getAbsolutePath();

					// create a new instance of the screenshot dialog, you can provide custom text
					// for the buttons
					ShareScreenshotFragment fragment =
							ShareScreenshotFragment.newInstance(path, "Share screenshot",
									getString(R.string.BTN_SAVE_SCREENSHOT), "Share screenshot");

					// optionally set a notification to tell the user a screenshot was taken
					fragment.setNotification(R.drawable.icon_placeholder, getString(R.string.MSGI_IMAGE_SAVED),
							getString(R.string.BTN_VIEW_IMAGE));

					// optionally add some text to the sharing intent
					Intent sharingIntent = new Intent(Intent.ACTION_SEND);
					sharingIntent.putExtra(Intent.EXTRA_TEXT, "This was taken with Metaio SDK");
					sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Check this cool AR!!");

					fragment.setSharingIntent(sharingIntent);

					// display the fragment
					fragment.show(getSupportFragmentManager(), "share_screenshot");
				}
			}
		}
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "onScreenshot: Error formatting date");
			MetaioDebug.printStackTrace(Log.ERROR, e);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public MetaioWorldPOIManagerCallback getMetaioWorldPOIManagerCallback()
	{
		if (mMetaioWorldPOIManagerCallback==null)
			mMetaioWorldPOIManagerCallback = new MetaioPOIManager(this);
		return mMetaioWorldPOIManagerCallback;
	}

	@Override
	public ARMetaioCloudPluginManager getARMetaioCloudPluginManagerInstance()
	{
		if (mMetaioCloudPluginManager==null)
			mMetaioCloudPluginManager = new MyCloudPlugin(this);
		return mMetaioCloudPluginManager;
	}

	/**
	 * Override {@link MetaioWorldPOIManagerCallback} to provide your own implementation of some of
	 * the methods
	 */
	class MetaioPOIManager extends MetaioWorldPOIManagerCallback
	{

		public MetaioPOIManager(Activity activity)
		{
			super(activity);
		}

		@Override
		public void onRadarPicked()
		{
			super.onRadarPicked();
		}
		
		@Override
		public void onTrackingEvent(TrackingValuesVector trackingValues)
		{
			// TODO Auto-generated method stub
			super.onTrackingEvent(trackingValues);
			Log.i("TRACKING",trackingValues.toString());
		}

		@Override
		protected void onSaveScreenshot(Bitmap screenshot, boolean saveToGalleryWithoutDialog)
		{
			MainActivity.this.onScreenshot(screenshot, saveToGalleryWithoutDialog);
		}
	}

	class MyCloudPlugin extends ARMetaioCloudPluginManager
	{

		public MyCloudPlugin(Activity activity)
		{
			super(activity);
		}

		@Override
		public MetaioWorldPOIManagerCallback getMetaioWorldPOIManagerCallback()
		{
			return MainActivity.this.getMetaioWorldPOIManagerCallback();
		}


		@Override
		public void showProgress(final boolean show)
		{

			progressView.post(new Runnable()
			{

				@Override
				public void run()
				{
					progressView.setIndeterminate(true);
					progressView.setVisibility(show?View.VISIBLE:View.INVISIBLE);
				}
			});
		}

		@Override
		public void showProgressBar(final float progress, final boolean show)
		{

			progressView.post(new Runnable()
			{

				@Override
				public void run()
				{
					progressView.setIndeterminate(false);
					progressView.setProgress((int)progress);
					progressView.setVisibility(show?View.VISIBLE:View.INVISIBLE);

				}
			});
		}

		@Override
		public void onSceneReady()
		{
			super.onSceneReady();
		}

		// called by the plugin
		@Override
		public void onSurfaceChanged(int width, int height)
		{
			Utils.log(getClass().getSimpleName()+".onSurfaceChanged");
			super.onSurfaceChanged(width, height);

			// get radar margins from the resources (this will make the values density independant)
			float marginTop = getResources().getDimension(R.dimen.radarTop);
			float marginRight = getResources().getDimension(R.dimen.radarRight);
			float radarScale = getResources().getDimension(R.dimen.radarScale);
			// set the radar to the top right corner and add some margin, scale to 1
			mMetaioCloudPluginManager.setRadarProperties(IGeometry.ANCHOR_TOP|IGeometry.ANCHOR_RIGHT, new Vector3d(
					-marginRight, -marginTop, 0f), new Vector3d(radarScale, radarScale, 1f));
		}

		@Override
		public void onSurfaceCreated()
		{
			super.onSurfaceCreated();
		}

		@Override
		public void onSurfaceDestroyed()
		{
			super.onSurfaceDestroyed();
		}

		@Override
		public void onServerEvent(DataSourceEvent event)
		{
			switch (event)
			{
				case DataSourceEventNoPoisReturned:
					MetaioCloudUtils.showToast(MainActivity.this, getString(R.string.MSGI_POIS_NOT_FOUND));
					break;
				case DataSourceEventServerError:
					MetaioCloudUtils.showToast(MainActivity.this, getString(R.string.MSGE_TRY_AGAIN));
					break;
				case DataSourceEventServerNotReachable:
				case DataSourceEventCouldNotResolveServer:
					MetaioCloudUtils.showToast(MainActivity.this, getString(R.string.MSGW_SERVER_UNREACHABLE));
					break;
				default:
					break;
			}
		}
	}
	

	@Override
	public void onDismiss(DialogInterface dialog)
	{
		onActivityResult(MetaioWorldPOIManagerCallback.REQUEST_POI_CONTEXT, RESULT_OK, null);
	}

}
