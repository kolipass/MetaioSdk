// Copyright 2007-2014 Metaio GmbH. All rights reserved.
package com.metaio.Template;

import java.io.File;

import android.os.Bundle;
import android.util.Log;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.EVISUAL_SEARCH_STATE;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.IVisualSearchCallback;
import com.metaio.sdk.jni.ImageStruct;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.TrackingValuesVector;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.sdk.jni.VisualSearchResponseVector;
import com.metaio.tools.io.AssetsManager;

public class Template extends ARViewActivity 
{

	private IGeometry mEarth;
	private IGeometry mEarthOcclusion;
	private IGeometry mEarthIndicators;
	private boolean mEarthOpened;
	private MetaioSDKCallbackHandler mSDKCallback;
	private VisualSearchCallbackHandler mVisualSearchCallback;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		mEarthOpened = false;
		
		mSDKCallback = new MetaioSDKCallbackHandler();
		mVisualSearchCallback = new VisualSearchCallbackHandler();
		
		if (metaioSDK != null)
		{	
			metaioSDK.registerVisualSearchCallback(mVisualSearchCallback);
		}
	}

	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		mSDKCallback.delete();
		mSDKCallback = null;
		mVisualSearchCallback.delete();
		mVisualSearchCallback = null;
	}
	
	@Override
	protected int getGUILayout() 
	{
		// Attaching layout to the activity
		return R.layout.template; 
	}
	
	@Override
	protected void loadContents() 
	{
		try
		{
			
			// Getting a file path for tracking configuration XML file
			final File trackingConfigFile = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TrackingData_MarkerlessFast.xml");
			
			// Assigning tracking configuration
			boolean result = metaioSDK.setTrackingConfiguration(trackingConfigFile); 
			MetaioDebug.log("Tracking data loaded: " + result); 
			
			final float scale = 11.f;
			final Rotation rotation = new Rotation(new Vector3d((float)Math.PI/2, 0.0f, 0.0f));
	        
			// Getting a file path for a 3D geometry
			final File earthModel = AssetsManager.getAssetPathAsFile(getApplicationContext(), "Earth.zip");			
			if (earthModel != null) 
			{
				// Loading 3D geometry
				mEarth = metaioSDK.createGeometry(earthModel);
				if (mEarth != null) 
				{
					// Set geometry properties
					mEarth.setScale(scale);
					mEarth.setRotation(rotation);
					
				}
				else
					MetaioDebug.log(Log.ERROR, "Error loading earth geometry: " + mEarth);
			}
			
			final File earthOcclusionModel = AssetsManager.getAssetPathAsFile(getApplicationContext(), "Earth_Occlusion.zip");
			if (earthOcclusionModel != null)
			{
				mEarthOcclusion = metaioSDK.createGeometry(earthOcclusionModel);
				if (mEarthOcclusion != null)
				{
					mEarthOcclusion.setScale(scale);
					mEarthOcclusion.setRotation(rotation);
					mEarthOcclusion.setOcclusionMode(true);
				}
			}
			else
				MetaioDebug.log(Log.ERROR, "Error loading earth occlusion geometry: " + mEarthOcclusion);
			
			final File earthIndicatorsModel = AssetsManager.getAssetPathAsFile(getApplicationContext(), "EarthIndicators.zip");
			if (earthIndicatorsModel != null)
			{
				mEarthIndicators = metaioSDK.createGeometry(earthIndicatorsModel);
				if (mEarthIndicators != null)
				{
					mEarthIndicators.setScale(scale);
					mEarthIndicators.setRotation(rotation);
				}
				else
					MetaioDebug.log(Log.ERROR, "Error loading earth indicator geometry: " + mEarthIndicators);
			}
			
		}       
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "Failed to load content: " + e);
		}
	}
	
  
	@Override
	protected void onGeometryTouched(IGeometry geometry) 
	{
		MetaioDebug.log("Template.onGeometryTouched: " + geometry);
		
		if (geometry != mEarthOcclusion)
		{
			if (!mEarthOpened)
			{
				mEarth.startAnimation("Open", false);
				mEarthIndicators.startAnimation("Grow", false);
				mEarthOpened = true;
			}
			else
			{
				mEarth.startAnimation("Close", false);
				mEarthIndicators.startAnimation("Shrink", false);
				mEarthOpened = false;
			}
		}

	}


	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() 
	{
		return mSDKCallback;
	}
	
	final class MetaioSDKCallbackHandler extends IMetaioSDKCallback 
	{

		@Override
		public void onSDKReady() 
		{
			MetaioDebug.log("The SDK is ready");
		}
		
		@Override
		public void onAnimationEnd(IGeometry geometry, String animationName) 
		{
			MetaioDebug.log("animation ended" + animationName);
		}
		
		@Override
		public void onMovieEnd(IGeometry geometry, File filePath)
		{
			MetaioDebug.log("movie ended" + filePath.getPath());
		}
		
		@Override
		public void onNewCameraFrame(ImageStruct cameraFrame)
		{
			MetaioDebug.log("a new camera frame image is delivered" + cameraFrame.getTimestamp());
		}
		
		@Override 
		public void onCameraImageSaved(File filePath)
		{
			MetaioDebug.log("a new camera frame image is saved to" + filePath.getPath());
		}
		
		@Override
		public void onScreenshotImage(ImageStruct image)
		{
			MetaioDebug.log("screenshot image is received" + image.getTimestamp());
		}
		
		@Override
		public void onScreenshotSaved(File filePath)
		{
			MetaioDebug.log("screenshot image is saved to" + filePath.getPath());
		}
		
		@Override
		public void onTrackingEvent(TrackingValuesVector trackingValues)
		{
			for (int i=0; i<trackingValues.size(); i++)
			{
				final TrackingValues v = trackingValues.get(i);
				MetaioDebug.log("Tracking state for COS "+v.getCoordinateSystemID()+" is "+v.getState());
			}
		}

		@Override
		public void onInstantTrackingEvent(boolean success, File filePath)
		{
			if (success)
			{
				MetaioDebug.log("Instant 3D tracking is successful");
			}
		}
	}
	
	final class VisualSearchCallbackHandler extends IVisualSearchCallback
	{
		@Override
		public void onVisualSearchResult(VisualSearchResponseVector response, int errorCode)
		{
			if (errorCode == 0)
			{
				MetaioDebug.log("Visual search is successful");
			}
		}

		@Override
		public void onVisualSearchStatusChanged(EVISUAL_SEARCH_STATE state) 
		{
			MetaioDebug.log("The current visual search state is: " + state);
		}
	}
}
