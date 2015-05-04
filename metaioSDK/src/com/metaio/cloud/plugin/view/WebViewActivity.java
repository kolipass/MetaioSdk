// Copyright 2007-2014 Metaio GmbH. All rights reserved.
package com.metaio.cloud.plugin.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.metaio.R;
import com.metaio.cloud.plugin.MetaioCloudPlugin;
import com.metaio.cloud.plugin.util.JunaioChannel;
import com.metaio.cloud.plugin.util.MetaioCloudUtils;

@SuppressWarnings("deprecation")
public class WebViewActivity extends Activity
{
	/**
	 * Web view
	 */
	private WebView mWebView;

	/**
	 * Progress bar displayed when loading a page
	 */
	private ProgressBar mProgressView;

	/**
	 * Navigation buttons that can be disabled
	 */
	private ImageButton mButtonBack, mButtonForward, mButtonStop;

	private static final int ID_OPENWINDOWLINK = 0x5;

	/**
	 * Set this to true to have a fullscreen webview without navigation controls
	 */
	public static boolean hideNavigationBar;

	@SuppressLint("SetJavaScriptEnabled")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		try
		{
			// get url and navigatio preference (default true)
			final String url = getIntent().getStringExtra(getPackageName() + ".URL");
			final boolean navigation = getIntent().getBooleanExtra(getPackageName() + ".NAVIGATION", true);

			setContentView(R.layout.webviewnav);

			// if we want navigation bar and not hide it, make it visible. Make it invisible if not.
			if (navigation && !hideNavigationBar)
			{
				findViewById(R.id.webBottomBar).setVisibility(View.VISIBLE);
			}
			else
			{
				findViewById(R.id.webBottomBar).setVisibility(View.GONE);
			}

			mButtonBack = (ImageButton)findViewById(R.id.buttonWebBack);
			mButtonForward = (ImageButton)findViewById(R.id.buttonWebForward);
			mButtonStop = (ImageButton)findViewById(R.id.buttonWebStop);

			mProgressView = (ProgressBar)findViewById(R.id.progressBar);
			mProgressView.setIndeterminate(true);

			// init webview
			mWebView = (WebView)findViewById(R.id.webView);

			// disable hw accel as it creates flickering pages, html5 video won't work with this
			if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 16)
				mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

			// This hides white bar on the right
			mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

			WebSettings settings = mWebView.getSettings();
			// enable plugins before java script
			settings.setPluginState(PluginState.ON);

			// TODO test these two settings with a big webpage (cafe lotty?)
			settings.setLoadWithOverviewMode(true);
			settings.setUseWideViewPort(true);
			// enable javascript and zoom controls
			settings.setJavaScriptEnabled(true);
			settings.setBuiltInZoomControls(true);
			settings.setGeolocationEnabled(true);
			settings.setDatabaseEnabled(true);
			String databasePath = getDir("database_ext", Context.MODE_PRIVATE).getPath();
			settings.setDatabasePath(databasePath);
			settings.setGeolocationDatabasePath(databasePath);
			settings.setDomStorageEnabled(true);
			settings.setAppCacheEnabled(true);

			// allow XMLHttpRequests
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
				settings.setAllowUniversalAccessFromFileURLs(true);

			MetaioWebViewClient client = new MetaioWebViewClient();
			mWebView.setWebViewClient(client);
			mWebView.setWebChromeClient(new MetaioWebChromeClient());

			registerForContextMenu(mWebView);

