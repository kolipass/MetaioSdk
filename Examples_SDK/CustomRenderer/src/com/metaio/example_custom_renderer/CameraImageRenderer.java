// Copyright 2007-2014 metaio GmbH. All rights reserved.
package com.metaio.example_custom_renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.metaio.sdk.jni.ESCREEN_ROTATION;
import com.metaio.sdk.jni.ImageStruct;

public final class CameraImageRenderer
{
	private static final String TAG = "CameraImageRenderer";

	/**
	 * Camera frame aspect ratio (does not change with screen rotation, e.g. 640/480 = 1.333)
	 */
	private float mCameraAspect;

	private int mCameraImageHeight;

	private int mCameraImageWidth;

	private Context mContext;

	private boolean mInitialized = false;

	private boolean mMustUpdateTexture = false;

	/**
	 * Value by which the X axis must be scaled in the overall projection matrix in order to make
	 * up for a aspect-corrected (by cropping) camera image. Set on each draw() call.
	 */
	private float mScaleX;

	private float mScaleY;

	private int mTexture = -1;

	private ByteBuffer mTextureBuffer;

	private boolean mTextureInitialized = false;

	private int mTextureHeight;

	private int mTextureWidth;

	private FloatBuffer mTexCoordsBuffer;

	private FloatBuffer mVertexBuffer;

	public CameraImageRenderer(Context context, GL10 gl)
	{
		mContext = context;

		final float[] vertices = {
			-1, -1, 0,
			 1, -1, 0,
			-1,  1, 0,
			 1,  1, 0
		};

		ByteBuffer buffer = ByteBuffer.allocateDirect(vertices.length * 4);
		buffer.order(ByteOrder.nativeOrder());
		mVertexBuffer = buffer.asFloatBuffer();
		mVertexBuffer.put(vertices);
		mVertexBuffer.rewind();

		// Create texture coordinates buffer but don't fill it yet
		buffer = ByteBuffer.allocateDirect(vertices.length/3 * 8);
		buffer.order(ByteOrder.nativeOrder());
		mTexCoordsBuffer = buffer.asFloatBuffer();

		// Generate texture
		int[] tmp = new int[1];
		gl.glGenTextures(1, tmp, 0);
		mTexture = tmp[0];
	}

	public void draw(GL10 gl, ESCREEN_ROTATION screenRotation)
	{
		if (!mInitialized)
			return;

		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, mTexture);

