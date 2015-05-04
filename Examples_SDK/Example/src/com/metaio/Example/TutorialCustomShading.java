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
import com.metaio.sdk.jni.IShaderMaterialOnSetConstantsCallback;
import com.metaio.sdk.jni.IShaderMaterialSetConstantsService;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.SWIGTYPE_p_void;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.io.AssetsManager;

public class TutorialCustomShading extends ARViewActivity
{
	private IGeometry mModel;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mModel = null;
	}

	@Override
	protected int getGUILayout()
	{
		return R.layout.tutorial_custom_shading;
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
			final File trackingConfigFile = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialCustomShading/Assets/TrackingData_MarkerlessFast.xml");

			final boolean result = metaioSDK.setTrackingConfiguration(trackingConfigFile);
			MetaioDebug.log("Tracking configuration loaded: " + result);

			final File modelPath = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialCustomShading/Assets/metaioman.md2");
			if (modelPath != null)
			{
				mModel = metaioSDK.createGeometry(modelPath);
				if (mModel != null)
				{
					MetaioDebug.log("Loaded geometry " + modelPath);

					mModel.startAnimation("idle", true);

					// Make him look away from the pattern
					mModel.setScale(2);
					mModel.setRotation(new Rotation((float)(-Math.PI/2.0), 0, 0));
					mModel.setTranslation(new Vector3d(0, -100, 50));
				}
				else
					MetaioDebug.log(Log.ERROR, "Error loading geometry: " + modelPath);
			}
			else
				MetaioDebug.log(Log.ERROR, "Model not found");

			final File shaderMaterialsFile = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialCustomShading/Assets/shader_materials.xml");
			if (shaderMaterialsFile == null || !metaioSDK.loadShaderMaterials(shaderMaterialsFile))
			{
				MetaioDebug.log(Log.ERROR, "Failed to load shader material");

				if (mModel != null)
					mModel.setVisible(false);
			}
			else
			{
				if (mModel != null)
					mModel.setShaderMaterial("tutorial_customshading");
			}

			if (mModel != null)
				mModel.setShaderMaterialOnSetConstantsCallback(new IShaderMaterialOnSetConstantsCallback() {
					@Override
					public void onSetShaderMaterialConstants(String shaderMaterialName, SWIGTYPE_p_void extra, IShaderMaterialSetConstantsService constantsService)
					{
						// We just pass the positive sinus (range [0;1]) of absolute time in seconds so that we can
						// use it to fade our effect in and out.

						final float time[] = new float[] { 0.5f * (1.0f + (float)Math.sin(System.currentTimeMillis() / 1000.0)) };
						constantsService.setShaderUniformF("myValue", time, 1);
					}
				});
		}
		catch (Exception e)
		{
			e.printStackTrace();
			MetaioDebug.log(Log.ERROR, "loadContents failed, see stack trace");
		}
	}

	@Override
	protected void onGeometryTouched(IGeometry geometry)
	{
	}
}