			if (savedInstanceState != null)
			{
				mWebView.restoreState(savedInstanceState);
			}
			else
			{

				// if we don't have to override the url, load it in the webview
				if (!client.shouldOverrideUrlLoading(mWebView, url))
					mWebView.loadUrl(url);
			}
		}
		catch (Exception e)
		{
			MetaioCloudPlugin.log(Log.ERROR, "WebViewActivity.onCreate", e);
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		try
		{
			MetaioCloudUtils.unbindDrawables(findViewById(android.R.id.content));

			mWebView.destroy();

		}
		catch (Exception e)
		{

		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		mWebView.saveState(outState);
		super.onSaveInstanceState(outState);

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		try
		{
			// if we push back button and the browser can go back then go back
			if (keyCode == KeyEvent.KEYCODE_BACK)
			{
				if (mWebView.canGoBack())
				{
					mWebView.goBack();
					return true;
				}
			}
		}
		catch (Exception e)
		{

		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Click handlers for buttons
	 * 
	 * @param target Button that is clicked
	 */
	public void onButtonClickHandler(View target)
	{
		try
		{

			if (target.getId() == R.id.buttonWebBack)
			{
				if (mWebView.canGoBack())
				{
					mWebView.goBack();
				}
			}
			else if (target.getId() == R.id.buttonWebReload)
			{
				mWebView.reload();
			}
//			else if (target.getId() == R.id.buttonWebStop)
//			{
//				mWebView.stopLoading();
//			}
			else if (target.getId() == R.id.buttonWebForward)
			{
				if (mWebView.canGoForward())
				{
					mWebView.goForward();
				}
			}
			else if (target.getId() == R.id.buttonClose)
			{
				finish();
			}
			else if (target.getId() == R.id.buttonShare)
			{
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(mWebView.getUrl()));
				startActivity(Intent.createChooser(i, getString(R.string.BTN_SHARING)));
			}
		}
		catch (Exception e)
		{

		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);

		final HitTestResult result = ((WebView)v).getHitTestResult();

		MenuItem.OnMenuItemClickListener handler = new MenuItem.OnMenuItemClickListener()
		{
			public boolean onMenuItemClick(MenuItem item)
			{
				switch (item.getItemId())
				{
					case ID_OPENWINDOWLINK:
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getExtra()));
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
						break;

					default:
						break;
				}
				return true;
			}
		};

		if (result.getType() == HitTestResult.IMAGE_TYPE || result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE)
		{
			// Menu options for an image.
			// set the header title to the image url
			menu.setHeaderTitle(result.getExtra());
			menu.add(0, ID_OPENWINDOWLINK, 0, getString(R.string.MENU_OPEN_IMG_EXTERNAL)).setOnMenuItemClickListener(
                    handler);
		}
		else if (result.getType() == HitTestResult.ANCHOR_TYPE || result.getType() == HitTestResult.SRC_ANCHOR_TYPE)
		{
			// Menu options for a hyperlink.
			// set the header title to the link url
			menu.setHeaderTitle(result.getExtra());
			menu.add(0, ID_OPENWINDOWLINK, 0, getString(R.string.MENU_OPEN_LINK_EXTERNAL)).setOnMenuItemClickListener(
                    handler);
		}

	}

	/**
	 * Custom WebChromeClient class to handle the events
	 */
	private class MetaioWebChromeClient extends WebChromeClient
	{

		@Override
		public void onProgressChanged(WebView view, int progress)
		{
			super.onProgressChanged(view, progress);
			mProgressView.setIndeterminate(false);
			mProgressView.setProgress(progress);
		}

		@Override
		public boolean onConsoleMessage(ConsoleMessage consoleMessage)
		{
			// if we are in developer mode or beta
			if (MetaioCloudPlugin.Settings.developerMode)
				Toast.makeText(getApplicationContext(), consoleMessage.message(), Toast.LENGTH_LONG).show();
			MetaioCloudPlugin.log(consoleMessage.message());

			return super.onConsoleMessage(consoleMessage);
		}

		@Override
		public boolean onJsAlert(WebView view, String url, String message, final JsResult result)
		{
			MetaioCloudPlugin.log("onJsAlert " + message);

			// display javascript alerts as AlertDialogs
			new AlertDialog.Builder(view.getContext()).setTitle("javaScript dialog").setMessage(message)
					.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int which)
						{
							result.confirm();
						}
					}).setCancelable(false).create().show();

			return true;
		}

		@Override
		public boolean onJsTimeout()
		{
			MetaioCloudPlugin.log("onJsTimeout");
			return false;
		}

		
		public void onExceededDatabaseQuota(String url, String databaseIdentifier, long currentQuota,
				long estimatedSize, long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater)
		{
			quotaUpdater.updateQuota(estimatedSize * 2);
		}

		public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback)
		{
			callback.invoke(origin, true, false);
		}

		ViewGroup customView;
		WebChromeClient.CustomViewCallback mCustomViewCallback;

		@Override
		public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback)
		{
			customView = (ViewGroup)LayoutInflater.from(getApplicationContext()).inflate(R.layout.html5container, null);


			mCustomViewCallback = callback;
			FrameLayout mHtml5Container = (FrameLayout)customView.findViewById(R.id.html5viewcontainer);
			mHtml5Container.addView(view);

			ImageButton closeButton = (ImageButton)customView.findViewById(R.id.buttonClose);
			closeButton.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					onHideCustomView();
				}
			});

			ViewGroup root = (ViewGroup)findViewById(android.R.id.content);
			root.addView(customView);

		}

		@Override
		public void onHideCustomView()
		{
			try
			{
				if (customView != null)
				{
					ViewParent viewparent = customView.getParent();
					if (viewparent != null)
						((ViewGroup)viewparent).removeView(customView);
				}
				mCustomViewCallback.onCustomViewHidden();
			}
			catch (Exception e)
			{
				Log.e("WebViewActivity", "Can't remove custom view (video player)", e);
			}

		}

		@Override
		public Bitmap getDefaultVideoPoster()
		{
			return BitmapFactory.decodeResource(getResources(), R.drawable.default_video_poster);
		}

		@Override
		public View getVideoLoadingProgressView()
		{
			return new ProgressBar(getApplicationContext(), null, android.R.attr.progressBarStyleLarge);
		}
	}

	private class MetaioWebViewClient extends WebViewClient
	{

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
			JunaioChannel channel = null;

			try
			{
				// Try to launch default intent first if valid
				Intent intent = MetaioCloudPlugin.getDefaultIntent(url);
				if (intent != null)
				{
					try
					{
						startActivity(intent);
						return true;
					}
					catch (Exception e)
					{
						MetaioCloudPlugin.log(Log.ERROR, "WebViewActivity: Failed to launched the default intent");
						return false;
					}
				}
			}
			catch (Exception e)
			{
			}
			try
			{

				if (url.compareToIgnoreCase("junaio://?action=closewebview") == 0)
				{
					MetaioCloudPlugin.log("Closing webview: " + url);
					finish();
					return true;
				}
				else if ((channel = MetaioCloudUtils.parseUrl(Uri.parse(url))) != null && channel.getChannelID() > -1)
				{
					MetaioCloudPlugin.log("Channel ID: " + channel.getChannelID());
                    MetaioCloudPlugin.getDataSource().loadChannelInformationFromID(channel.getChannelID());

                    finish();
					return true;
				}
				// Open in Google Docs viewer if supported file type (based on file extention)
				else if (MetaioCloudPlugin.isSupportedOnGoogleDocs(url)
						&& !url.contains("docs.google.com/gview?embedded"))
				{
					url = "http://docs.google.com/gview?embedded=true&url=" + url;
					view.loadUrl(url);
					return true;

				}
				else if (url.contains("youtube.com"))
				{
					Uri parsedUrl = Uri.parse(url);
					Intent youtubeIntent = new Intent(Intent.ACTION_VIEW, parsedUrl);
					startActivity(youtubeIntent);
					finish();
					return true;
				}
				else if (url.startsWith("http"))
				{
					view.loadUrl(url);
					return false; // load it in webview
				}
				else
				{
					Intent externalIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					externalIntent.addCategory(Intent.CATEGORY_BROWSABLE);
					startActivity(externalIntent);
					return true;
				}
			}
			catch (Exception e)
			{
				return false;
			}

//			return false;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon)
		{
			MetaioCloudPlugin.log("Started loading " + url);
			mProgressView.setVisibility(View.VISIBLE);

			if (mButtonStop != null)
			{
				mButtonStop.setEnabled(true);
			}
			changeButtonState(view);
			super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{
			MetaioCloudPlugin.log("Finished loading " + url);
			mProgressView.setVisibility(View.GONE);

			if (mButtonStop != null)
			{
				mButtonStop.setEnabled(false);
			}
			changeButtonState(view);
			view.resumeTimers();
			super.onPageFinished(view, url);
		}

		@Override
		public void onLoadResource(WebView view, String url)
		{
			super.onLoadResource(view, url);
			view.resumeTimers();
			MetaioCloudPlugin.log("onLoadResource " + url);
		}

		private void changeButtonState(WebView webview)
		{
			if (mButtonBack != null && mButtonForward != null)
			{
				if (webview.canGoBack())
				{
					mButtonBack.setEnabled(true);
				}
				else
				{
					mButtonBack.setEnabled(false);
				}
				if (webview.canGoForward())
				{
					mButtonForward.setEnabled(true);
				}
				else
				{
					mButtonForward.setEnabled(false);
				}
			}
		}
	}

}
