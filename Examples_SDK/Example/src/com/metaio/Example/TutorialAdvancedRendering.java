// Copyright 2007-2014 metaio GmbH. All rights reserved.
package com.metaio.Example;

import java.io.File;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.tools.io.AssetsManager;

public class TutorialAdvancedRendering extends ARViewActivity
{
	@Override
	protected int getGUILayout()
	{
		// Attaching layout to the activity
		return R.layout.tutorial_advanced_rendering;
	}

	public void onButtonClick(View v)
	{
		finish();
	}

	@Override
	protected void loadContents()
	{
		try
		{
			// Getting a file path for tracking configuration XML file
			final File trackingConfigFile = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialAdvancedRendering/Assets/TrackingData_MarkerlessFast.xml");

			// Assigning tracking configuration
			boolean result = metaioSDK.setTrackingConfiguration(trackingConfigFile);
			MetaioDebug.log("Tracking data loaded: " + result);

			// loading geometry
			final File filepath = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialAdvancedRendering/Assets/metaioman.md2");
			if (filepath != null) 
			{
				IGeometry geometry = metaioSDK.createGeometry(filepath);
				if (geometry != null) 
				{
					geometry.setScale(4f);
					MetaioDebug.log("Loaded geometry " + filepath);
				} else {
					MetaioDebug.log(Log.ERROR, "Error loading geometry: "+ filepath);
				}
			}

			// Enable advanced rendering
			if(!metaioSDK.autoEnableAdvancedRenderingFeatures()) {
				final TutorialAdvancedRendering _this = this;
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						AlertDialog alert;
						alert = new AlertDialog.Builder(_this)
						.setTitle("Advanced Rendering Features Disabled.")
						.setMessage("This device does not support the advanced rendering features of the metaio SDK.")
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						})
						.create();
						alert.show();
					}
				});
			}
		// As an alternative, metaioSDK.autoEnableAdvancedRenderingFeatures() should be used to
			// only enable advanced rendering on devices that are capable of performing it at a
			// decent speed.

			// Adjust effects
			metaioSDK.setDepthOfFieldParameters(0.1f, 0.6f, 0.2f);
			// Slightly reduce amount of motion blur
			metaioSDK.setMotionBlurIntensity(0.8f);
		}
		catch (Exception e)
		{
			MetaioDebug.printStackTrace(Log.ERROR, e);
		}
	}


	@Override
	protected void onGeometryTouched(IGeometry geometry)
	{
		// Not used in this tutorial
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler()
	{
		// No callbacks needed in this tutorial
		return null;
	}
}
