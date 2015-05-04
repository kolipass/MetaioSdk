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

public class TutorialDynamicModels extends ARViewActivity 
{

	private MetaioSDKCallbackHandler mCallbackHandler;
	private IGeometry mMetaioMan;

	private boolean mIsCloseToModel;

	/* This method is regularly called, calculates the distance between phone and target
	 * and performs actions based on the distance 
	 */
	private void checkDistanceToTarget() 
	{
		
		// get tracing values for COS 1
		final TrackingValues tv = metaioSDK.getTrackingValues(1);
		
		// check if the current state is tracking
		// Note, you can use this mechanism also to detect if something is tracking or not.
		// (e.g. for triggering an action as soon as some target is visible on screen)
		if (tv.isTrackingState())
		{
			// get the translation part of the pose
			final Vector3d translation = tv.getTranslation();
			
			// calculate the distance as sqrt( x^2 + y^2 + z^2 )
			final float distanceToTarget = translation.norm();
			
			// define a threshold distance
			final float threshold = 800;
			
			// if we are already close to the model
			if (mIsCloseToModel)
			{
				// if our distance is larger than our threshold (+ a little)
				if (distanceToTarget > (threshold + 10))
				{
					// we flip this variable again
					mIsCloseToModel = false;
					// and start the close_up animation
					mMetaioMan.startAnimation("close_up", false);
				}
			}
			else 
			{
				// we're not close yet, let's check if we are now
				if (distanceToTarget < threshold)
				{
					// flip the variable
					mIsCloseToModel = true;
					// and start an animation
					mMetaioMan.startAnimation("close_down", false);
				}
			}
			
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		mCallbackHandler = new MetaioSDKCallbackHandler();
		mIsCloseToModel = false;
				
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
		// TODO: return 0 in case of no GUI overlay
		return R.layout.tutorial_dynamic_models; 
	}


	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() 
	{
		return mCallbackHandler;
	}
	

	@Override
	public void onDrawFrame() 
	{
		super.onDrawFrame();
		
		try
		{
			checkDistanceToTarget();
		}
		catch (Exception e)
		{
		}
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
			
			// TODO: Load desired tracking data for planar marker tracking
			File filepath = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialDynamicModels/Assets/TrackingData_MarkerlessFast.xml");
			
			boolean result = metaioSDK.setTrackingConfiguration(filepath); 
			MetaioDebug.log("Tracking data loaded: " + result); 
	        
			// Load all the geometries
			filepath = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialDynamicModels/Assets/metaioman.md2");			
			if (filepath != null) 
			{
				mMetaioMan = metaioSDK.createGeometry(filepath);
				if (mMetaioMan != null) 
				{
					// Set geometry properties
					mMetaioMan.setScale(4f);
					
					// Start first animation
					mMetaioMan.startAnimation("idle", true);					 
					
				}
				else
					MetaioDebug.log(Log.ERROR, "Error loading geometry: "+filepath);
			}
			
		}       
		catch (Exception e)
		{
			
		}
	}
	
	@Override
	protected void onGeometryTouched(final IGeometry geometry) 
	{
		MetaioDebug.log("UnifeyeCallbackHandler.onGeometryTouched: "+geometry);
		
		geometry.startAnimation("shock_down", false);
		
	}
	  
	final class MetaioSDKCallbackHandler extends IMetaioSDKCallback
	{
	
		@Override
		public void onAnimationEnd(IGeometry geometry, String animationName) 
		{
			MetaioDebug.log("UnifeyeCallbackHandler.onAnimationEnd: "+animationName);
			if (animationName.equalsIgnoreCase("shock_down"))
			{
				geometry.startAnimation("shock_idle", false);
			}
			else if (animationName.equalsIgnoreCase("shock_idle"))
			{
				geometry.startAnimation("shock_up", false);
			}
			else if (animationName.equalsIgnoreCase("shock_up") || animationName.equalsIgnoreCase("close_up"))
			{
				if (mIsCloseToModel)
					geometry.startAnimation("close_idle", true);
				else
					geometry.startAnimation("idle", true);
			}
			else if (animationName.equalsIgnoreCase("close_down"))
				geometry.startAnimation("close_idle", true);

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

	}
	
}
