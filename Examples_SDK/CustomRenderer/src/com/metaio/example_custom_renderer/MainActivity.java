// Copyright 2007-2014 metaio GmbH. All rights reserved.
package com.metaio.example_custom_renderer;

import java.io.File;
import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.SensorsComponentAndroid;
import com.metaio.sdk.jni.CameraVector;
import com.metaio.sdk.jni.ECAMERA_TYPE;
import com.metaio.sdk.jni.ERENDER_SYSTEM;
import com.metaio.sdk.jni.ESCREEN_ROTATION;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.ImageStruct;
import com.metaio.sdk.jni.MetaioSDK;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.TrackingValuesVector;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.Screen;
import com.metaio.tools.SystemInfo;
import com.metaio.tools.io.AssetsManager;

public final class MainActivity extends Activity implements Renderer
{

	/**
	 * Defines whether the activity is currently paused
	 */
	private boolean mActivityIsPaused;

	/**
	 * Camera image renderer which takes care of differences in camera image and viewport aspect
	 * ratios
	 */
	private CameraImageRenderer mCameraImageRenderer;

	/**
	 * Simple cube that is rendered on top of the target pattern
	 */
	private Cube mCube;

	/**
	 * metaio SDK instance
	 */
	private IMetaioSDKAndroid metaioSDK;

	/**
	 * metaio SDK callback handler
	 */
	private MetaioSDKCallbackHandler mSDKCallback;

	/**
	 * Whether the metaio SDK null renderer is initialized
	 */
	private boolean mRendererInitialized;

	/**
	 * Current screen rotation
	 */
	private ESCREEN_ROTATION mScreenRotation;

	/**
	 * Whether see-through is enabled. Note that with stereo see-through rendering, display the
	 * camera image does not make sense because the virtual content will be aligned to the real
	 * world, not to the camera image. This is mainly there for debugging purposes, e.g. to check if
	 * the camera is working. For see-through glasses, you will always have this set to true.
	 */
	private final boolean mSeeThrough = false;

	/**
	 * Whether stereo rendering should be used (by splitting viewport in half, as used by many
	 * see-through glasses)
	 */
	private boolean mStereoRendering = false;

	/**
	 * Sensors component
	 */
	private SensorsComponentAndroid mSensors;

	/**
	 * Main GLSufaceView in which everything is rendered
	 */
	private GLSurfaceView mSurfaceView;

	/**
	 * Contains the Android UI layout
	 */
	private View mUI;

	/**
	 * Size of the viewport, needed to set the GL viewport to left/right eye
	 */
	private int[] mViewportSize;

