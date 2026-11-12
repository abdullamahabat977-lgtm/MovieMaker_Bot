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

/**
 * Low-memory OpenGL ES 2.0 scene preview.
 * One-finger drag orbits the camera; two-finger pinch changes camera distance.
 */
public final class Scene3DView extends GLSurfaceView {
    public static final int SCENE_CHARACTER = 0;
    public static final int SCENE_CUBE = 1;

    private final PreviewRenderer previewRenderer;
    private float previousX, previousY, previousPinchDistance;

    public Scene3DView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        // Ask Android for a depth-enabled RGB565-compatible config without requiring
        // one exact channel layout; this is more tolerant of vendor EGL implementations.
        setEGLConfigChooser(true);
        setPreserveEGLContextOnPause(false);
        previewRenderer = new PreviewRenderer();
        setRenderer(previewRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    /** Selects a lightweight procedural 3D object without rebuilding the GL view. */
    public void setSceneMode(int mode) {
        previewRenderer.setSceneMode(mode == SCENE_CUBE ? SCENE_CUBE : SCENE_CHARACTER);
        requestRender();
    }

    /** Changes the preview character's skin palette without recreating the renderer. */
    public void setSkinPalette(int index) {
        previewRenderer.setSkinPalette(index);
        requestRender();
    }

    /** Changes the preview character's shirt palette without recreating the renderer. */
    public void setShirtPalette(int index) {
        previewRenderer.setShirtPalette(index);
        requestRender();
    }

    private static float spacing(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0f;
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                previousX = event.getX();
                previousY = event.getY();
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                previousPinchDistance = spacing(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() >= 2) {
                    float current = spacing(event);
                    if (previousPinchDistance > 0f && current > 0f) {
                        previewRenderer.zoom((previousPinchDistance - current) * 0.018f);
                    }
                    previousPinchDistance = current;
                } else {
                    float x = event.getX();
                    float y = event.getY();
                    previewRenderer.orbit((x - previousX) * 0.45f, (y - previousY) * 0.35f);
                    previousX = x;
                    previousY = y;
                }
                requestRender();
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                previousPinchDistance = 0f;
                int remaining = event.getActionIndex() == 0 ? 1 : 0;
                if (remaining < event.getPointerCount()) {
                    previousX = event.getX(remaining);
                    previousY = event.getY(remaining);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                previousPinchDistance = 0f;
                return true;
            default:
                return true;
        }
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
        // Eight triangular faces form a compact low-poly head mesh.
        private static final float[] HEAD = {
             0,1,0,  1,0,0,  0,0,1,
             0,1,0,  0,0,-1, 1,0,0,
             0,1,0, -1,0,0, 0,0,-1,
             0,1,0,  0,0,1, -1,0,0,
             0,-1,0, 0,0,1, 1,0,0,
             0,-1,0, 1,0,0, 0,0,-1,
             0,-1,0, 0,0,-1, -1,0,0,
             0,-1,0, -1,0,0, 0,0,1
        };
        private static final float[] FLOOR = {
            -8,-1.16f,-8, 8,-1.16f,-8, 8,-1.16f,8,
            -8,-1.16f,-8, 8,-1.16f,8, -8,-1.16f,8
        };
        private static final float[][] FACE_COLORS = {
            {0.95f,0.43f,0.16f,1f}, {0.58f,0.24f,0.12f,1f},
            {0.18f,0.55f,0.78f,1f}, {0.20f,0.38f,0.64f,1f},
            {0.98f,0.68f,0.20f,1f}, {0.45f,0.31f,0.18f,1f}
        };
        // Static palettes avoid allocating new color arrays on every rendered frame.
        private static final float[][] SKIN_PALETTES = {
            {0.91f,0.73f,0.56f,1f}, {0.72f,0.48f,0.34f,1f},
            {0.54f,0.35f,0.24f,1f}, {0.36f,0.22f,0.16f,1f}
        };
        private static final float[][] SHIRT_PALETTES = {
            {0.16f,0.39f,0.69f,1f}, {0.71f,0.24f,0.26f,1f},
            {0.15f,0.51f,0.35f,1f}, {0.45f,0.31f,0.66f,1f},
            {0.77f,0.54f,0.16f,1f}, {0.22f,0.27f,0.36f,1f}
        };
        private static final float[][] SHIRT_LIGHT_PALETTES = {
            {0.28f,0.54f,0.84f,1f}, {0.84f,0.36f,0.38f,1f},
            {0.29f,0.70f,0.50f,1f}, {0.61f,0.47f,0.82f,1f},
            {0.90f,0.71f,0.36f,1f}, {0.40f,0.46f,0.56f,1f}
        };
        private static final float[] TROUSERS_COLOR = {0.13f,0.20f,0.32f,1f};
        private static final float[] BOOTS_COLOR = {0.07f,0.09f,0.13f,1f};
        private static final float[] HAIR_COLOR = {0.13f,0.10f,0.09f,1f};
        private static final float[] EYE_COLOR = {0.035f,0.045f,0.06f,1f};
        private static final float[] FLOOR_COLOR = {0.19f,0.34f,0.25f,1f};
        private static final float[] GRID_COLOR = {0.48f,0.61f,0.52f,1f};

        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] model = new float[16];
        private final float[] vp = new float[16];
        private final float[] mvp = new float[16];
        private FloatBuffer cubeBuffer, headBuffer, floorBuffer, gridBuffer;
        private int program, positionHandle, matrixHandle, colorHandle;
        private volatile float yaw = -25f, pitch = 18f, distance = 8.5f;
        private volatile int sceneMode = SCENE_CHARACTER;
        private volatile int skinPaletteIndex = 1;
        private volatile int shirtPaletteIndex = 0;

        void setSceneMode(int mode) {
            sceneMode = mode;
        }

        void setSkinPalette(int index) {
            skinPaletteIndex = Math.max(0, Math.min(SKIN_PALETTES.length - 1, index));
        }

        void setShirtPalette(int index) {
            shirtPaletteIndex = Math.max(0, Math.min(SHIRT_PALETTES.length - 1, index));
        }
        private volatile boolean failed;

        void orbit(float dx, float dy) {
            yaw = (yaw + dx) % 360f;
            pitch = Math.max(-12f, Math.min(68f, pitch + dy));
        }

        void zoom(float delta) {
            distance = Math.max(4.2f, Math.min(17f, distance + delta));
        }

        private FloatBuffer buffer(float[] values) {
            FloatBuffer result = ByteBuffer.allocateDirect(values.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            result.put(values).position(0);
            return result;
        }

        private FloatBuffer makeGrid() {
            float[] lines = new float[17 * 4 * 3];
            int p = 0;
            for (int i = -8; i <= 8; i++) {
                lines[p++] = i; lines[p++] = -1.145f; lines[p++] = -8;
                lines[p++] = i; lines[p++] = -1.145f; lines[p++] = 8;
                lines[p++] = -8; lines[p++] = -1.145f; lines[p++] = i;
                lines[p++] = 8; lines[p++] = -1.145f; lines[p++] = i;
            }
            return buffer(lines);
        }

        private int compile(int type, String source) {
            int shader = GLES20.glCreateShader(type);
            if (shader == 0) throw new IllegalStateException("OpenGL shader جوړ نه شو");
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] status = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
            if (status[0] == 0) {
                String message = GLES20.glGetShaderInfoLog(shader);
                GLES20.glDeleteShader(shader);
                throw new IllegalStateException("Shader: " + message);
            }
            return shader;
        }

        @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            android.util.Log.i("MovieStudio3D", "GL surface created; version="
                    + GLES20.glGetString(GLES20.GL_VERSION) + "; renderer="
                    + GLES20.glGetString(GLES20.GL_RENDERER));
            failed = false;
            program = 0;
            try {
                GLES20.glClearColor(0.42f, 0.57f, 0.72f, 1f);
                GLES20.glClearDepthf(1f);
                GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                GLES20.glDepthFunc(GLES20.GL_LEQUAL);
                GLES20.glDisable(GLES20.GL_CULL_FACE);

                cubeBuffer = buffer(CUBE);
                headBuffer = buffer(HEAD);
                floorBuffer = buffer(FLOOR);
                gridBuffer = makeGrid();

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
                if (status[0] == 0) {
                    String message = GLES20.glGetProgramInfoLog(program);
                    GLES20.glDeleteProgram(program);
                    program = 0;
                    throw new IllegalStateException("Program: " + message);
                }
                positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
                matrixHandle = GLES20.glGetUniformLocation(program, "uMvp");
                colorHandle = GLES20.glGetUniformLocation(program, "uColor");
                if (positionHandle < 0 || matrixHandle < 0 || colorHandle < 0)
                    throw new IllegalStateException("د 3D shader ځایونه ونه موندل شول");
            } catch (Throwable error) {
                failed = true;
                android.util.Log.e("MovieStudio3D", "3D surface initialization failed", error);
            }
        }

