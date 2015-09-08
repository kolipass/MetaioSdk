// Copyright 2007-2014 metaio GmbH. All rights reserved.
package com.metaio.Example;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;

import com.metaio.cloud.plugin.util.MetaioCloudUtils;
import com.metaio.sdk.ARELInterpreterAndroidJava;
import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.AnnotatedGeometriesGroupCallback;
import com.metaio.sdk.jni.EGEOMETRY_FOCUS_STATE;
import com.metaio.sdk.jni.IAnnotatedGeometriesGroup;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.IRadar;
import com.metaio.sdk.jni.ImageStruct;
import com.metaio.sdk.jni.LLACoordinate;
import com.metaio.sdk.jni.Rotation;
import com.metaio.tools.io.AssetsManager;

public class TutorialLocationBasedAR extends ARViewActivity
{
	private IAnnotatedGeometriesGroup mAnnotatedGeometriesGroup;

	private MyAnnotatedGeometriesGroupCallback mAnnotatedGeometriesGroupCallback;

	/**
	 * List of geometries
	 */
	private ArrayList<IGeometry> mGeometries;

	/**
	 * Radar object
	 */
	private IRadar mRadar;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Set GPS-Compass tracking configuration
		final boolean result = metaioSDK.setTrackingConfiguration("GPS", false);
		MetaioDebug.log("Tracking data loaded: " + result);
	}

	@Override
	protected void onDestroy()
	{
		// Break circular reference of Java objects
		if (mAnnotatedGeometriesGroup != null)
		{
			mAnnotatedGeometriesGroup.registerCallback(null);
		}

		if (mAnnotatedGeometriesGroupCallback != null)
		{
			mAnnotatedGeometriesGroupCallback.delete();
			mAnnotatedGeometriesGroupCallback = null;
		}

		super.onDestroy();
	}

	@Override
	public void onDrawFrame()
	{
		try
		{
			// Set rotation of the geometries to always face user
			final float heading = (float) Math.toRadians(mSensors.getHeading());
			
			Rotation rototation = new Rotation((float)(Math.PI/2.0), 0f, -heading);
			for (IGeometry geometry : mGeometries)
			{
				geometry.setRotation(rototation);
			}
			rototation.delete();
			rototation = null;
			
		}
		catch (Exception e)
		{
		}
		
		super.onDrawFrame();
	}

	public void onButtonClick(View v)
	{
		finish();
	}

	@Override
	protected int getGUILayout()
	{
		return R.layout.tutorial_location_based_ar;
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler()
	{
		return null;
	}

	@Override
	protected void loadContents()
	{
		mAnnotatedGeometriesGroup = metaioSDK.createAnnotatedGeometriesGroup();
		mAnnotatedGeometriesGroupCallback = new MyAnnotatedGeometriesGroupCallback();
		mAnnotatedGeometriesGroup.registerCallback(mAnnotatedGeometriesGroupCallback);

		// Clamp geometries' Z position to range [5000;200000] no matter how close or far they are
		// away.
		// This influences minimum and maximum scaling of the geometries (easier for development).
		metaioSDK.setLLAObjectRenderingLimits(5, 200);

		// Set render frustum accordingly
		metaioSDK.setRendererClippingPlaneLimits(10, 220000);

		// let's create LLA objects for known cities
		final LLACoordinate MUNICH = new LLACoordinate(48.142573, 11.550321, 0, 0);
		final LLACoordinate LONDON = new LLACoordinate(51.50661, -0.130463, 0, 0);
		final LLACoordinate TOKYO = new LLACoordinate(35.657464, 139.773865, 0, 0);
		final LLACoordinate ROME = new LLACoordinate(41.90177, 12.45987, 0, 0);
		final LLACoordinate PARIS = new LLACoordinate(48.85658, 2.348671, 0, 0);
		mGeometries = new ArrayList<IGeometry>();
		
		// Load some POIs. Each of them has the same shape at its geoposition. We pass a string
		// (const char*) to IAnnotatedGeometriesGroup::addGeometry so that we can use it as POI
		// title
		// in the callback, in order to create an annotation image with the title on it.
		IGeometry geometry = createPOIGeometry(LONDON);
		mAnnotatedGeometriesGroup.addGeometry(geometry, "London");
		mGeometries.add(geometry);

		geometry = createPOIGeometry(PARIS);
		mAnnotatedGeometriesGroup.addGeometry(geometry, "Paris");
		mGeometries.add(geometry);

		geometry = createPOIGeometry(ROME);
		mAnnotatedGeometriesGroup.addGeometry(geometry, "Rome");
		mGeometries.add(geometry);

		geometry = createPOIGeometry(TOKYO);
		mAnnotatedGeometriesGroup.addGeometry(geometry, "Tokyo");
		mGeometries.add(geometry);

		File metaioManModel =
				AssetsManager.getAssetPathAsFile(getApplicationContext(),
						"TutorialLocationBasedAR/Assets/metaioman.md2");
		if (metaioManModel != null)
		{
			geometry = metaioSDK.createGeometry(metaioManModel);
			if (geometry != null)
			{
				geometry.setTranslationLLA(MUNICH);
				geometry.setLLALimitsEnabled(true);
				geometry.setScale(500f);
			}
			else
			{
				MetaioDebug.log(Log.ERROR, "Error loading geometry: " + metaioManModel);
			}
		}

		// create radar
		mRadar = metaioSDK.createRadar();
		mRadar.setBackgroundTexture(AssetsManager.getAssetPathAsFile(getApplicationContext(),
				"TutorialLocationBasedAR/Assets/radar.png"));
		mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPathAsFile(getApplicationContext(),
				"TutorialLocationBasedAR/Assets/yellow.png"));
		mRadar.setRelativeToScreen(IGeometry.ANCHOR_TL);

		// add geometries to the radar
		mRadar.add(geometry);
		for (IGeometry g:mGeometries)
		{
			mRadar.add(g);
		}
	}

	private IGeometry createPOIGeometry(LLACoordinate lla)
	{
		final File path =
				AssetsManager.getAssetPathAsFile(getApplicationContext(),
						"TutorialLocationBasedAR/Assets/ExamplePOI.obj");
		if (path != null)
		{
			IGeometry geometry = metaioSDK.createGeometry(path);
			geometry.setTranslationLLA(lla);
			geometry.setLLALimitsEnabled(true);
			geometry.setScale(100f);
			return geometry;
		}
		else
		{
			MetaioDebug.log(Log.ERROR, "Missing files for POI geometry");
			return null;
		}
	}

	@Override
	protected void onGeometryTouched(final IGeometry geometry)
	{
		MetaioDebug.log("Geometry selected: " + geometry);

		mSurfaceView.queueEvent(new Runnable()
		{

			@Override
			public void run()
			{
				mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPathAsFile(getApplicationContext(),
						"TutorialLocationBasedAR/Assets/yellow.png"));
				mRadar.setObjectTexture(geometry, AssetsManager.getAssetPathAsFile(getApplicationContext(),
						"TutorialLocationBasedAR/Assets/red.png"));
				mAnnotatedGeometriesGroup.setSelectedGeometry(geometry);
			}
		});
	}

	final class MyAnnotatedGeometriesGroupCallback extends AnnotatedGeometriesGroupCallback
	{
		Bitmap mAnnotationBackground, mEmptyStarImage, mFullStarImage;
		int mAnnotationBackgroundIndex;
		ImageStruct texture;
		String[] textureHash = new String[1];
		TextPaint mPaint;
		Lock geometryLock;


		Bitmap inOutCachedBitmaps[] = new Bitmap[] {mAnnotationBackground, mEmptyStarImage, mFullStarImage};
		int inOutCachedAnnotationBackgroundIndex[] = new int[] {mAnnotationBackgroundIndex};

		public MyAnnotatedGeometriesGroupCallback()
		{
			mPaint = new TextPaint();
			mPaint.setFilterBitmap(true); // enable dithering
			mPaint.setAntiAlias(true); // enable anti-aliasing
		}

		@Override
		public IGeometry loadUpdatedAnnotation(IGeometry geometry, Object userData, IGeometry existingAnnotation)
		{
			if (userData == null)
			{
				return null;
			}

			if (existingAnnotation != null)
			{
				// We don't update the annotation if e.g. distance has changed
				return existingAnnotation;
			}

			String title = (String)userData; // as passed to addGeometry
			LLACoordinate location = geometry.getTranslationLLA();
			float distance = (float)MetaioCloudUtils.getDistanceBetweenTwoCoordinates(location, mSensors.getLocation());
			Bitmap thumbnail = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
			try
			{
				texture =
						ARELInterpreterAndroidJava.getAnnotationImageForPOI(title, title, distance, "5", thumbnail,
								null,
								metaioSDK.getRenderSize(), TutorialLocationBasedAR.this,
								mPaint, inOutCachedBitmaps, inOutCachedAnnotationBackgroundIndex, textureHash);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (thumbnail != null)
					thumbnail.recycle();
				thumbnail = null;
			}

			mAnnotationBackground = inOutCachedBitmaps[0];
			mEmptyStarImage = inOutCachedBitmaps[1];
			mFullStarImage = inOutCachedBitmaps[2];
			mAnnotationBackgroundIndex = inOutCachedAnnotationBackgroundIndex[0];

			IGeometry resultGeometry = null;

			if (texture != null)
			{
				if (geometryLock != null)
				{
					geometryLock.lock();
				}

				try
				{
					// Use texture "hash" to ensure that SDK loads new texture if texture changed
					resultGeometry = metaioSDK.createGeometryFromImage(textureHash[0], texture, true, false);
				}
				finally
				{
					if (geometryLock != null)
					{
						geometryLock.unlock();
					}
				}
			}

			return resultGeometry;
		}

		@Override
		public void onFocusStateChanged(IGeometry geometry, Object userData, EGEOMETRY_FOCUS_STATE oldState,
				EGEOMETRY_FOCUS_STATE newState)
		{
			MetaioDebug.log("onFocusStateChanged for " + (String)userData + ", " + oldState + "->" + newState);
		}
	}
}