		if (mMustUpdateTexture)
		{
			if(!mTextureInitialized)
			{
				// Allocate camera image texture once with 2^n dimensions
				gl.glTexImage2D(
					GL10.GL_TEXTURE_2D,
					0,
					GL10.GL_RGBA,
					mTextureWidth,
					mTextureHeight,
					0,
					GL10.GL_RGBA,
					GL10.GL_UNSIGNED_BYTE,
					null);

				mTextureInitialized = true;
			}

			// ...but only overwrite the camera image-sized region
			gl.glTexSubImage2D(
				GL10.GL_TEXTURE_2D,
				0,
				0,
				0,
				mCameraImageWidth,
				mCameraImageHeight,
				GL10.GL_RGBA,
				GL10.GL_UNSIGNED_BYTE,
				mTextureBuffer);

			final float xRatio = (float)mCameraImageWidth / mTextureWidth;
			final float yRatio = (float)mCameraImageHeight / mTextureHeight;

			final boolean cameraIsRotated = screenRotation == ESCREEN_ROTATION.ESCREEN_ROTATION_90 ||
			                                screenRotation == ESCREEN_ROTATION.ESCREEN_ROTATION_270;
			final float cameraAspect = cameraIsRotated ? 1.0f/mCameraAspect : mCameraAspect;

			Display display = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			DisplayMetrics displayMetrics = new DisplayMetrics();
			display.getMetrics(displayMetrics);

			// DisplayMetrics.widthPixels/heightPixels are the width/height in the current
			// orientation (i.e. values get swapped when you rotate the device)
			float screenAspect = (float)displayMetrics.widthPixels / displayMetrics.heightPixels;

			float offsetX, offsetY;

			if (cameraAspect > screenAspect)
			{
				// Camera image is wider (e.g. 480x640 camera image vs. a 480x800 device, example
				// in portrait mode), so crop the width of the camera image
				float aspectRatio = screenAspect / cameraAspect;
				offsetX = 0.5f * (1 - aspectRatio);
				offsetY = 0;

				mScaleX = cameraAspect / screenAspect;
				mScaleY = 1;
			}
			else
			{
				// Screen is wider, so crop the height of the camera image
				float aspectRatio = cameraAspect / screenAspect;
				offsetY = 0.5f * (1 - aspectRatio);
				offsetX = 0;

				mScaleX = 1;
				mScaleY = screenAspect / cameraAspect;
			}

			if (cameraIsRotated)
			{
				// Camera image will be rendered with +-90° rotation, so switch UV coordinates
				float tmp = offsetX;
				offsetX = offsetY;
				offsetY = tmp;
			}

			// Calculate texture coordinates. offsetX/offsetY are for cropping if camera and screen
			// aspect ratios differ. xRatio/yRatio are here because the OpenGL texture has
			// dimensions of 2^n, but the camera image does not fill it completely (e.g. camera
			// image 640x480 vs. texture size 1024x512).
			mTexCoordsBuffer.put(new float[] {
				offsetX * xRatio,     (1-offsetY) * yRatio,
				(1-offsetX) * xRatio, (1-offsetY) * yRatio,
				offsetX * xRatio,     offsetY * yRatio,
				(1-offsetX) * xRatio, offsetY * yRatio
			});
			mTexCoordsBuffer.rewind();

			mMustUpdateTexture = false;
		}

		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexCoordsBuffer);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		switch (screenRotation)
		{
			// Portrait
			case ESCREEN_ROTATION_270:
				gl.glRotatef(-90, 0, 0, 1);
				break;

			// Reverse portrait (upside down)
			case ESCREEN_ROTATION_90:
				gl.glRotatef(90, 0, 0, 1);
				break;

			// Landscape (right side of tall device facing up)
			case ESCREEN_ROTATION_0:
				break;

			// Reverse landscape (left side of tall device facing up)
			case ESCREEN_ROTATION_180:
				gl.glRotatef(180, 0, 0, 1);
				break;

			default:
				Log.e(TAG, "Unknown screen rotation");
		}

		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glPopMatrix();

		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);

		gl.glDisable(GL10.GL_TEXTURE_2D);
	}

	private static int getNextPowerOf2(int value)
	{
		for (int i = 0; i < 12; ++i)
		{
			if ((1 << i) >= value)
				return 1 << i;
		}

		throw new RuntimeException("Value too large");
	}

	public float getScaleX()
	{
		return mScaleX;
	}

	public float getScaleY()
	{
		return mScaleY;
	}

	private void init(int cameraImageWidth, int cameraImageHeight)
	{
		mTextureWidth = getNextPowerOf2(cameraImageWidth);
		mTextureHeight = getNextPowerOf2(cameraImageHeight);

		mTextureBuffer = ByteBuffer.allocateDirect(cameraImageWidth * cameraImageHeight * 4);

		mInitialized = true;
	}

	public void updateFrame(ImageStruct frame)
	{
		final int frameWidth = frame.getWidth();
		final int frameHeight = frame.getHeight();

		mCameraAspect = (float)frameWidth / frameHeight;

		switch (frame.getColorFormat())
		{
			case ECF_RGBA8:
				if (!mInitialized)
					init(frameWidth, frameHeight);

				if (!frame.getOriginIsUpperLeft())
				{
					Log.e(TAG, "Unimplemented: camera image upside-down");
					return;
				}

				mTextureBuffer.rewind();
				frame.copyBufferToNioBuffer(mTextureBuffer);
				mTextureBuffer.rewind();

				break;

			default:
				Log.e(TAG, "Unimplemented color format " + frame.getColorFormat());
				return;
		}

		mMustUpdateTexture = true;

		mCameraImageWidth = frameWidth;
		mCameraImageHeight = frameHeight;
	}
}