	/**
	 * Load native libs required by the Metaio SDK
	 */
	protected void loadNativeLibs() throws UnsatisfiedLinkError
	{
		IMetaioSDKAndroid.loadNativeLibs();
		MetaioDebug.log(Log.INFO, "MetaioSDK libs loaded for " + SystemInfo.getDeviceABI() + " using "
				+ com.metaio.sdk.jni.SystemInfo.getAvailableCPUCores() + " CPU cores");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mUI = View.inflate(this, R.layout.main, null);

		// For see-through mode, the Metaio watermark must be shown manually if you are using the
		// free license, as
		// required by the license terms. If you have purchased Basic/Pro, you can delete these
		// lines.
		if (mSeeThrough)
		{
			mUI.findViewById(R.id.watermark).setVisibility(View.VISIBLE);
		}

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		try
		{
			// Load native libs
			loadNativeLibs();

			// Create metaio SDK instance by passing a valid signature
			metaioSDK = MetaioSDK.CreateMetaioSDKAndroid(this, getResources().getString(R.string.metaioSDKSignature));
			if (metaioSDK == null)
			{
				throw new Exception("Unsupported platform!");
			}

			// Register Metaio SDK callback
			mSDKCallback = new MetaioSDKCallbackHandler();
			metaioSDK.registerCallback(mSDKCallback);

			// Register sensors component
			mSensors = new SensorsComponentAndroid(getApplicationContext());
			metaioSDK.registerSensorsComponent(mSensors);

		}
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "Error creating Metaio SDK!");
			MetaioDebug.printStackTrace(Log.ERROR, e);
			finish();
			return;
		}

		try
		{
			// Enable metaio SDK log messages based on build configuration
			MetaioDebug.enableLogging(BuildConfig.DEBUG);

			// Extract all assets and overwrite existing files if debug build
			AssetsManager.extractAllAssets(getApplicationContext(), BuildConfig.DEBUG);
		}
		catch (IOException e)
		{
			MetaioDebug.log(Log.ERROR, "Error extracting application assets!");
			MetaioDebug.printStackTrace(Log.ERROR, e);
		}

		mCube = new Cube();
		mSurfaceView = null;
		mRendererInitialized = false;
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		if (mSurfaceView != null)
			mSurfaceView.onPause();

		mActivityIsPaused = true;
		metaioSDK.pause();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		metaioSDK.resume();
		mActivityIsPaused = false;

		if (mSurfaceView != null)
		{
			if (mSurfaceView.getParent() == null)
			{
				FrameLayout.LayoutParams params =
						new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				addContentView(mSurfaceView, params);
				mSurfaceView.setZOrderMediaOverlay(true);

				if (mUI != null)
				{
					if (mUI.getParent() == null)
					{
						addContentView(mUI, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
					}
					mUI.bringToFront();
				}
			}

			mSurfaceView.onResume();
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		if (metaioSDK != null)
		{
			// Set empty content view
			setContentView(new FrameLayout(this));

			// Start camera only when the activity starts the first time (see lifecycle:
			// http://developer.android.com/training/basics/activity-lifecycle/pausing.html)
			if (!mActivityIsPaused)
			{
				startCamera();
			}

			// Create a new GLSurfaceView
			mSurfaceView = new GLSurfaceView(this);
			mSurfaceView.setEGLContextClientVersion(1);
			mSurfaceView.setRenderer(this);
			mSurfaceView.setKeepScreenOn(true);
		}
	}

	@Override
	protected void onStop()
	{
		super.onStop();

		// Remove GLSurfaceView from the hierarchy because it has been destroyed automatically
		if (mSurfaceView != null)
		{
			((ViewGroup)mSurfaceView.getParent()).removeView(mSurfaceView);
			mSurfaceView = null;
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		if (metaioSDK != null)
		{
			metaioSDK.delete();
			metaioSDK = null;
		}

		if (mSDKCallback != null)
		{
			mSDKCallback.delete();
			mSDKCallback = null;
		}

		if (mSensors != null)
		{
			mSensors.registerCallback(null);
			mSensors.release();
			mSensors.delete();
			mSensors = null;
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		mScreenRotation = Screen.getRotation(this);
		metaioSDK.setScreenRotation(mScreenRotation);
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * Start camera. Override this to change camera index or resolution
	 */
	protected void startCamera()
	{
		final CameraVector cameras = metaioSDK.getCameraList();
		if (cameras.size() > 0)
		{
			// Start the first camera by default
			com.metaio.sdk.jni.Camera camera = cameras.get(0);

			// Disable YUV image pipeline to easily handle RGB images in CameraImageRenderer. If the
			// see-through
			// constant setting is active, we don't need this (YUV pipeline is faster!).
			if (!mSeeThrough)
			{
				camera.setYuvPipeline(false);
			}

			metaioSDK.startCamera(camera);
		}
	}

	@Override
	public void onDrawFrame(GL10 gl)
	{
		metaioSDK.requestCameraImage();

		// Note: The metaio SDK itself does not render anything here because we initialized it with
		// the NULL renderer. This call is necessary to get the camera image and update tracking.
		metaioSDK.render();

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		gl.glDisable(GL10.GL_DEPTH_TEST);

		if (!mSeeThrough)
		{
			gl.glViewport(0, 0, mViewportSize[0], mViewportSize[1]);
			mCameraImageRenderer.draw(gl, mScreenRotation);
		}

		gl.glEnable(GL10.GL_DEPTH_TEST);

		//
		// Render cube in front of camera image (if we're currently tracking)
		//

		final TrackingValues trackingValues = metaioSDK.getTrackingValues(1);

		if (trackingValues.isTrackingState())
		{
			float[] modelMatrix = new float[16];
			// preMultiplyWithStandardViewMatrix=false parameter explained below
			metaioSDK.getTrackingValues(1, modelMatrix, false, true);

			for (int eye = 0; eye < 2; ++eye)
			{
				ECAMERA_TYPE renderTarget =
						!mStereoRendering ? ECAMERA_TYPE.ECT_RENDERING_MONO : (eye == 0
								? ECAMERA_TYPE.ECT_RENDERING_STEREO_LEFT
								: ECAMERA_TYPE.ECT_RENDERING_STEREO_RIGHT);

				// Render on half viewport for stereo, or full viewport for mono
				if (mStereoRendering)
				{
					gl.glViewport(eye * (mViewportSize[0] / 2), 0, mViewportSize[0] / 2, mViewportSize[1]);
				}
				else
				{
					gl.glViewport(0, 0, mViewportSize[0], mViewportSize[1]);
				}

				// With getTrackingValues(..., preMultiplyWithStandardViewMatrix=true), the metaio
				// SDK
				// would calculate a model-view matrix, i.e. a standard look-at matrix (looking from
				// the
				// origin along the negative Z axis) multiplied by the model matrix (tracking pose).
				// Here we use our own view matrix for demonstration purposes (parameter set to
				// false),
				// for instance if you have your own camera implementation. Additionally, the cube
				// is
				// scaled up by factor 40 and translated by 40 units in order to have its back face
				// lie
				// on the tracked image.
				gl.glMatrixMode(GL10.GL_MODELVIEW);
				gl.glLoadIdentity();

				// We assume that hand-eye calibration is only used for stereo rendering
				if (mStereoRendering)
				{
					Vector3d t = new Vector3d();
					Rotation r = new Rotation();
					metaioSDK.getHandEyeCalibration(t, r, renderTarget);
					float[] hecMatrix = new float[16];
					r.getRotationMatrix4x4(hecMatrix);
					hecMatrix[3] = t.getX();
					hecMatrix[7] = t.getY();
					hecMatrix[11] = t.getZ();
					toColumnMajor(hecMatrix);
					gl.glMultMatrixf(hecMatrix, 0); // MODEL_VIEW = HEC * LOOK_AT * MODEL
				}

				// Use typical view matrix (camera looking along negative Z axis, see previous hint)
				// gl.glMultMatrixf(<<<identity view matrix>>>, 0);

				// The order is important here: We first want to scale the cube, then put it 40
				// units
				// higher (because it's rendered from -1 to +1 on all axes, after scaling that's
				// +-40)
				// so that its back face lies on the tracked image and move it into place
				// (transformation to the coordinate system of the tracked image).
				gl.glMultMatrixf(modelMatrix, 0); // MODEL_VIEW = HEC * LOOK_AT * MODEL
				gl.glTranslatef(0, 0, 40.0f);
				gl.glScalef(40.0f, 40.0f, 40.0f); // all sides of the cube then have dimension 80

				gl.glMatrixMode(GL10.GL_PROJECTION);
				float[] projMatrix = new float[16];

				// Use right-handed projection matrix
				metaioSDK.getProjectionMatrix(projMatrix, true, renderTarget);

				gl.glLoadMatrixf(projMatrix, 0);

				mCube.render(gl);

				if (!mStereoRendering)
				{
					break;
				}
			}
		}
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		if (height == 0)
			height = 1;

		mViewportSize = new int[] {width, height};

		gl.glViewport(0, 0, width, height);

		if (metaioSDK != null)
		{
			metaioSDK.resizeRenderer(width, height);
		}
		else
		{
			MetaioDebug.log(Log.ERROR, "Metaio SDK not yet created");
		}
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		if (!mRendererInitialized)
		{
			mScreenRotation = Screen.getRotation(this);

			// Set up custom rendering (metaio SDK will only do tracking and not render any objects
			// itself)
			metaioSDK.initializeRenderer(0, 0, mScreenRotation, ERENDER_SYSTEM.ERENDER_SYSTEM_NULL);
			mRendererInitialized = true;
		}

		// Create camera image renderer
		mCameraImageRenderer = new CameraImageRenderer(this, gl);

		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glClearColor(0, 0, 0, 0);

		gl.glClearDepthf(1.0f);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		gl.glDisable(GL10.GL_LIGHTING);

		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
	}

	private static void swap(float[] a, int i, int j)
	{
		float t = a[i];
		a[i] = a[j];
		a[j] = t;
	}

	/**
	 * Converts matrix from row major to column major in-place
	 * 
	 * @param m Matrix to convert in-place
	 */
	private static void toColumnMajor(float[] m)
	{
		swap(m, 1, 4);
		swap(m, 2, 8);
		swap(m, 3, 12);
		swap(m, 6, 9);
		swap(m, 7, 13);
		swap(m, 11, 14);
	}

	final class MetaioSDKCallbackHandler extends IMetaioSDKCallback
	{
		@SuppressWarnings("unused")
		@Override
		public void onNewCameraFrame(ImageStruct cameraFrame)
		{
			if (mCameraImageRenderer != null && !mSeeThrough)
			{
				mCameraImageRenderer.updateFrame(cameraFrame);
			}
		}

		@Override
		public void onSDKReady()
		{
			// Load desired tracking configuration when the SDK is ready
			final File trackingConfigFile =
					AssetsManager.getAssetPathAsFile(getApplicationContext(), "TrackingData_MarkerlessFast.xml");
			if (trackingConfigFile == null || !metaioSDK.setTrackingConfiguration(trackingConfigFile))
			{
				MetaioDebug.log(Log.ERROR, "Failed to set tracking configuration");
			}

			metaioSDK.setStereoRendering(true);
			metaioSDK.setSeeThrough(mSeeThrough);

			// @formatter:off

			// Recommended way to load stereo calibration (in this order):
			// 1) Load your own, exact calibration (calibration XML file created with Toolbox 6.0.1 or newer),
			//    i.e. *you* as developer provide a calibration file. Note that the path to "custom-hec.xml"
			//    doesn't actually exist in this example; it's only there to show how to apply a custom
			//    calibration file.
			// 2) Load calibration XML file from default path, i.e. in case the user has used Toolbox to
			//    calibrate (result file always stored at same path).
			// 3) Load calibration built into Metaio SDK for known devices (may not give perfect result
			//    because stereo glasses can vary).
			// Items 2) and 3) only do something on Android for the moment, as there are no supported,
			// non-Android stereo devices yet.
			final File calibrationFile = AssetsManager.getAssetPathAsFile(MainActivity.this, "custom-hec.xml");

			// @formatter:on

			if ((calibrationFile == null || !metaioSDK.setHandEyeCalibrationFromFile(calibrationFile))
					&& !metaioSDK.setHandEyeCalibrationFromFile() && !metaioSDK.setHandEyeCalibrationByDevice())
			{
				MetaioDebug.log(mStereoRendering ? Log.ERROR : Log.INFO,
						"No hand-eye calibration found/set for this device");
			}
		}

		@Override
		public void onTrackingEvent(TrackingValuesVector trackingValues)
		{
			for (int i = 0; i < trackingValues.size(); ++i)
			{
				final TrackingValues v = trackingValues.get(i);
				MetaioDebug.log("Tracking state for COS " + v.getCoordinateSystemID() + " is " + v.getState());
			}
		}
	}
}
