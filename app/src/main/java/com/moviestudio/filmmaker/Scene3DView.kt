package com.moviestudio.filmmaker

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightweight real-time 3D viewport built with Android OpenGL ES 2.0.
 * No network, AI video service, or extra rendering dependency is required.
 */
class Scene3DView(context: Context, private val sceneType: String) : GLSurfaceView(context) {
    private val sceneRenderer = SceneRenderer(sceneType)

    init {
        setEGLContextClientVersion(2)
        setRenderer(sceneRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    private var lastX = 0f
    private var lastY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                sceneRenderer.rotate(dx * 0.45f, dy * 0.20f)
                lastX = event.x
                lastY = event.y
                return true
            }
        }
        return true
    }
}

private class SceneRenderer(private val sceneType: String) : GLSurfaceView.Renderer {
    @Volatile private var yaw = -18f
    @Volatile private var pitch = 8f

    private var program = 0
    private var positionHandle = 0
    private var normalHandle = 0
    private var mvpHandle = 0
    private var colorHandle = 0
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProjection = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    private lateinit var sphere: FloatBuffer
    private lateinit var cube: FloatBuffer
    private var sphereCount = 0
    private var cubeCount = 0

    fun rotate(dx: Float, dy: Float) {
        yaw += dx
        pitch = (pitch + dy).coerceIn(-35f, 45f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.48f, 0.68f, 0.88f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        sphere = makeSphere(18, 24)
        cube = makeCube()
        sphereCount = sphere.limit() / 6
        cubeCount = cube.limit() / 6

        val vertexShader = """
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            uniform mat4 uMvp;
            varying float vShade;
            void main() {
                gl_Position = uMvp * vec4(aPosition, 1.0);
                vec3 lightDir = normalize(vec3(-0.35, 0.8, 0.55));
                vShade = 0.48 + 0.52 * max(dot(normalize(aNormal), lightDir), 0.0);
            }
        """.trimIndent()
        val fragmentShader = """
            precision mediump float;
            uniform vec4 uColor;
            varying float vShade;
            void main() {
                gl_FragColor = vec4(uColor.rgb * vShade, uColor.a);
            }
        """.trimIndent()
        program = GLES20.glCreateProgram()
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        normalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        mvpHandle = GLES20.glGetUniformLocation(program, "uMvp")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        Matrix.perspectiveM(projection, 0, 43f, width.toFloat() / height.coerceAtLeast(1), 0.1f, 80f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        Matrix.setLookAtM(view, 0, 0f, 3.4f, 8.5f, 0f, 1f, 0f, 0f, 1f, 0f)
        val rotation = FloatArray(16)
        Matrix.setIdentityM(rotation, 0)
        Matrix.rotateM(rotation, 0, pitch, 1f, 0f, 0f)
        Matrix.rotateM(rotation, 0, yaw, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjection, 0, view, 0, rotation, 0)
        val combined = FloatArray(16)
        Matrix.multiplyMM(combined, 0, projection, 0, viewProjection, 0)
        System.arraycopy(combined, 0, viewProjection, 0, 16)

        GLES20.glUseProgram(program)
        drawSphere(-0.3f, -0.12f, 0f, 8.2f, 0.12f, 8.2f, 0.24f, 0.48f, 0.22f)
        drawEnvironment()
        drawCharacter()
    }

    private fun drawEnvironment() {
        // Distant hills and a simple sky landmark make the 3D scene readable.
        drawSphere(-3.4f, 0.2f, -3.0f, 2.8f, 1.5f, 1.5f, 0.30f, 0.48f, 0.30f)
        drawSphere(3.2f, 0.15f, -3.8f, 3.0f, 1.8f, 1.6f, 0.25f, 0.43f, 0.28f)
        drawSphere(0f, 4.2f, -8f, 0.55f, 0.55f, 0.55f, 1.0f, 0.78f, 0.22f)

        when {
            sceneType == "environment_city" -> {
                drawCube(-2.8f, 1.0f, -2.4f, 1.0f, 2.1f, 0.9f, 0.72f, 0.72f, 0.78f)
                drawCube(-1.4f, 0.7f, -3.1f, 0.8f, 1.5f, 0.8f, 0.82f, 0.55f, 0.35f)
                drawCube(2.4f, 1.3f, -2.8f, 1.0f, 2.7f, 0.9f, 0.55f, 0.63f, 0.78f)
                drawCube(3.7f, 0.8f, -3.5f, 0.8f, 1.7f, 0.8f, 0.76f, 0.70f, 0.55f)
                drawCube(0f, 0.03f, -1.8f, 1.2f, 0.05f, 7f, 0.20f, 0.21f, 0.23f)
            }
            sceneType == "environment_room" -> {
                drawCube(0f, 1.2f, -3.4f, 5.2f, 2.5f, 0.12f, 0.76f, 0.66f, 0.52f)
                drawCube(-2.1f, 0.55f, -2.0f, 1.0f, 0.8f, 0.8f, 0.45f, 0.24f, 0.15f)
                drawCube(2.1f, 0.7f, -2.4f, 0.8f, 1.1f, 0.8f, 0.40f, 0.28f, 0.20f)
            }
            sceneType == "environment_forest" -> {
                for (i in -2..2) {
                    drawCube(i * 1.35f, 0.65f, -2.7f - (i % 2) * 0.7f, 0.16f, 1.3f, 0.16f, 0.34f, 0.20f, 0.10f)
                    drawSphere(i * 1.35f, 1.75f, -2.7f - (i % 2) * 0.7f, 0.72f, 0.95f, 0.72f, 0.10f, 0.42f, 0.16f)
                }
            }
            sceneType == "environment_desert" -> {
                drawSphere(-2.8f, 0.08f, -3f, 3.0f, 0.65f, 2.0f, 0.78f, 0.60f, 0.34f)
                drawSphere(2.8f, 0.05f, -4f, 3.2f, 0.8f, 2.1f, 0.85f, 0.68f, 0.40f)
            }
            else -> {
                // Village scene: low houses, doors and roofs.
                drawCube(-2.6f, 0.55f, -2.8f, 1.65f, 1.05f, 1.3f, 0.73f, 0.56f, 0.36f)
                drawCube(-2.6f, 1.28f, -2.8f, 1.9f, 0.22f, 1.5f, 0.42f, 0.25f, 0.16f)
                drawCube(2.8f, 0.5f, -3.4f, 1.5f, 0.95f, 1.2f, 0.78f, 0.67f, 0.48f)
                drawCube(2.8f, 1.16f, -3.4f, 1.75f, 0.2f, 1.4f, 0.43f, 0.26f, 0.17f)
            }
        }
    }

    private fun drawCharacter() {
        // A complete, visible low-poly humanoid assembled from shaded 3D meshes.
        val skinR = 0.72f; val skinG = 0.48f; val skinB = 0.32f
        drawSphere(0f, 1.28f, 0f, 0.43f, 0.63f, 0.27f, 0.16f, 0.30f, 0.72f)
        drawSphere(0f, 2.12f, 0f, 0.27f, 0.29f, 0.25f, skinR, skinG, skinB)
        drawSphere(0f, 2.34f, -0.015f, 0.275f, 0.12f, 0.26f, 0.12f, 0.075f, 0.045f)
        drawSphere(-0.105f, 2.14f, 0.225f, 0.025f, 0.025f, 0.018f, 0.04f, 0.035f, 0.03f)
        drawSphere(0.105f, 2.14f, 0.225f, 0.025f, 0.025f, 0.018f, 0.04f, 0.035f, 0.03f)
        drawSphere(-0.56f, 1.34f, 0f, 0.15f, 0.48f, 0.16f, 0.16f, 0.30f, 0.72f)
        drawSphere(0.56f, 1.34f, 0f, 0.15f, 0.48f, 0.16f, 0.16f, 0.30f, 0.72f)
        drawSphere(-0.59f, 0.84f, 0.02f, 0.13f, 0.14f, 0.13f, skinR, skinG, skinB)
        drawSphere(0.59f, 0.84f, 0.02f, 0.13f, 0.14f, 0.13f, skinR, skinG, skinB)
        drawSphere(-0.21f, 0.48f, 0f, 0.17f, 0.52f, 0.18f, 0.12f, 0.14f, 0.20f)
        drawSphere(0.21f, 0.48f, 0f, 0.17f, 0.52f, 0.18f, 0.12f, 0.14f, 0.20f)
        drawSphere(-0.22f, 0.12f, 0.10f, 0.19f, 0.12f, 0.30f, 0.08f, 0.08f, 0.09f)
        drawSphere(0.22f, 0.12f, 0.10f, 0.19f, 0.12f, 0.30f, 0.08f, 0.08f, 0.09f)
    }

    private fun drawSphere(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        Matrix.scaleM(model, 0, sx, sy, sz)
        drawMesh(sphere, sphereCount, r, g, b)
    }

    private fun drawCube(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        Matrix.scaleM(model, 0, sx, sy, sz)
        drawMesh(cube, cubeCount, r, g, b)
    }

    private fun drawMesh(buffer: FloatBuffer, count: Int, r: Float, g: Float, b: Float) {
        Matrix.multiplyMM(mvp, 0, viewProjection, 0, model, 0)
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0)
        GLES20.glUniform4f(colorHandle, r, g, b, 1f)
        buffer.position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(normalHandle)
        buffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 24, buffer)
        buffer.position(3)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 24, buffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val message = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException("3D shader error: $message")
        }
        return shader
    }

    private fun makeSphere(stacks: Int, slices: Int): FloatBuffer {
        val data = ArrayList<Float>()
        fun vertex(lat: Float, lon: Float) {
            val x = cos(lat) * cos(lon)
            val y = sin(lat)
            val z = cos(lat) * sin(lon)
            data.add(x); data.add(y); data.add(z)
            data.add(x); data.add(y); data.add(z)
        }
        for (i in 0 until stacks) {
            val lat0 = -Math.PI.toFloat() / 2f + Math.PI.toFloat() * i / stacks
            val lat1 = -Math.PI.toFloat() / 2f + Math.PI.toFloat() * (i + 1) / stacks
            for (j in 0 until slices) {
                val lon0 = 2f * Math.PI.toFloat() * j / slices
                val lon1 = 2f * Math.PI.toFloat() * (j + 1) / slices
                vertex(lat0, lon0); vertex(lat1, lon0); vertex(lat1, lon1)
                vertex(lat0, lon0); vertex(lat1, lon1); vertex(lat0, lon1)
            }
        }
        return floatBuffer(data.toFloatArray())
    }

    private fun makeCube(): FloatBuffer {
        val data = ArrayList<Float>()
        fun face(nx: Float, ny: Float, nz: Float, vararg p: Float) {
            val order = intArrayOf(0, 1, 2, 0, 2, 3)
            for (idx in order) {
                val k = idx * 3
                data.add(p[k]); data.add(p[k + 1]); data.add(p[k + 2])
                data.add(nx); data.add(ny); data.add(nz)
            }
        }
        face(0f, 0f, 1f, -0.5f,-0.5f,0.5f, 0.5f,-0.5f,0.5f, 0.5f,0.5f,0.5f, -0.5f,0.5f,0.5f)
        face(0f, 0f, -1f, 0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f, 0.5f,0.5f,-0.5f)
        face(1f, 0f, 0f, 0.5f,-0.5f,0.5f, 0.5f,-0.5f,-0.5f, 0.5f,0.5f,-0.5f, 0.5f,0.5f,0.5f)
        face(-1f, 0f, 0f, -0.5f,-0.5f,-0.5f, -0.5f,-0.5f,0.5f, -0.5f,0.5f,0.5f, -0.5f,0.5f,-0.5f)
        face(0f, 1f, 0f, -0.5f,0.5f,0.5f, 0.5f,0.5f,0.5f, 0.5f,0.5f,-0.5f, -0.5f,0.5f,-0.5f)
        face(0f, -1f, 0f, -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f, -0.5f,-0.5f,0.5f)
        return floatBuffer(data.toFloatArray())
    }

    private fun floatBuffer(values: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(values)
            position(0)
        }
}