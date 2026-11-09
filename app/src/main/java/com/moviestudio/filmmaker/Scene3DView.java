package com.moviestudio.filmmaker;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Lightweight ES 2.0 preview foundation for low-memory Android phones. */
public final class Scene3DView extends GLSurfaceView {
    private final PreviewRenderer previewRenderer;
    private float previousX, previousY;

    public Scene3DView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 0, 16, 0);
        previewRenderer = new PreviewRenderer();
        setRenderer(previewRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setPreserveEGLContextOnPause(false);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            previousX = event.getX();
            previousY = event.getY();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            previewRenderer.rotate((event.getX() - previousX) * 0.45f,
                    (event.getY() - previousY) * 0.25f);
            previousX = event.getX();
            previousY = event.getY();
            requestRender();
            return true;
        }
        return true;
    }

    private static final class PreviewRenderer implements Renderer {
        private static final float[] CUBE = {
            -1,-1, 1,  1,-1, 1,  1, 1, 1,  -1,-1, 1,  1, 1, 1, -1,1,1,
             1,-1,-1, -1,-1,-1, -1,1,-1,  1,-1,-1, -1,1,-1, 1,1,-1,
            -1,-1,-1, -1,-1, 1, -1,1,1,  -1,-1,-1, -1,1,1, -1,1,-1,
             1,-1,1,  1,-1,-1, 1,1,-1,  1,-1,1, 1,1,-1, 1,1,1,
            -1,1,1,  1,1,1, 1,1,-1,  -1,1,1, 1,1,-1, -1,1,-1,
            -1,-1,-1, 1,-1,-1, 1,-1,1, -1,-1,-1, 1,-1,1, -1,-1,1
        };
        private static final float[] FLOOR = {
            -8,0,-8, 8,0,-8, 8,0,8,
            -8,0,-8, 8,0,8, -8,0,8
        };
        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] rotation = new float[16];
        private final float[] model = new float[16];
        private final float[] vp = new float[16];
        private final float[] mvp = new float[16];
        private FloatBuffer cubeBuffer, floorBuffer;
        private int program, positionHandle, matrixHandle, colorHandle;
        private volatile float yaw = -25f, pitch = 12f;
        private volatile boolean failed;

        void rotate(float dx, float dy) {
            yaw += dx;
            pitch = Math.max(-20f, Math.min(55f, pitch + dy));
        }

        private FloatBuffer buffer(float[] values) {
            FloatBuffer result = ByteBuffer.allocateDirect(values.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            result.put(values).position(0);
            return result;
        }

        private int compile(int type, String source) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] status = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
            if (status[0] == 0) {
                String message = GLES20.glGetShaderInfoLog(shader);
                GLES20.glDeleteShader(shader);
                throw new IllegalStateException(message);
            }
            return shader;
        }

        @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            try {
                failed = false;
                GLES20.glClearColor(0.48f, 0.65f, 0.82f, 1f);
                GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                cubeBuffer = buffer(CUBE);
                floorBuffer = buffer(FLOOR);
                String vertex =
                    "attribute vec3 aPosition; uniform mat4 uMvp;" +
                    "void main(){gl_Position=uMvp*vec4(aPosition,1.0);}";
                String fragment =
                    "precision mediump float; uniform vec4 uColor;" +
                    "void main(){gl_FragColor=uColor;}";
                int vs = compile(GLES20.GL_VERTEX_SHADER, vertex);
                int fs = compile(GLES20.GL_FRAGMENT_SHADER, fragment);
                program = GLES20.glCreateProgram();
                GLES20.glAttachShader(program, vs);
                GLES20.glAttachShader(program, fs);
                GLES20.glLinkProgram(program);
                GLES20.glDeleteShader(vs);
                GLES20.glDeleteShader(fs);
                int[] status = new int[1];
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
                if (status[0] == 0) throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));
                positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
                matrixHandle = GLES20.glGetUniformLocation(program, "uMvp");
                colorHandle = GLES20.glGetUniformLocation(program, "uColor");
            } catch (Throwable error) {
                failed = true;
                android.util.Log.e("MovieStudio3D", "Preview initialization failed", error);
            }
        }

        @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            Matrix.perspectiveM(projection, 0, 45f,
                    (float) width / Math.max(1, height), 0.1f, 100f);
        }

        private void draw(FloatBuffer vertices, int count, float[] color,
                          float x, float y, float z, float sx, float sy, float sz) {
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, x, y, z);
            Matrix.scaleM(model, 0, sx, sy, sz);
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0);
            GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvp, 0);
            GLES20.glUniform4fv(colorHandle, 1, color, 0);
            vertices.position(0);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
                    false, 12, vertices);
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);
        }

        @Override public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            if (failed || program == 0) return;
            try {
                Matrix.setLookAtM(view, 0, 0f, 3.8f, 8.5f, 0f, 1f, 0f, 0f, 1f, 0f);
                Matrix.setIdentityM(rotation, 0);
                Matrix.rotateM(rotation, 0, pitch, 1f, 0f, 0f);
                Matrix.rotateM(rotation, 0, yaw, 0f, 1f, 0f);
                Matrix.multiplyMM(vp, 0, projection, 0, view, 0);
                Matrix.multiplyMM(vp, 0, vp, 0, rotation, 0);
                GLES20.glUseProgram(program);
                GLES20.glEnableVertexAttribArray(positionHandle);
                draw(floorBuffer, 6, new float[]{0.22f,0.48f,0.28f,1f}, 0f,-1.15f,0f,1f,1f,1f);
                draw(cubeBuffer, 36, new float[]{0.92f,0.55f,0.22f,1f}, 0f,0f,0f,0.85f,0.85f,0.85f);
                GLES20.glDisableVertexAttribArray(positionHandle);
            } catch (Throwable error) {
                failed = true;
                android.util.Log.e("MovieStudio3D", "Preview draw failed", error);
            }
        }
    }
}
