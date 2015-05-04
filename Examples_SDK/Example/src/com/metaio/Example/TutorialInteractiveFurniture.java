// Copyright 2007-2014 metaio GmbH. All rights reserved.
package com.metaio.Example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.GestureHandlerAndroid;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.GestureHandler;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.ImageStruct;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.StringVector;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.Vector2d;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.io.AssetsManager;

public class TutorialInteractiveFurniture extends ARViewActivity
{
	private MetaioSDKCallbackHandler mCallbackHandler;
	private IGeometry mMetaioMan;
	private IGeometry mTV;
	private IGeometry mScreen;
	private IGeometry mChair;
	private GestureHandlerAndroid mGestureHandler;
	private TrackingValues mTrackingValues;
	private int mGestureMask;
	boolean mImageTaken;
	private Vector2d mMidPoint;
	private View mLayoutGeometries;

	/**
	 * File where camera image will be temporarily stored
	 */
	private File mImageFile;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mGestureMask = GestureHandler.GESTURE_ALL;
		mImageTaken = false;

		mCallbackHandler = new MetaioSDKCallbackHandler();
		mGestureHandler = new GestureHandlerAndroid(metaioSDK, mGestureMask);
		mMidPoint = new Vector2d();

		mImageFile = new File(Environment.getExternalStorageDirectory(), "target.jpg");
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		try
		{
			// Pause movie texture
			mScreen.pauseMovieTexture();
		}
		catch (NullPointerException e)
		{
		}

	}

	@Override
	protected void onResume()
	{
		super.onResume();

		try
		{
			// Resume movie texture
			mScreen.startMovieTexture(true);
		}
		catch (NullPointerException e)
		{
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		mLayoutGeometries = mGUIView.findViewById(R.id.layoutGeometries);

		// if a tracking target image exists, then the app is still running in the background
		if (mImageFile.exists() && mTrackingValues != null)
		{
			// the tracking target has to be reset and so are the tracking values
			metaioSDK.setImage(mImageFile);
			metaioSDK.setCosOffset(1, mTrackingValues);

		}

	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		mCallbackHandler.delete();
		mCallbackHandler = null;

		// delete the tracking target image before exit if it has been generated
		if (mImageFile.exists())
		{
			boolean result = mImageFile.delete();
			MetaioDebug.log("The file has been deleted: " + result);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		super.onTouch(v, event);

		mGestureHandler.onTouch(v, event);

		return true;
	}

	@Override
	protected int getGUILayout()
	{
		return R.layout.tutorial_interactive_furniture;
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler()
	{
		return mCallbackHandler;
	}

	@Override
	public void onSurfaceChanged(int width, int height)
	{
		super.onSurfaceChanged(width, height);

		// Update mid point of the view
		mMidPoint.setX(width / 2f);
		mMidPoint.setY(height / 2f);
	}

	@Override
	public void onDrawFrame()
	{
		super.onDrawFrame();

		// reset the location and scale of the geometries
		if (mImageTaken == true)
		{
			// load the dummy tracking config file
			boolean result = metaioSDK.setTrackingConfiguration("DUMMY");
			MetaioDebug.log("Tracking data Dummy loaded: " + result);

			metaioSDK.setCosOffset(1, mTrackingValues);

			mImageTaken = false;
		}


	}

	public void onButtonClick(View v)
	{
		finish();
	}

	// called when the save screenshot button has been pressed
	public void onSaveScreen(View v)
	{
		// request screen shot
		metaioSDK.requestScreenshot();
	}

	// called when the reset button has been pressed
	public void onClearScreen(View v)
	{
		// Start camera again
		startCamera();

		// delete the tracking target if generated
		String imagepath = Environment.getExternalStorageDirectory().getPath() + "/target.jpg";
		File file = new File(imagepath);
		if (file.exists())
		{
			boolean result = file.delete();
			MetaioDebug.log("The file has been deleted: " + result);
		}

		// load the ORIENTATION tracking config file again
		boolean result = metaioSDK.setTrackingConfiguration("ORIENTATION_FLOOR");
		MetaioDebug.log("Tracking data loaded: " + result);

		// reset the geometry buttons to unselected and hide the geometries
		ImageButton button = (ImageButton)findViewById(R.id.buttonTV);
		button.setImageResource(R.drawable.button_tv_unselected);
		button = (ImageButton)findViewById(R.id.buttonChair);
		button.setSelected(false);
		button.setImageResource(R.drawable.button_chair_unselected);
		button = (ImageButton)findViewById(R.id.buttonMan);
		button.setSelected(false);
		button.setImageResource(R.drawable.button_man_unselected);
		button.setSelected(false);

		setVisibleTV(false);
		setVisibleChair(false);
		setVisibleMan(false);

		mLayoutGeometries.setVisibility(View.GONE);
		mGUIView.bringToFront();
	}

	// called when the take picture button has been pressed
	public void onTakePicture(View v)
	{
		// take a picture using the SDK and save it to external storage
		metaioSDK.requestCameraImage(mImageFile);

	}


	// called when the TV button has been pressed
	public void onTVButtonClick(View v)
	{
		ImageButton button = (ImageButton)v;
		button.setSelected(!button.isSelected());

		if (button.isSelected())
		{
			button.setImageResource(R.drawable.button_tv_selected);

			// reset the location and scale of the geometries

			Vector3d translation = metaioSDK.get3DPositionFromViewportCoordinates(1, mMidPoint);
			mTV.setTranslation(translation);
			mScreen.setTranslation(translation);
			mTV.setScale(50f);
			mScreen.setScale(50f);
		}
		else
			button.setImageResource(R.drawable.button_tv_unselected);
		setVisibleTV(button.isSelected());

	}

	public void onChairButtonClick(View v)
	{
		ImageButton button = (ImageButton)v;
		button.setSelected(!button.isSelected());

		if (button.isSelected())
		{
			button.setImageResource(R.drawable.button_chair_selected);

			Vector3d translation = metaioSDK.get3DPositionFromViewportCoordinates(1, mMidPoint);
			mChair.setTranslation(translation);
			mChair.setScale(50f);
		}
		else
			button.setImageResource(R.drawable.button_chair_unselected);
		setVisibleChair(button.isSelected());

	}

	public void onManButtonClick(View v)
	{
		ImageButton button = (ImageButton)v;
		button.setSelected(!button.isSelected());

		if (button.isSelected())
		{
			button.setImageResource(R.drawable.button_man_selected);

			Vector3d translation = metaioSDK.get3DPositionFromViewportCoordinates(1, mMidPoint);
			mMetaioMan.setTranslation(translation);
			mMetaioMan.setScale(5f);
		}
		else
			button.setImageResource(R.drawable.button_man_unselected);
		setVisibleMan(button.isSelected());

	}

	@Override
	protected void loadContents()
	{
		try
		{
			// TODO: Load desired tracking data for planar marker tracking
			boolean result = metaioSDK.setTrackingConfiguration("ORIENTATION_FLOOR");
			MetaioDebug.log("Tracking data loaded: " + result);

			// Load all the geometries
			// Load TV

			File filepath =
					AssetsManager.getAssetPathAsFile(getApplicationContext(),
							"TutorialInteractiveFurniture/Assets/tv.obj");
			if (filepath != null)
			{
				mTV = metaioSDK.createGeometry(filepath);

				if (mTV != null)
				{
					mTV.setScale(50f);
					mTV.setRotation(new Rotation((float)Math.PI / 2f, 0f, -(float)Math.PI / 4f));
					mTV.setTranslation(new Vector3d(0f, 10f, 0f));

					mGestureHandler.addObject(mTV, 1);
				}
				else
				{
					MetaioDebug.log(Log.ERROR, "Error loading geometry: " + filepath);
				}
			}


			// Load screen
			filepath =
					AssetsManager.getAssetPathAsFile(getApplicationContext(),
							"TutorialInteractiveFurniture/Assets/screen.obj");
			if (filepath != null)
			{
				mScreen = metaioSDK.createGeometry(filepath);

				// the parameters for the screen should be exactly the same as the ones for the TV
				if (mScreen != null)
				{
					mScreen.setScale(50f);
					mScreen.setRotation(new Rotation((float)Math.PI / 2f, 0f, -(float)Math.PI / 4f));
					mScreen.setTranslation(new Vector3d(0f, 10f, 0f));

					mScreen.setMovieTexture(AssetsManager.getAssetPathAsFile(getApplicationContext(),
							"TutorialInteractiveFurniture/Assets/sintel.3g2"));
					mScreen.startMovieTexture(true);

					mGestureHandler.addObject(mScreen, 1);
					setVisibleTV(false);
				}
				else
				{
					MetaioDebug.log(Log.ERROR, "Error loading geometry: " + filepath);
				}
			}

			// Load chair
			filepath =
					AssetsManager.getAssetPathAsFile(getApplicationContext(),
							"TutorialInteractiveFurniture/Assets/stuhl.obj");
			if (filepath != null)
			{
				mChair = metaioSDK.createGeometry(filepath);

				if (mChair != null)
				{
					mChair.setScale(50f);
					mChair.setTranslation(new Vector3d(0f, 0f, 0f));
					mChair.setRotation(new Rotation((float)Math.PI / 2f, 0f, 0f));

					mGestureHandler.addObject(mChair, 2);
					setVisibleChair(false);
				}
				else
				{
					MetaioDebug.log(Log.ERROR, "Error loading geometry: " + filepath);
				}
			}


			// Load metaio man
			filepath =
					AssetsManager.getAssetPathAsFile(getApplicationContext(),
							"TutorialInteractiveFurniture/Assets/metaioman.md2");
			if (filepath != null)
			{
				mMetaioMan = metaioSDK.createGeometry(filepath);

				if (mMetaioMan != null)
				{
					// Set geometry properties
					mMetaioMan.setScale(5f);
					mMetaioMan.setTranslation(new Vector3d(0f, 0f, 0f));

					mGestureHandler.addObject(mMetaioMan, 3);

					// Start first animation
					StringVector animations = mMetaioMan.getAnimationNames();
					if (animations.size() > 0)
					{
						mMetaioMan.startAnimation(animations.get(0), true);
					}

					setVisibleMan(false);
				}
				else
				{
					MetaioDebug.log(Log.ERROR, "Error loading geometry: " + filepath);
				}
			}
		}
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "loadContents failed: " + e);
		}
	}

	private void setVisibleTV(boolean visible)
	{
		if (mTV != null && mScreen != null)
		{
			mTV.setVisible(visible);
			mScreen.setVisible(visible);
		}
		if (visible)
		{
			mScreen.startMovieTexture();
		}
		else
		{
			mScreen.stopMovieTexture();
		}
	}

	private void setVisibleChair(boolean visible)
	{
		if (mChair != null)
		{
			mChair.setVisible(visible);
		}
	}

	private void setVisibleMan(boolean visible)
	{
		if (mMetaioMan != null)
		{
			mMetaioMan.setVisible(visible);
		}
	}

	@Override
	protected void onGeometryTouched(final IGeometry geometry)
	{
		MetaioDebug.log("MetaioSDKCallbackHandler.onGeometryTouched: " + geometry);
	}

	final class MetaioSDKCallbackHandler extends IMetaioSDKCallback
	{
		/**
		 * Get path to Pictures directory if it exists
		 * 
		 * @return Path to Pictures directory on the device if found, else <code>null</code>
		 */
		private File getPicturesDirectory()
		{
			File picPath = null;

			try
			{
				picPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
				File path = new File(picPath, "Metaio Example");
				boolean success = path.mkdirs() || path.isDirectory();
				if (!success)
				{
					path = new File(Environment.getExternalStorageDirectory(), "Pictures");
				}
				success = path.mkdirs() || path.isDirectory();
				if (!success)
				{
					path = Environment.getDataDirectory();
				}

				return path.getAbsoluteFile();
			}
			catch (Exception e)
			{
				return null;
			}
		}

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

		// callback function for taking images using SDK
		@Override
		public void onCameraImageSaved(final File filePath)
		{
			// save the tracking values in case the application exits improperly
			mTrackingValues = metaioSDK.getTrackingValues(1);
			mImageTaken = true;

			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (filePath.getPath().length() > 0)
					{
						metaioSDK.setImage(filePath);
						mLayoutGeometries.setVisibility(View.VISIBLE);
					}
				}
			});

		}

		@Override
		public void onScreenshotImage(ImageStruct image)
		{
			final File directory = getPicturesDirectory();
			if (directory == null)
			{
				image.release();
				image.delete();

				MetaioDebug.log(Log.ERROR, "Could not find pictures directory, not saving screenshot");
				return;
			}

			// Creating directory
			directory.mkdirs();

			try
			{
				// Creating file
				final File screenshotFile = new File(directory, "screenshot_" + System.currentTimeMillis() + ".jpg");
				screenshotFile.createNewFile();

				FileOutputStream stream = new FileOutputStream(screenshotFile);

				boolean result = false;
				Bitmap bitmap = image.getBitmap();
				try
				{
					result = bitmap.compress(CompressFormat.JPEG, 100, stream);
				}
				finally
				{
					// release screenshot ImageStruct
					image.release();
					image.delete();

					stream.close();
				}

				if (!result)
				{
					MetaioDebug.log(Log.ERROR, "Failed to save screenshot to " + screenshotFile);
					return;
				}

				final String url =
						MediaStore.Images.Media.insertImage(getContentResolver(), bitmap,
								"screenshot_" + System.currentTimeMillis(), "screenshot");


				// Recycle the bitmap
				bitmap.recycle();
				bitmap = null;

				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						String message = "The screenshot has been added to the gallery.";
						if (url == null)
						{
							message = "Unable to add the screen shot to the gallery";
						}
						else
						{
							MediaScannerConnection.scanFile(getApplicationContext(),
									new String[] {screenshotFile.getAbsolutePath()}, new String[] {"image/jpg"},
									new OnScanCompletedListener()
									{
										@Override
										public void onScanCompleted(String path, Uri uri)
										{
											MetaioDebug.log("Screen saved at path " + path);
										}
									});
						}

						Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();
					}
				});
			}
			catch (IOException e)
			{
				MetaioDebug.printStackTrace(Log.ERROR, e);
			}
		}
	}
}
