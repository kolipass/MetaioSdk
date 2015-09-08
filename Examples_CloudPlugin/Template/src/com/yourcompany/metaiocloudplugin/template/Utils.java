package com.yourcompany.metaiocloudplugin.template;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.util.Log;

import com.metaio.cloud.plugin.MetaioCloudPlugin;

public final class Utils
{
	/**
	 * Standard tag used for all the debug messages
	 */
	public static final String TAG = "MetaioCloudPluginTemplate";

	/**
	 * Display log messages with debug priority
	 * 
	 * @param msg Message to display
	 * @see Log#d(String, String)
	 */
	public static void log(String msg)
	{
		if (msg != null)
			Log.d(TAG, msg);
	}
	
	/**
	 * Shows an error dialog for a non-success Cloud Plugin result value
	 *
	 * @param result Error result
	 * @param activity Parent activity
	 */
	public static void showErrorForCloudPluginResult(int result, final Activity activity)
	{
		AlertDialog.Builder builder = new Builder(activity);
		builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				activity.finish();
			}
		});

		// Default message if not set below
		builder.setMessage("Error");
		
		switch (result)
		{
			case MetaioCloudPlugin.ERROR_EXSTORAGE:
				builder.setMessage("External storage is not available. If you have your USB plugged in, set the USB mode to only charge");
				break;
			case MetaioCloudPlugin.ERROR_INSTORAGE:
				builder.setMessage("Internal storage is not available");
				break;
			case MetaioCloudPlugin.CANCELLED:
				log("Starting junaio cancelled");
				break;
			case MetaioCloudPlugin.ERROR_CPU_NOT_SUPPORTED:
				log("CPU is not supported");
				break;
			case MetaioCloudPlugin.ERROR_GOOGLE_SERVICES:
				log("Google APIs not found");
				break;
		}

		builder.show();
	}
}