        @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
            if (width <= 0 || height <= 0) return;
            GLES20.glViewport(0, 0, width, height);
            Matrix.perspectiveM(projection, 0, 45f,
                    (float) width / (float) height, 0.1f, 60f);
        }

        private void draw(FloatBuffer vertices, int mode, int firstVertex, int count,
                          float[] color, float x, float y, float z,
                          float sx, float sy, float sz) {
            draw(vertices, mode, firstVertex, count, color,
                    x, y, z, sx, sy, sz, 0f);
        }

        private void draw(FloatBuffer vertices, int mode, int firstVertex, int count,
                          float[] color, float x, float y, float z,
                          float sx, float sy, float sz, float rotationZ) {
            Matrix.setIdentityM(model, 0);
            Matrix.translateM(model, 0, x, y, z);
            if (rotationZ != 0f) Matrix.rotateM(model, 0, rotationZ, 0f, 0f, 1f);
            Matrix.scaleM(model, 0, sx, sy, sz);
            Matrix.multiplyMM(mvp, 0, vp, 0, model, 0);
            GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvp, 0);
            GLES20.glUniform4fv(colorHandle, 1, color, 0);
            vertices.position(firstVertex * 3);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
                    false, 12, vertices);
            GLES20.glDrawArrays(mode, 0, count);
        }

        /** Draws an actual low-poly humanoid from reusable 3D primitive meshes. */
        private void drawCharacter() {
            final float[] skin = SKIN_PALETTES[skinPaletteIndex];
            final float[] shirt = SHIRT_PALETTES[shirtPaletteIndex];
            final float[] shirtLight = SHIRT_LIGHT_PALETTES[shirtPaletteIndex];
            final int triangles = GLES20.GL_TRIANGLES;

            // Legs and boots.
            draw(cubeBuffer, triangles, 0, 36, TROUSERS_COLOR,
                    -0.17f, -0.68f, 0f, 0.16f, 0.30f, 0.17f, 2f);
            draw(cubeBuffer, triangles, 0, 36, TROUSERS_COLOR,
                     0.17f, -0.68f, 0f, 0.16f, 0.30f, 0.17f, -2f);
            draw(cubeBuffer, triangles, 0, 36, TROUSERS_COLOR,
                    -0.17f, -0.96f, 0f, 0.135f, 0.25f, 0.145f);
            draw(cubeBuffer, triangles, 0, 36, TROUSERS_COLOR,
                     0.17f, -0.96f, 0f, 0.135f, 0.25f, 0.145f);
            draw(cubeBuffer, triangles, 0, 36, BOOTS_COLOR,
                    -0.17f, -1.105f, 0.075f, 0.18f, 0.075f, 0.27f);
            draw(cubeBuffer, triangles, 0, 36, BOOTS_COLOR,
                     0.17f, -1.105f, 0.075f, 0.18f, 0.075f, 0.27f);

            // Waist, torso and neck.
            draw(cubeBuffer, triangles, 0, 36, TROUSERS_COLOR,
                    0f, -0.43f, 0f, 0.31f, 0.19f, 0.21f);
            draw(cubeBuffer, triangles, 0, 36, shirt,
                    0f, 0.06f, 0f, 0.38f, 0.49f, 0.23f);
            draw(cubeBuffer, triangles, 0, 36, shirtLight,
                    0f, 0.35f, 0.205f, 0.22f, 0.045f, 0.025f);
            draw(cubeBuffer, triangles, 0, 36, skin,
                    0f, 0.62f, 0f, 0.12f, 0.15f, 0.12f);

            // Arms are separate 3D pieces; the rotation is around each part's centre.
            draw(cubeBuffer, triangles, 0, 36, shirt,
                    -0.48f, 0.08f, 0f, 0.135f, 0.36f, 0.14f, 17f);
            draw(cubeBuffer, triangles, 0, 36, shirt,
                     0.48f, 0.08f, 0f, 0.135f, 0.36f, 0.14f, -17f);
            draw(cubeBuffer, triangles, 0, 36, shirt,
                    -0.60f, -0.30f, 0f, 0.115f, 0.30f, 0.12f, 6f);
            draw(cubeBuffer, triangles, 0, 36, shirt,
                     0.60f, -0.30f, 0f, 0.115f, 0.30f, 0.12f, -6f);
            draw(cubeBuffer, triangles, 0, 36, skin,
                    -0.64f, -0.55f, 0f, 0.13f, 0.13f, 0.13f);
            draw(cubeBuffer, triangles, 0, 36, skin,
                     0.64f, -0.55f, 0f, 0.13f, 0.13f, 0.13f);

            // Low-poly head, hair and simple facial details.
            draw(headBuffer, triangles, 0, 24, skin,
                    0f, 0.94f, 0f, 0.27f, 0.34f, 0.25f);
            draw(cubeBuffer, triangles, 0, 36, HAIR_COLOR,
                    0f, 1.205f, -0.015f, 0.245f, 0.075f, 0.235f);
            draw(cubeBuffer, triangles, 0, 36, EYE_COLOR,
                    -0.09f, 0.98f, 0.225f, 0.026f, 0.032f, 0.018f);
            draw(cubeBuffer, triangles, 0, 36, EYE_COLOR,
                     0.09f, 0.98f, 0.225f, 0.026f, 0.032f, 0.018f);
            draw(cubeBuffer, triangles, 0, 36, skin,
                    0f, 0.91f, 0.255f, 0.035f, 0.055f, 0.035f);
        }

        @Override public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            if (failed || program == 0) return;
            try {
                double yawRadians = Math.toRadians(yaw);
                double pitchRadians = Math.toRadians(pitch);
                float horizontal = (float) (Math.cos(pitchRadians) * distance);
                float eyeX = (float) (Math.sin(yawRadians) * horizontal);
                float eyeY = (float) (Math.sin(pitchRadians) * distance);
                float eyeZ = (float) (Math.cos(yawRadians) * horizontal);
                Matrix.setLookAtM(view, 0, eyeX, eyeY + 0.15f, eyeZ,
                        0f, -0.15f, 0f, 0f, 1f, 0f);
                Matrix.multiplyMM(vp, 0, projection, 0, view, 0);

                GLES20.glUseProgram(program);
                GLES20.glEnableVertexAttribArray(positionHandle);
                draw(floorBuffer, GLES20.GL_TRIANGLES, 0, 6, FLOOR_COLOR,
                        0f, 0f, 0f, 1f, 1f, 1f);
                draw(gridBuffer, GLES20.GL_LINES, 0, 17 * 4, GRID_COLOR,
                        0f, 0f, 0f, 1f, 1f, 1f);
                if (sceneMode == SCENE_CHARACTER) {
                    drawCharacter();
                } else {
                    for (int face = 0; face < 6; face++) {
                        draw(cubeBuffer, GLES20.GL_TRIANGLES, face * 6, 6, FACE_COLORS[face],
                                0f, 0f, 0f, 0.85f, 0.85f, 0.85f);
                    }
                }
                GLES20.glDisableVertexAttribArray(positionHandle);
            } catch (Throwable error) {
                failed = true;
                android.util.Log.e("MovieStudio3D", "3D frame rendering failed", error);
            }
        }
    }
}
