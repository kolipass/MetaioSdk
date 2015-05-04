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
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.SensorsComponentAndroid;
import com.metaio.sdk.jni.CameraVector;
import com.metaio.sdk.jni.ERENDER_SYSTEM;
import com.metaio.sdk.jni.ESCREEN_ROTATION;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.ImageStruct;
import com.metaio.sdk.jni.MetaioSDK;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.TrackingValuesVector;
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
	 * Camera image renderer which takes care of differences in camera image and viewport
	 * aspect ratios
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
	 * Sensors component
	 */
	private SensorsComponentAndroid mSensors;

	/**
	 * Main GLSufaceView in which everything is rendered
	 */
	private GLSurfaceView mSurfaceView;
	
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
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				addContentView(mSurfaceView, params);
				mSurfaceView.setZOrderMediaOverlay(true);
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
			
			// Start camera only when the activity starts the first time
			// (see lifecycle: http://developer.android.com/training/basics/activity-lifecycle/pausing.html)
			if (!mActivityIsPaused)
				startCamera(); 

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
			ViewGroup v = (ViewGroup) findViewById(android.R.id.content);
			v.removeAllViews();
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
			com.metaio.sdk.jni.Camera camera = cameras.get(0); 	// Start the first camera by default
			camera.setYuvPipeline(false);		// Disable YUV image pipeline to easily handle RGB images
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

		mCameraImageRenderer.draw(gl, mScreenRotation);

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

			// With getTrackingValues(..., preMultiplyWithStandardViewMatrix=true), the metaio SDK
			// would calculate a model-view matrix, i.e. a standard look-at matrix (looking from the
			// origin along the negative Z axis) multiplied by the model matrix (tracking pose).
			// Here we use our own view matrix for demonstration purposes (parameter set to false),
			// for instance if you have your own camera implementation. Additionally, the cube is
			// scaled up by factor 40 and translated by 40 units in order to have its back face lie
			// on the tracked image.
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glLoadIdentity();

			// Use typical view matrix (camera looking along negative Z axis, see previous hint)
			gl.glLoadIdentity();

			// The order is important here: We first want to scale the cube, then put it 40 units
			// higher (because it's rendered from -1 to +1 on all axes, after scaling that's +-40)
			// so that its back face lies on the tracked image and move it into place
			// (transformation to the coordinate system of the tracked image).
			gl.glMultMatrixf(modelMatrix, 0); // MODEL_VIEW = LOOK_AT * MODEL
			gl.glTranslatef(0, 0, 40);
			gl.glScalef(40, 40, 40); // all sides of the cube then have dimension 80

			gl.glMatrixMode(GL10.GL_PROJECTION);
			float[] projMatrix = new float[16];

			// Use right-handed projection matrix
			metaioSDK.getProjectionMatrix(projMatrix, true);

			// Since we render the camera image ourselves, and there are devices whose screen aspect
			// ratio does not match the camera aspect ratio, we have to make up for the stretched
			// and cropped camera image. The CameraImageRenderer class gives us values by which
			// pixels should be scaled from the middle of the screen (e.g. getScaleX() > 1 if the
			// camera image is wider than the screen and thus its width is displayed cropped).
			projMatrix[0] *= mCameraImageRenderer.getScaleX();
			projMatrix[5] *= mCameraImageRenderer.getScaleY();
			gl.glLoadMatrixf(projMatrix, 0);

			mCube.render(gl);
		}
	}



	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		if (height == 0)
			height = 1;

		gl.glViewport(0, 0, width, height);

		if (metaioSDK != null)
			metaioSDK.resizeRenderer(width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		if (!mRendererInitialized)
		{
			mScreenRotation = Screen.getRotation(this);

			// Set up custom rendering (metaio SDK will only do tracking and not render any objects itself)
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
	
	
	final class MetaioSDKCallbackHandler extends IMetaioSDKCallback 
	{

		@Override
		public void onNewCameraFrame(ImageStruct cameraFrame)
		{
			if (mCameraImageRenderer != null)
				mCameraImageRenderer.updateFrame(cameraFrame);
		}

		@Override
		public void onSDKReady()
		{
			// Load desired tracking configuration when the SDK is ready
			final File trackingConfigFile = AssetsManager.getAssetPathAsFile(getApplicationContext(), "TrackingData_MarkerlessFast.xml");
			if (trackingConfigFile == null || !metaioSDK.setTrackingConfiguration(trackingConfigFile))
				MetaioDebug.log(Log.ERROR, "Failed to set tracking configuration");
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
