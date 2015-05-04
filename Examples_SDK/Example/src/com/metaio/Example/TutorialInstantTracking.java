// Copyright 2007-2014 metaio GmbH. All rights reserved.
package com.metaio.Example;

import java.io.File;
import java.io.FileInputStream;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.tools.io.AssetsManager;

public class TutorialInstantTracking extends ARViewActivity
{

	/**
	 * Tiger geometry
	 */
	private IGeometry mTiger;

	/**
	 * metaio SDK callback handler
	 */
	private MetaioSDKCallbackHandler mCallbackHandler;

	/**
	 * Flag to indicate proximity to the tiger
	 */
	boolean mIsCloseToTiger;

	/**
	 * Media Player to play the sound of the tiger
	 */
	MediaPlayer mMediaPlayer;

	private View m2DButton;
	private View m2DRectifiedButton;
	private View m3DButton;
	private View m2DSLAMButton;
	private View m2DSLAMExtrapolationButton;

	/**
	 * The flag indicating a mode of instant tracking
	 *
	 * @see {@link IMetaioSDKAndroid#startInstantTracking(String, String, boolean)}
	 */
	boolean mPreview = true;

	/**
	 * Whether to set tracking configuration on onInstantTrackingEvent
	 */
	boolean mMustUseInstantTrackingEvent = false;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		try
		{
			mTiger = null;
			mCallbackHandler = new MetaioSDKCallbackHandler();
			mIsCloseToTiger = false;
			mMediaPlayer = new MediaPlayer();
			FileInputStream fis =
					new FileInputStream(AssetsManager.getAssetPathAsFile(getApplicationContext(),
							"TutorialInstantTracking/Assets/meow.mp3"));
			mMediaPlayer.setDataSource(fis.getFD());
			mMediaPlayer.prepare();
			fis.close();

			m2DButton = mGUIView.findViewById(R.id.instant2DButton);
			m2DRectifiedButton = mGUIView.findViewById(R.id.instant2DRectifiedButton);
			m3DButton = mGUIView.findViewById(R.id.instant3DButton);
			m2DSLAMButton = mGUIView.findViewById(R.id.instant2DSLAMButton);
			m2DSLAMExtrapolationButton = mGUIView.findViewById(R.id.instant2DSLAMExtrapolationButton);
		}
		catch (Exception e)
		{
			mMediaPlayer = null;
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		mCallbackHandler.delete();
		mCallbackHandler = null;
		try
		{
			mMediaPlayer.release();
		}
		catch (Exception e)
		{
		}
	}

	/**
	 * This method is regularly called in the rendering loop. It calculates the distance between
	 * device and the target and performs actions based on the proximity
	 */
	private void checkDistanceToTarget()
	{
		// get tracking values for COS 1
		TrackingValues tv = metaioSDK.getTrackingValues(1);

		// Note, you can use this mechanism also to detect if something is tracking or not.
		// (e.g. for triggering an action as soon as some target is visible on screen)
		if (tv.isTrackingState())
		{
			// calculate the distance as sqrt( x^2 + y^2 + z^2 )
			final float distance = tv.getTranslation().norm();

			// define a threshold distance
			final float threshold = 200;

			// moved close to the tiger
			if (distance < threshold)
			{
				// if not already close to the model
				if (!mIsCloseToTiger)
				{
					MetaioDebug.log("Moved close to the tiger");
					mIsCloseToTiger = true;
					playSound();
					mTiger.startAnimation("tap");
				}
			}
			else
			{
				if (mIsCloseToTiger)
				{
					MetaioDebug.log("Moved away from the tiger");
					mIsCloseToTiger = false;
				}
			}

		}
	}

	/**
	 * Play sound that has been loaded
	 */
	private void playSound()
	{
		try
		{
			MetaioDebug.log("Playing sound");
			mMediaPlayer.start();
		}
		catch (Exception e)
		{
			MetaioDebug.log("Error playing sound: " + e.getMessage());
		}
	}


	@Override
	protected int getGUILayout()
	{
		return R.layout.tutorial_instant_tracking;
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

		checkDistanceToTarget();

	}

	public void onButtonClick(View v)
	{
		finish();
	}

