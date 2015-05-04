// Copyright 2007-2014 metaio GmbH. All rights reserved.
package com.metaio.Example;

import java.io.File;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.TrackingValuesVector;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.io.AssetsManager;

public class TutorialTrackingSamples extends ARViewActivity 
{

	/**
	 * Reference to loaded metaioman geometry
	 */
	private IGeometry mMetaioMan;

	/**
	 * Currently loaded tracking configuration file
	 */
	File trackingConfigFile;
	
	private MetaioSDKCallbackHandler mCallbackHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		mCallbackHandler = new MetaioSDKCallbackHandler();
	}
	
	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		mCallbackHandler.delete();
		mCallbackHandler = null;
	}
	
	@Override
	protected int getGUILayout() 
	{
		return R.layout.tutorial_tracking_samples; 
	}

	public void onButtonClick(View v)
	{
		finish();
	}
	
	public void onIdButtonClick(View v)
	{
		trackingConfigFile = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialTrackingSamples/Assets/TrackingData_Marker.xml");
		MetaioDebug.log("Tracking Config path = "+trackingConfigFile);
		
		boolean result = metaioSDK.setTrackingConfiguration(trackingConfigFile); 
		MetaioDebug.log("Id Marker tracking data loaded: " + result); 
		mMetaioMan.setScale(new Vector3d(2f, 2f, 2f));
	}
	
	public void onPictureButtonClick(View v)
	{
		trackingConfigFile = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialTrackingSamples/Assets/TrackingData_PictureMarker.xml");
		MetaioDebug.log("Tracking Config path = "+trackingConfigFile);
		
		boolean result = metaioSDK.setTrackingConfiguration(trackingConfigFile); 
		MetaioDebug.log("Picture Marker tracking data loaded: " + result); 
		mMetaioMan.setScale(new Vector3d(8f, 8f, 8f));

	}
	
	public void onMarkerlessButtonClick(View v)
	{
		trackingConfigFile = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialTrackingSamples/Assets/TrackingData_MarkerlessFast.xml");
		MetaioDebug.log("Tracking Config path = "+trackingConfigFile);
	
		boolean result = metaioSDK.setTrackingConfiguration(trackingConfigFile); 
		MetaioDebug.log("Markerless tracking data loaded: " + result); 
		mMetaioMan.setScale(new Vector3d(4f, 4f, 4f));
	}
	
	@Override
	protected void loadContents() 
	{
		try
		{
			
			// Load the desired tracking configuration
			trackingConfigFile = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialTrackingSamples/Assets/TrackingData_MarkerlessFast.xml");
			final boolean result = metaioSDK.setTrackingConfiguration(trackingConfigFile); 
			MetaioDebug.log("Tracking configuration loaded: " + result); 
	        
			// Load all the geometries. First - Model
			final File metaioManModel = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialTrackingSamples/Assets/metaioman.md2");			
			if (metaioManModel != null) 
			{
				mMetaioMan = metaioSDK.createGeometry(metaioManModel);
				if (mMetaioMan != null) 
				{
					// Set geometry properties
					mMetaioMan.setScale(4f);
					MetaioDebug.log("Loaded geometry "+metaioManModel);
				}
				else
					MetaioDebug.log(Log.ERROR, "Error loading geometry: "+metaioManModel);
			}
			
		
		}       
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "Error loading contents!");
			MetaioDebug.printStackTrace(Log.ERROR, e);
		}
	}
	
  
	@Override
	protected void onGeometryTouched(IGeometry geometry) 
	{
		// TODO Auto-generated method stub	
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() 
	{
		return mCallbackHandler;
	}
	
	final class MetaioSDKCallbackHandler extends IMetaioSDKCallback 
	{

		@Override
		public void onSDKReady() 
		{
			// show GUI
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					mGUIView.setVisibility(View.VISIBLE);
				}
			});
		}
		
		@Override
		public void onTrackingEvent(TrackingValuesVector trackingValues)
		{
			// if we detect any target, we bind the loaded geometry to this target
			if(mMetaioMan != null)
			{
				for (int i=0; i<trackingValues.size(); i++)
				{
					final TrackingValues tv = trackingValues.get(i);
					if (tv.isTrackingState())
					{
						mMetaioMan.setCoordinateSystemID(tv.getCoordinateSystemID());
						break;
					}
				}
			}
			
		}
	}
	
}
