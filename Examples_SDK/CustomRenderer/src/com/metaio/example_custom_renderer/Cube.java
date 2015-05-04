// Copyright 2007-2014 metaio GmbH. All rights reserved.
package com.metaio.example_custom_renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public final class Cube
{
	private float[] colors = {
		0.0f, 1.0f, 0.0f,
		1.0f, 0.5f, 0.0f,
		1.0f, 0.0f, 0.0f,
		1.0f, 1.0f, 0.0f,
		0.0f, 0.0f, 1.0f,
		1.0f, 0.0f, 1.0f
	};

	private FloatBuffer vertexBuffer;

	/**
	 * Triangle strip vertices for a cube
	 */
	private float[] vertices = {
		// Top (green)
		-1.0f,  1.0f, -1.0f,
		-1.0f,  1.0f,  1.0f,
		 1.0f,  1.0f, -1.0f,
		 1.0f,  1.0f,  1.0f,

		// Bottom (orange)
		-1.0f, -1.0f,  1.0f,
		-1.0f, -1.0f, -1.0f,
		 1.0f, -1.0f,  1.0f,
		 1.0f, -1.0f, -1.0f,

		// Front (red)
		-1.0f,  1.0f,  1.0f,
		-1.0f, -1.0f,  1.0f,
		 1.0f,  1.0f,  1.0f,
		 1.0f, -1.0f,  1.0f,

		// Back (yellow)
		-1.0f, -1.0f, -1.0f,
		-1.0f,  1.0f, -1.0f,
		 1.0f, -1.0f, -1.0f,
		 1.0f,  1.0f, -1.0f,

		// Left (blue)
		-1.0f,  1.0f, -1.0f,
		-1.0f, -1.0f, -1.0f,
		-1.0f,  1.0f,  1.0f,
		-1.0f, -1.0f,  1.0f,

		// Right (purple)
		1.0f,  1.0f,  1.0f,
		1.0f, -1.0f,  1.0f,
		1.0f,  1.0f, -1.0f,
		1.0f, -1.0f, -1.0f
	};

	public Cube()
	{
		ByteBuffer buffer = ByteBuffer.allocateDirect(vertices.length * 4);
		buffer.order(ByteOrder.nativeOrder());
		vertexBuffer = buffer.asFloatBuffer();
		vertexBuffer.put(vertices);
		vertexBuffer.rewind();
	}

	public void render(GL10 gl)
	{
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);

		for (int side = 0; side < 6; ++side)
		{
			gl.glColor4f(colors[3*side], colors[3*side+1], colors[3*side+2], 1);

			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, side*4, 4);
		}

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}
}