	public void on2DButtonClicked(View v)
	{
		mMustUseInstantTrackingEvent = true;
		mTiger.setVisible(false);
		m2DRectifiedButton.setEnabled(!mPreview);
		m3DButton.setEnabled(!mPreview);
		m2DSLAMButton.setEnabled(!mPreview);
		m2DSLAMExtrapolationButton.setEnabled(!mPreview);
		metaioSDK.startInstantTracking("INSTANT_2D", new File(""), mPreview);
		mPreview = !mPreview;
	}

	public void on2DRectifiedButtonClicked(View v)
	{
		mMustUseInstantTrackingEvent = true;
		mTiger.setVisible(false);
		m2DButton.setEnabled(!mPreview);
		m3DButton.setEnabled(!mPreview);
		m2DSLAMButton.setEnabled(!mPreview);
		m2DSLAMExtrapolationButton.setEnabled(!mPreview);
		metaioSDK.startInstantTracking("INSTANT_2D_GRAVITY", new File(""), mPreview);
		mPreview = !mPreview;
	}

	public void on3DButtonClicked(View v)
	{
		mMustUseInstantTrackingEvent = false;
		mTiger.setVisible(false);
		metaioSDK.startInstantTracking("INSTANT_3D");
	}

	public void on2DSLAMButtonClicked(View v)
	{
		mMustUseInstantTrackingEvent = true;
		mTiger.setVisible(false);
		m2DButton.setEnabled(!mPreview);
		m2DRectifiedButton.setEnabled(!mPreview);
		m3DButton.setEnabled(!mPreview);
		m2DSLAMExtrapolationButton.setEnabled(!mPreview);
		metaioSDK.startInstantTracking("INSTANT_2D_GRAVITY_SLAM", new File(""), mPreview);
		mPreview = !mPreview;
	}

	public void on2DSLAMExtrapolationButtonClicked(View v)
	{
		mMustUseInstantTrackingEvent = true;
		mTiger.setVisible(false);
		m2DButton.setEnabled(!mPreview);
		m2DRectifiedButton.setEnabled(!mPreview);
		m3DButton.setEnabled(!mPreview);
		m2DSLAMButton.setEnabled(!mPreview);
		metaioSDK.startInstantTracking("INSTANT_2D_GRAVITY_SLAM_EXTRAPOLATED", new File(""), mPreview);
		mPreview = !mPreview;
	}

	@Override
	protected void loadContents()
	{
		try
		{
			// Load tiger model
			final File tigerModelPath =
					AssetsManager.getAssetPathAsFile(getApplicationContext(), "TutorialInstantTracking/Assets/tiger.md2");
			mTiger = metaioSDK.createGeometry(tigerModelPath);

			// Set geometry properties and initially hide it
			mTiger.setScale(8f);
			mTiger.setRotation(new Rotation(0f, 0f, (float)Math.PI));
			mTiger.setVisible(false);
			mTiger.setAnimationSpeed(60f);
			mTiger.startAnimation("meow");
			MetaioDebug.log("Loaded geometry " + tigerModelPath);
		}
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "Error loading geometry: " + e.getMessage());
		}
	}


	@Override
	protected void onGeometryTouched(IGeometry geometry)
	{
		playSound();
		geometry.startAnimation("tap");
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
		public void onInstantTrackingEvent(boolean success, File filePath)
		{
			if (success)
			{
				// Since SDK 6.0, INSTANT_3D doesn't create a new tracking configuration anymore
				// (see changelog)
				if (mMustUseInstantTrackingEvent)
				{
					MetaioDebug.log("MetaioSDKCallbackHandler.onInstantTrackingEvent: " + filePath.getPath());
					metaioSDK.setTrackingConfiguration(filePath);
				}

				mTiger.setVisible(true);
			}
			else
			{
				MetaioDebug.log(Log.ERROR, "Failed to create instant tracking configuration!");
			}
		}

		@Override
		public void onAnimationEnd(IGeometry geometry, String animationName)
		{
			// Play a random animation from the list
			final String[] animations = {"meow", "scratch", "look", "shake", "clean"};
			final int random = (int)(Math.random() * animations.length);
			geometry.startAnimation(animations[random]);
		}
	}

}
