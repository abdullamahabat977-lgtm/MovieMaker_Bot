package com.moviestudio.filmmaker;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Java-based lightweight 3D viewport.
 * Uses Android OpenGL ES 2.0; renders only when first shown or dragged
 * to reduce battery, heat and memory pressure on entry-level phones.
 */
public final class Scene3DView extends GLSurfaceView {
    private final SceneRenderer renderer;
    private float lastX, lastY;

    public Scene3DView(Context context, String sceneType) {
        super(context);
        setEGLContextClientVersion(2);
        // Opaque preview: avoid requiring an alpha EGL channel on older GPUs.
        setEGLConfigChooser(8, 8, 8, 0, 16, 0);
        renderer = new SceneRenderer(sceneType == null ? "environment_village" : sceneType);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setPreserveEGLContextOnPause(false);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastX;
                float dy = event.getY() - lastY;
                renderer.rotate(dx * 0.45f, dy * 0.20f);
                lastX = event.getX();
                lastY = event.getY();
                requestRender();
                return true;
            default:
                return true;
        }
    }

    @Override public void onResume() {
        super.onResume();
        requestRender();
    }

    private static final class SceneRenderer implements GLSurfaceView.Renderer {
        private final String sceneType;
        private int program, positionHandle, normalHandle, mvpHandle, colorHandle;
        private FloatBuffer sphere, cube;
        private int sphereCount, cubeCount;
        private final float[] projection = new float[16];
        private final float[] view = new float[16];
        private final float[] vp = new float[16];
        private final float[] model = new float[16];
        private final float[] mvp = new float[16];
        private volatile float yaw = -18f, pitch = 8f;
        private volatile boolean failed = false;

        SceneRenderer(String type) { sceneType = type; }

        void rotate(float dx, float dy) {
            yaw += dx;
            pitch = Math.max(-35f, Math.min(45f, pitch + dy));
        }

        @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            failed = false;
            try {
            GLES20.glClearColor(0.48f, 0.68f, 0.88f, 1f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            // Disable face culling: keeps every generated low-poly mesh visible
            // regardless of triangle winding on different Android GPU drivers.
            GLES20.glDisable(GLES20.GL_CULL_FACE);
            sphere = makeSphere(16, 20);
            cube = makeCube();
            sphereCount = sphere.limit() / 6;
            cubeCount = cube.limit() / 6;

            String vs =
                "attribute vec3 aPosition; attribute vec3 aNormal;" +
                "uniform mat4 uMvp; varying float vShade;" +
                "void main(){ gl_Position=uMvp*vec4(aPosition,1.0);" +
                "vec3 l=normalize(vec3(-0.35,0.8,0.55));" +
                "vShade=0.48+0.52*max(dot(normalize(aNormal),l),0.0);}";
            String fs =
                "precision mediump float; uniform vec4 uColor; varying float vShade;" +
                "void main(){ gl_FragColor=vec4(uColor.rgb*vShade,uColor.a);}";
            int v = compile(GLES20.GL_VERTEX_SHADER, vs);
            int f = compile(GLES20.GL_FRAGMENT_SHADER, fs);
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, v);
            GLES20.glAttachShader(program, f);
            GLES20.glLinkProgram(program);
            int[] status = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
            if (status[0] == 0) throw new IllegalStateException("3D link: " + GLES20.glGetProgramInfoLog(program));
            positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
            normalHandle = GLES20.glGetAttribLocation(program, "aNormal");
            mvpHandle = GLES20.glGetUniformLocation(program, "uMvp");
            colorHandle = GLES20.glGetUniformLocation(program, "uColor");
            } catch (Throwable error) {
                failed = true;
                android.util.Log.e("MovieStudio3D", "3D preview initialization failed", error);
                GLES20.glClearColor(0.16f, 0.20f, 0.29f, 1f);
            }
        }

        @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            Matrix.perspectiveM(projection, 0, 43f, (float) width / Math.max(1, height), 0.1f, 80f);
        }

        @Override public void onDrawFrame(GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            if (failed) return;
            try {
                Matrix.setLookAtM(view, 0, 0f, 3.4f, 8.5f, 0f, 1f, 0f, 0f, 1f, 0f);
                float[] rotation = new float[16];
                Matrix.setIdentityM(rotation, 0);
                Matrix.rotateM(rotation, 0, pitch, 1f, 0f, 0f);
                Matrix.rotateM(rotation, 0, yaw, 0f, 1f, 0f);
                float[] camera = new float[16];
                Matrix.multiplyMM(camera, 0, view, 0, rotation, 0);
                Matrix.multiplyMM(vp, 0, projection, 0, camera, 0);
                if (program == 0 || positionHandle < 0 || normalHandle < 0 ||
                    mvpHandle < 0 || colorHandle < 0) {
                    throw new IllegalStateException("3D shader handles are unavailable");
                }
                GLES20.glUseProgram(program);
                // Broad ground plane and a distant sun.
                sphere(-0.3f, -0.10f, -0.8f, 8.2f, 0.10f, 8.2f, 0.24f, 0.48f, 0.22f);
                sphere(0f, 4.2f, -8f, 0.55f, 0.55f, 0.55f, 1f, 0.78f, 0.22f);
                drawEnvironment();
                drawCharacter();
            } catch (Throwable error) {
                failed = true;
                android.util.Log.e("MovieStudio3D", "3D preview frame failed", error);
                GLES20.glClearColor(0.16f, 0.20f, 0.29f, 1f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            }
        }

        private void drawEnvironment() {
            if ("environment_city".equals(sceneType)) {
                box(-2.8f,1f,-2.4f,1f,2.1f,.9f,.72f,.72f,.78f);
                box(-1.4f,.7f,-3.1f,.8f,1.5f,.8f,.82f,.55f,.35f);
                box(2.4f,1.3f,-2.8f,1f,2.7f,.9f,.55f,.63f,.78f);
                box(3.7f,.8f,-3.5f,.8f,1.7f,.8f,.76f,.70f,.55f);
                box(0f,.02f,-1.8f,1.2f,.04f,7f,.20f,.21f,.23f);
            } else if ("environment_room".equals(sceneType)) {
                box(0f,1.2f,-3.4f,5.2f,2.5f,.12f,.76f,.66f,.52f);
                box(-2.1f,.55f,-2f,1f,.8f,.8f,.45f,.24f,.15f);
                box(2.1f,.7f,-2.4f,.8f,1.1f,.8f,.40f,.28f,.20f);
            } else if ("environment_forest".equals(sceneType)) {
                for (int i=-2;i<=2;i++) {
                    box(i*1.35f,.65f,-2.7f-(i%2)*.7f,.16f,1.3f,.16f,.34f,.20f,.10f);
                    sphere(i*1.35f,1.75f,-2.7f-(i%2)*.7f,.72f,.95f,.72f,.10f,.42f,.16f);
                }
            } else if ("environment_desert".equals(sceneType)) {
                sphere(-2.8f,.08f,-3f,3f,.65f,2f,.78f,.60f,.34f);
                sphere(2.8f,.05f,-4f,3.2f,.8f,2.1f,.85f,.68f,.40f);
            } else {
                // Afghan village: mud-brick houses with flat roofs.
                box(-2.6f,.55f,-2.8f,1.65f,1.05f,1.3f,.73f,.56f,.36f);
                box(-2.6f,1.28f,-2.8f,1.9f,.22f,1.5f,.42f,.25f,.16f);
                box(2.8f,.5f,-3.4f,1.5f,.95f,1.2f,.78f,.67f,.48f);
                box(2.8f,1.16f,-3.4f,1.75f,.2f,1.4f,.43f,.26f,.17f);
            }
            // Distant hills make the scene feel like an outdoor location.
            sphere(-3.4f,.2f,-5f,2.8f,1.5f,1.5f,.30f,.48f,.30f);
            sphere(3.2f,.15f,-5.8f,3f,1.8f,1.6f,.25f,.43f,.28f);
        }

        private void drawCharacter() {
            // Stylized but complete humanoid placeholder, assembled as 3D meshes.
            sphere(0f,1.28f,0f,.43f,.63f,.27f,.16f,.30f,.72f);
            sphere(0f,2.12f,0f,.27f,.29f,.25f,.72f,.48f,.32f);
            sphere(0f,2.34f,-.015f,.275f,.12f,.26f,.12f,.075f,.045f);
            sphere(-.105f,2.14f,.225f,.025f,.025f,.018f,.04f,.035f,.03f);
            sphere(.105f,2.14f,.225f,.025f,.025f,.018f,.04f,.035f,.03f);
            sphere(-.56f,1.34f,0f,.15f,.48f,.16f,.16f,.30f,.72f);
            sphere(.56f,1.34f,0f,.15f,.48f,.16f,.16f,.30f,.72f);
            sphere(-.59f,.84f,.02f,.13f,.14f,.13f,.72f,.48f,.32f);
            sphere(.59f,.84f,.02f,.13f,.14f,.13f,.72f,.48f,.32f);
            sphere(-.21f,.48f,0f,.17f,.52f,.18f,.12f,.14f,.20f);
            sphere(.21f,.48f,0f,.17f,.52f,.18f,.12f,.14f,.20f);
            sphere(-.22f,.12f,.10f,.19f,.12f,.30f,.08f,.08f,.09f);
            sphere(.22f,.12f,.10f,.19f,.12f,.30f,.08f,.08f,.09f);
        }

        private void sphere(float x,float y,float z,float sx,float sy,float sz,float r,float g,float b) {
            Matrix.setIdentityM(model,0); Matrix.translateM(model,0,x,y,z); Matrix.scaleM(model,0,sx,sy,sz);
            draw(sphere,sphereCount,r,g,b);
        }
        private void box(float x,float y,float z,float sx,float sy,float sz,float r,float g,float b) {
            Matrix.setIdentityM(model,0); Matrix.translateM(model,0,x,y,z); Matrix.scaleM(model,0,sx,sy,sz);
            draw(cube,cubeCount,r,g,b);
        }
        private void draw(FloatBuffer mesh,int count,float r,float g,float b) {
            Matrix.multiplyMM(mvp,0,vp,0,model,0);
            GLES20.glUniformMatrix4fv(mvpHandle,1,false,mvp,0);
            GLES20.glUniform4f(colorHandle,r,g,b,1f);
            mesh.position(0);
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glEnableVertexAttribArray(normalHandle);
            GLES20.glVertexAttribPointer(positionHandle,3,GLES20.GL_FLOAT,false,24,mesh);
            mesh.position(3);
            GLES20.glVertexAttribPointer(normalHandle,3,GLES20.GL_FLOAT,false,24,mesh);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
            mesh.position(0);
        }

        private int compile(int type,String source) {
            int shader=GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader,source); GLES20.glCompileShader(shader);
            int[] ok=new int[1]; GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,ok,0);
            if(ok[0]==0) throw new IllegalStateException("3D shader: "+GLES20.glGetShaderInfoLog(shader));
            return shader;
        }

        private FloatBuffer makeSphere(int stacks,int slices) {
            ArrayList<Float> d=new ArrayList<>();
            for(int i=0;i<stacks;i++) {
                float lat0=(float)(-Math.PI/2+Math.PI*i/stacks);
                float lat1=(float)(-Math.PI/2+Math.PI*(i+1)/stacks);
                for(int j=0;j<slices;j++) {
                    float lon0=(float)(2*Math.PI*j/slices), lon1=(float)(2*Math.PI*(j+1)/slices);
                    addVertex(d,lat0,lon0); addVertex(d,lat1,lon0); addVertex(d,lat1,lon1);
                    addVertex(d,lat0,lon0); addVertex(d,lat1,lon1); addVertex(d,lat0,lon1);
                }
            }
            return buffer(d);
        }
        private void addVertex(ArrayList<Float>d,float lat,float lon) {
            float x=(float)(Math.cos(lat)*Math.cos(lon)), y=(float)Math.sin(lat), z=(float)(Math.cos(lat)*Math.sin(lon));
            d.add(x);d.add(y);d.add(z);d.add(x);d.add(y);d.add(z);
        }
        private FloatBuffer makeCube() {
            ArrayList<Float>d=new ArrayList<>();
            face(d,0,0,1,new float[]{-.5f,-.5f,.5f,.5f,-.5f,.5f,.5f,.5f,.5f,-.5f,.5f,.5f});
            face(d,0,0,-1,new float[]{.5f,-.5f,-.5f,-.5f,-.5f,-.5f,-.5f,.5f,-.5f,.5f,.5f,-.5f});
            face(d,1,0,0,new float[]{.5f,-.5f,.5f,.5f,-.5f,-.5f,.5f,.5f,-.5f,.5f,.5f,.5f});
            face(d,-1,0,0,new float[]{-.5f,-.5f,-.5f,-.5f,-.5f,.5f,-.5f,.5f,.5f,-.5f,.5f,-.5f});
            face(d,0,1,0,new float[]{-.5f,.5f,.5f,.5f,.5f,.5f,.5f,.5f,-.5f,-.5f,.5f,-.5f});
            face(d,0,-1,0,new float[]{-.5f,-.5f,-.5f,.5f,-.5f,-.5f,.5f,-.5f,.5f,-.5f,-.5f,.5f});
            return buffer(d);
        }
        private void face(ArrayList<Float>d,float nx,float ny,float nz,float[]p) {
            int[] order={0,1,2,0,2,3};
            for(int idx:order) { int k=idx*3; d.add(p[k]);d.add(p[k+1]);d.add(p[k+2]);d.add(nx);d.add(ny);d.add(nz); }
        }
        private FloatBuffer buffer(ArrayList<Float>d) {
            ByteBuffer bb=ByteBuffer.allocateDirect(d.size()*4).order(ByteOrder.nativeOrder());
            FloatBuffer fb=bb.asFloatBuffer();
            for(Float v:d) fb.put(v);
            fb.position(0); return fb;
        }
    }
}