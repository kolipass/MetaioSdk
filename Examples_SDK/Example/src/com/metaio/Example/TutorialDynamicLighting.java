// Copyright 2007-2014 metaio GmbH. All rights reserved.
package com.metaio.Example;

import java.io.File;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.ELIGHT_TYPE;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.ILight;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.io.AssetsManager;

public class TutorialDynamicLighting extends ARViewActivity
{
	private ILight mDirectionalLight;

	private IGeometry mDirectionalLightGeo;

	private IGeometry mModel;

	private ILight mPointLight;

	private IGeometry mPointLightGeo;

	private ILight mSpotLight;

	private IGeometry mSpotLightGeo;

	private IGeometry createLightGeometry()
	{
		final File modelPath = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialDynamicLighting/Assets/sphere_10mm.obj");
		if (modelPath != null)
		{
			IGeometry model = metaioSDK.createGeometry(modelPath);
			if (model != null)
				return model;
			else
				MetaioDebug.log(Log.ERROR, "Error loading geometry: " + modelPath);
		}
		else
			MetaioDebug.log(Log.ERROR, "Could not find 3D model to use as light indicator");

		return null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mModel = null;
	}

	@Override
	protected int getGUILayout()
	{
		return R.layout.tutorial_dynamic_lighting;
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler()
	{
		return null;
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
			final File trackingConfigFile = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialDynamicLighting/Assets/TrackingData_MarkerlessFast.xml");

			final boolean result = metaioSDK.setTrackingConfiguration(trackingConfigFile);
			MetaioDebug.log("Tracking configuration loaded: " + result);

			final File modelPath = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialDynamicLighting/Assets/cube_50mm.obj");
			if (modelPath != null)
			{
				mModel = metaioSDK.createGeometry(modelPath);
				if (mModel != null)
					MetaioDebug.log("Loaded geometry " + modelPath);
				else
					MetaioDebug.log(Log.ERROR, "Error loading geometry: " + modelPath);
			}
			else
				MetaioDebug.log(Log.ERROR, "Model not found");

			metaioSDK.setAmbientLight(new Vector3d(0.05f));

			mDirectionalLight = metaioSDK.createLight();
			mDirectionalLight.setType(ELIGHT_TYPE.ELIGHT_TYPE_DIRECTIONAL);
			mDirectionalLight.setAmbientColor(new Vector3d(0, 0.15f, 0)); // slightly green
			mDirectionalLight.setDiffuseColor(new Vector3d(0.6f, 0.2f, 0)); // orange
			mDirectionalLight.setCoordinateSystemID(1);
			mDirectionalLightGeo = createLightGeometry();
			mDirectionalLightGeo.setCoordinateSystemID(mDirectionalLight.getCoordinateSystemID());
			mDirectionalLightGeo.setDynamicLightingEnabled(false);

			mPointLight = metaioSDK.createLight();
			mPointLight.setType(ELIGHT_TYPE.ELIGHT_TYPE_POINT);
			mPointLight.setAmbientColor(new Vector3d(0, 0, 0.15f)); // slightly blue ambient
			mPointLight.setAttenuation(new Vector3d(0, 0, 40));
			mPointLight.setDiffuseColor(new Vector3d(0, 0.8f, 0.05f)); // green-ish
			mPointLight.setCoordinateSystemID(1);
			mPointLightGeo = createLightGeometry();
			mPointLightGeo.setCoordinateSystemID(mPointLight.getCoordinateSystemID());
			mPointLightGeo.setDynamicLightingEnabled(false);

			mSpotLight = metaioSDK.createLight();
			mSpotLight.setAmbientColor(new Vector3d(0.17f, 0, 0)); // slightly red ambient
			mSpotLight.setType(ELIGHT_TYPE.ELIGHT_TYPE_SPOT);
			mSpotLight.setRadiusDegrees(10);
			mSpotLight.setDiffuseColor(new Vector3d(1, 1, 0)); // yellow
			mSpotLight.setCoordinateSystemID(1);
			mSpotLightGeo = createLightGeometry();
			mSpotLightGeo.setCoordinateSystemID(mSpotLight.getCoordinateSystemID());
			mSpotLightGeo.setDynamicLightingEnabled(false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			MetaioDebug.log(Log.ERROR, "loadContents failed, see stack trace");
		}
	}

	@Override
	public void onDrawFrame()
	{
		super.onDrawFrame();

		// If content not loaded yet, do nothing
		if (mDirectionalLight == null)
			return;

		// Lights circle around
		final double time = System.currentTimeMillis() / 1000.0;
		final Vector3d lightPos = new Vector3d(
			200.0f * (float)Math.cos(time),
			120.0f * (float)Math.sin(0.25f*time),
			200.0f * (float)Math.sin(time));

		final float FREQ2MUL = 0.4f;
		final Vector3d lightPos2 = new Vector3d(
						150.0f * (float)(Math.cos(FREQ2MUL*2.2*time) * (1 + 2+2*Math.sin(FREQ2MUL*0.6*time))),
						30.0f * (float)Math.sin(FREQ2MUL*0.35*time),
						150.0f * (float)Math.sin(FREQ2MUL*2.2*time));

		final Vector3d directionalLightDir = new Vector3d(
						(float)Math.cos(1.2*time),
						(float)Math.sin(0.25*time),
						(float)Math.sin(0.8*time));

		// This will only apply in the upcoming frame:

		// Directional light
		mDirectionalLight.setDirection(directionalLightDir);
		updateLightIndicator(mDirectionalLightGeo, mDirectionalLight);

		// Point light
		mPointLight.setTranslation(lightPos);
		updateLightIndicator(mPointLightGeo, mPointLight);

		// Spot light
		mSpotLight.setTranslation(lightPos2);
		// Spot towards origin of COS
		mSpotLight.setDirection(new Vector3d(-lightPos2.getX(), -lightPos2.getY(), -lightPos2.getZ()));
		updateLightIndicator(mSpotLightGeo, mSpotLight);
	}

	@Override
	protected void onGeometryTouched(IGeometry geometry)
	{
	}

	private void updateLightIndicator(IGeometry indicatorGeo, ILight light)
	{
		indicatorGeo.setVisible(light.isEnabled());

		if (!light.isEnabled())
			return;

		if (light.getType() == ELIGHT_TYPE.ELIGHT_TYPE_DIRECTIONAL)
		{
			final Vector3d dir = light.getDirection();
			final float norm = dir.norm();

			// Indicate "source" of directional light (not really the source because it's infinite)
			indicatorGeo.setTranslation(new Vector3d(-200.0f * dir.getX() / norm, -200.0f * dir.getY() / norm, -200.0f * dir.getZ() / norm));
		}
		else
			indicatorGeo.setTranslation(light.getTranslation());
	}
}
