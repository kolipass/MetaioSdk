// Copyright 2007-2014 metaio GmbH. All rights reserved.
package com.metaio.Example;

import java.io.File;

import android.util.Log;
import android.view.View;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.Camera;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.io.AssetsManager;

public class TutorialFaceTracking extends ARViewActivity 
{
	
	@Override
	protected int getGUILayout() 
	{
		// Attaching layout to the activity
		return R.layout.tutorial_hello_world; 
	}


	public void onButtonClick(View v)
	{
		finish();
	}
	
	
	@Override
	protected void startCamera() 
	{
		// Try to start the front camera if found, else start the first
		// found camera
		metaioSDK.startCamera(Camera.FACE_FRONT);
	}
	
	@Override
	protected void loadContents() 
	{
		try
		{
			
			// Assigning tracking configuration
			final boolean result = metaioSDK.setTrackingConfiguration("FACE"); 
			MetaioDebug.log("Tracking configuration loaded: " + result); 
	        
			// Getting file path for the image
			File filepath = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialFaceTracking/Assets/Thought1.png");			
			if (filepath != null) 
			{
				// Create geometry from image file
				IGeometry geometry = metaioSDK.createGeometryFromImage(filepath);
				if (geometry != null) 
				{
					// Set geometry properties
					geometry.setScale(0.5f);
					geometry.setTranslation(new Vector3d(60f, 80f, 0f));
					
				}
				else
					MetaioDebug.log(Log.ERROR, "Error loading geometry: "+filepath);
			}
			
			// Getting file path for the image
			filepath = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialFaceTracking/Assets/Thought2.png");			
			if (filepath != null) 
			{
				// Create geometry from image file
				IGeometry geometry = metaioSDK.createGeometryFromImage(filepath);
				if (geometry != null)
				{
					// Set geometry properties
					geometry.setScale(0.5f);
					geometry.setTranslation(new Vector3d(-60f, -60f, 0f));
					
				}
				else
					MetaioDebug.log(Log.ERROR, "Error loading geometry: "+filepath);
			}
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
