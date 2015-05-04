// Copyright 2007-2014 metaio GmbH. All rights reserved.
package com.metaio.Example;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.metaio.sdk.ARELActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.tools.io.AssetsManager;


@SuppressLint("SetJavaScriptEnabled")
public class MainActivity extends Activity
{
	
	WebView mWebView;

	/**
	 * Task that will extract all the assets
	 */
	AssetsExtracter mTask;
	
	/**
	 * Progress view
	 */
	View mProgress;

	/**
	 * True while launching a tutorial, used to prevent
	 * multiple launches of the tutorial
	 */
	boolean mLaunchingTutorial;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.webview);
		
		// Enable metaio SDK log messages based on build configuration
		MetaioDebug.enableLogging(BuildConfig.DEBUG);
		 
		mProgress = findViewById(R.id.progress);
		mWebView = (WebView) findViewById(R.id.webview);
		
		// extract all the assets
		mTask = new AssetsExtracter();
		mTask.execute(0);
		
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		mWebView.resumeTimers();
		mLaunchingTutorial = false;
	}
	
	@Override
	protected void onPause() 
	{
		super.onPause();
		mWebView.pauseTimers();
	}

	@Override
	public void onBackPressed() 
	{
		// if web view can go back, go back
		if (mWebView.canGoBack())
			mWebView.goBack();
		else
			super.onBackPressed();
	}
	
	/**
	 * This task extracts all the assets to an external or internal location
	 * to make them accessible to Metaio SDK
	 */
	private class AssetsExtracter extends AsyncTask<Integer, Integer, Boolean>
	{

		@Override
		protected void onPreExecute() 
		{
			mProgress.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) 
		{
			try 
			{
				// Extract all assets except Menu. Overwrite existing files for debug build only.
				final String[] ignoreList = {"Menu", "webkit", "sounds", "images", "webkitsec"};
				AssetsManager.extractAllAssets(getApplicationContext(), "", ignoreList, BuildConfig.DEBUG);
			} 
			catch (IOException e) 
			{
				MetaioDebug.printStackTrace(Log.ERROR, e);
				return false;
			}

			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) 
		{
			mProgress.setVisibility(View.GONE);
			
			if (result)
			{
				WebSettings settings = mWebView.getSettings();
				
				settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
				settings.setJavaScriptEnabled(true);
				
				mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
				mWebView.setWebViewClient(new WebViewHandler());
				mWebView.loadUrl("file:///android_asset/Menu/index.html");
				mWebView.setVisibility(View.VISIBLE);
			}
			else
			{
				MetaioDebug.log(Log.ERROR, "Error extracting assets, closing the application...");
				finish();
			}
		}
	}
	
	/**
	 * Display a short toast message
	 * @param message Message to display
	 */
	private void showToast(final String message)
	{
		Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
		toast.show();
	}
	
	class WebViewHandler extends WebViewClient
	{
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) 
		{
			mProgress.setVisibility(View.VISIBLE);
		}
		
		@Override
		public void onPageFinished(WebView view, String url) 
		{
			mProgress.setVisibility(View.GONE);
		}
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) 
		{
			if (!url.toLowerCase(Locale.US).startsWith("metaiosdkexample"))
			{
				if (url.contains("metaio.com"))
				{
					// Open external browser
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					intent.addCategory(Intent.CATEGORY_BROWSABLE);
					startActivity(intent);
					return true;
				}

				return false;
			}
			else if (mLaunchingTutorial)
			{
				return true;
			}

			startTutorial(url);
			return true;
		}
		
		/**
		 * Start a Native or AREL tutorial from local URL
		 * @param url URL with prefix metaiosdkexample:// or metaiosdkexamplearel://
		 */
		private void startTutorial(final String url)
		{
			final String tutorialId = url.substring(url.lastIndexOf("=") + 1);
			if (tutorialId == null || tutorialId.length() == 0)
			{
				MetaioDebug.log(Log.ERROR, "Invalid tutorial URL: "+url);
				showToast("Invalid tutorial URL: "+url);
				return;
			}
			
			MetaioDebug.log("Tutorial ID detected: "+tutorialId);
			
			if (url.toLowerCase(Locale.US).startsWith("metaiosdkexample://"))
			{
				try 
				{
					final Class<?> activity = Class.forName(getPackageName()+".Tutorial"+tutorialId);
					mLaunchingTutorial = true;
					startActivity(new Intent(getApplicationContext(), activity));
				} 
				catch (ClassNotFoundException e) 
				{
					MetaioDebug.log(Log.ERROR, "Invalid tutorial id, class not found: "+tutorialId);
					showToast("Invalid tutorial id, class not found: "+tutorialId);
				}
			}
			else if (url.toLowerCase(Locale.US).startsWith("metaiosdkexamplearel://"))
			{
				final String arelConfigFile = "index.xml";
				final File arelConfigFilePath = AssetsManager.getAssetPathAsFile(getApplicationContext(), "Tutorial"+tutorialId+"/"+arelConfigFile);
				if (arelConfigFilePath != null)
				{
					MetaioDebug.log("AREL configuration to be passed to intent: "+arelConfigFilePath.getPath());
					Intent intent = new Intent(getApplicationContext(), ARELViewActivity.class);
					intent.putExtra(getPackageName()+ARELActivity.INTENT_EXTRA_AREL_SCENE, arelConfigFilePath);
					mLaunchingTutorial = true;
					startActivity(intent);
				}
				else
				{
					MetaioDebug.log(Log.ERROR, "Invalid tutorial id, AREL configuration file not found: "+tutorialId);
					showToast("Invalid tutorial id, AREL configuration file not found: "+tutorialId);
				}
			}
		}
	}
}

