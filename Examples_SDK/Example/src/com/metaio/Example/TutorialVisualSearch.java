// Copyright 2007-2014 metaio GmbH. All rights reserved.
package com.metaio.Example;

import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.ETRACKING_STATE;
import com.metaio.sdk.jni.EVISUAL_SEARCH_STATE;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.IVisualSearchCallback;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.TrackingValuesVector;
import com.metaio.sdk.jni.VisualSearchResponseVector;
import com.metaio.tools.io.AssetsManager;

public class TutorialVisualSearch extends ARViewActivity 
{

	/**
	 * Geometry to display visual search result
	 */
	private IGeometry mModel;
	
	/**
	 * metaio SDK callback handler
	 */
	private MetaioSDKCallbackHandler mCallbackHandler;
	
	/**
	 * Visual search callback handler
	 */
	private VisualSearchCallbackHandler mVSCallback;
	
	/**
	 * Paint object used to draw text on the images
	 */
	Paint mPaint;
	
	boolean m_request;
	
	TrackingValues m_trackingValues;
	
	/**
	 * Visual search database ID
	 */
	private final static String databaseID = "sdktest";

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		mCallbackHandler = new MetaioSDKCallbackHandler();

		// initialize a visual search callback handler and register the callback
		mVSCallback = new VisualSearchCallbackHandler();
		metaioSDK.registerVisualSearchCallback(mVSCallback);
		
		mPaint = new Paint();
		
		m_request = true;

	}
	
	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		
		mCallbackHandler.delete();
		mCallbackHandler = null;
		
		mVSCallback.delete();
		mVSCallback = null;
	}

	@Override
	protected int getGUILayout() 
	{
		// Attaching layout to the activity
		return R.layout.tutorial_visual_search;
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() 
	{
		return mCallbackHandler;
	}

	public void onButtonClick(View v) 
	{
		finish();
	}

	@Override
	protected void loadContents() 
	{
	}

	@Override
	public void onDrawFrame() 
	{
		m_trackingValues = metaioSDK.getTrackingValues(1);
		//request new VisualSearch before rendering next frame
		if (m_request || !m_trackingValues.isTrackingState())
		{
			metaioSDK.requestVisualSearch(databaseID, true);
			m_request = false;
		}
		super.onDrawFrame();
	}
	
	@Override
	protected void onGeometryTouched(IGeometry geometry) 
	{
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

					// a toast message to alert the user
					Toast toast = Toast.makeText(getApplicationContext(), 
							"Please hold the camera to an image you want to perform visual search on.", Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
					
					// Start visual search
					m_request = true;
				}
			});
		}
		
		@Override
		public void onTrackingEvent(TrackingValuesVector trackingValues) 
		{
			if (trackingValues.size() > 0)
			{
				// if tracking is lost, request visual search
				if (trackingValues.get(0).getState() == ETRACKING_STATE.ETS_LOST)
				{
					MetaioDebug.log("Requesting a new visual search because tracking is lost...");
					m_request=true;
				}
			}
		}
	}

	final class VisualSearchCallbackHandler extends IVisualSearchCallback 
	{
		@Override
		public void onVisualSearchResult(VisualSearchResponseVector response, int errorCode)
		{
			// Don't set another tracking configuration while there's an active one
			if (m_trackingValues.isTrackingState())
			{
				return;
			}

			MetaioDebug.log("onVisualSearchResult: "+errorCode+", "+response.size());
			if (errorCode == 0 && response.size() > 0) 
			{
				
				// set the searched image as a tracking target
				MetaioDebug.log("Loading tracking configuration...");
				boolean result = metaioSDK.setTrackingConfiguration(response.get(0).getTrackingConfiguration(), false);
				MetaioDebug.log("Tracking configuration loaded: "+result);

				// load an image geometry to display the result on the pattern
				final String texturePath = AssetsManager.getAssetPath(getApplicationContext(), "TutorialVisualSearch/Assets/poi.png");
				if (texturePath != null) 
				{
					// remove the file extension
					final String name = response.get(0).getTrackingConfigurationName().replaceFirst("[.][^.]+$", "");
					
					// create a billboard texture that highlights the file name of the searched image
					final String imagePath = createTexture(name, texturePath);
					
					if (imagePath != null)
					{
						if (mModel == null)
						{
							// create new geometry
							mModel = metaioSDK.createGeometryFromImage(imagePath);
							mModel.setScale(1.5f);
							MetaioDebug.log("The image has been loaded successfully");
						}
						else
						{
							// update texture with new image
							mModel.setTexture(imagePath);
						}
					}
					else
					{
						MetaioDebug.log(Log.ERROR, "Error creating image texture");
					}
				}

			} 
			else 
			{
				if (errorCode > 0)
					MetaioDebug.log(Log.ERROR, "Visual search error: "+errorCode);
				
				// if visual search didn't succeed, request another round
				MetaioDebug.log("Requesting new visual search because search failed...");
				metaioSDK.requestVisualSearch(databaseID, true);
			}
		}

		@Override
		public void onVisualSearchStatusChanged(EVISUAL_SEARCH_STATE state) 
		{
			MetaioDebug.log("The current visual search state is: " + state);
		}

	}
	
	/**
	 * Create a texture with the title on the background image 
	 * @param title Title string
	 * @param backgroundImagePath Full path to the background image
	 * @return Full path of the created texture, or null if failed
	 */
	private String createTexture(String title, String backgroundImagePath) 
	{
		try 
		{
			final String texturepath = getCacheDir()+"/"+title+".png";

			// Load background image and make a mutable copy
			Bitmap backgroundImage = BitmapFactory.decodeFile(backgroundImagePath);
			Bitmap image = backgroundImage.copy(Bitmap.Config.ARGB_8888, true);
			backgroundImage.recycle();
			backgroundImage = null;

			Canvas c = new Canvas(image);

			mPaint.setColor(Color.WHITE);
			mPaint.setTextSize(66);
			mPaint.setTypeface(Typeface.DEFAULT);

			// Draw title string
			if (title != null && title.length() > 0) 
			{
				String n = title.trim();

				final int maxWidth = 10000;

				int i = mPaint.breakText(n, true, maxWidth, null);
				mPaint.setTextAlign(Align.CENTER);

				c.drawText(n.substring(0, i), c.getWidth() / 2,
						c.getHeight() / 2 + 25.0f, mPaint);

			}

			// writing to the file
			try 
			{
				FileOutputStream out = new FileOutputStream(texturepath);
				image.compress(Bitmap.CompressFormat.PNG, 90, out);
				MetaioDebug.log("Texture file is saved to " + texturepath);
				return texturepath;
			} 
			catch (Exception e) 
			{
				MetaioDebug.log("Failed to save texture file");
				e.printStackTrace();
			}

			image.recycle();
			image = null;

		} 
		catch (Exception e) 
		{
			MetaioDebug.log(Log.ERROR, "Error creating billboard texture: "+ e.getMessage());
			MetaioDebug.printStackTrace(Log.ERROR, e);
			return null;
		}
		return null;
	}


}
