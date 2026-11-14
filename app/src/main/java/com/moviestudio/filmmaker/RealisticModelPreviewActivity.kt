package com.moviestudio.filmmaker

import android.app.Activity
import android.app.ActivityManager
import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceView
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.filament.Skybox
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel

/**
 * Physically based GLB viewer. Textures, mesh detail, skinning and embedded animations
 * come from the imported model; this screen does not fabricate a cartoon replacement.
 */
class RealisticModelPreviewActivity : Activity() {
    private var modelViewer: ModelViewer? = null
    private var userX = 0f
    private var userY = 0f
    private var userZ = 0f
    private var userScale = 1f
    private var userRotationX = 0f
    private var userRotationY = 0f
    private var userRotationZ = 0f
    private var animationStartTimeNanos = 0L
    private var currentAnimationIndex = 0
    private var animationPlaying = true
    private var animationStatus: android.widget.TextView? = null
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val viewer = modelViewer ?: return
            val animator = viewer.animator
            if (animationPlaying && animator != null && animator.animationCount > 0) {
                if (animationStartTimeNanos == 0L) animationStartTimeNanos = frameTimeNanos
                val elapsedSeconds = (frameTimeNanos - animationStartTimeNanos) / 1_000_000_000f
                val duration = animator.getAnimationDuration(currentAnimationIndex)
                val animationTime = if (duration > 0f) elapsedSeconds % duration else elapsedSeconds
                animator.applyAnimation(currentAnimationIndex, animationTime)
                animator.updateBoneMatrices()
            }
            viewer.render(frameTimeNanos)
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra(EXTRA_MODEL_PATH).orEmpty()
        val file = File(path)
        if (!file.isFile) {
            Toast.makeText(this, "د GLB ماډل فایل ونه موندل شو", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val deviceInfo = (getSystemService(ACTIVITY_SERVICE) as ActivityManager).deviceConfigurationInfo
        if (deviceInfo == null || deviceInfo.reqGlEsVersion < 0x00030000) {
            Toast.makeText(this, "د حقیقي 3D لید لپاره OpenGL ES 3.0 ته اړتیا ده", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        try {
            Utils.init()
            val surface = SurfaceView(this)
            val viewer = ModelViewer(surface)
            modelViewer = viewer
            surface.setOnTouchListener(viewer)
            val frame = FrameLayout(this)
            frame.addView(surface, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            ))
            setContentView(frame)
            viewer.scene.skybox = Skybox.Builder()
                .color(0.12f, 0.16f, 0.22f, 1.0f)
                .build(viewer.engine)
            require(file.length() > 12L) { "GLB فایل تش دی" }
            val glbBuffer = FileInputStream(file).channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, 0L, file.length())
            }
            viewer.loadModelGlb(glbBuffer)
            viewer.transformToUnitCube()
            check(viewer.asset != null) { "د GLB ماډل لوستل ناکام شول" }
            userX = intent.getFloatExtra(EXTRA_X, 0f)
            userY = intent.getFloatExtra(EXTRA_Y, 0f)
            userZ = intent.getFloatExtra(EXTRA_Z, 0f)
            userScale = intent.getFloatExtra(EXTRA_SCALE, 1f).coerceIn(0.25f, 3f)
            userRotationX = intent.getFloatExtra(EXTRA_ROTATION_X, 0f)
            userRotationY = intent.getFloatExtra(EXTRA_ROTATION_Y, 0f)
            userRotationZ = intent.getFloatExtra(EXTRA_ROTATION_Z, 0f)
            applyObjectTransform(viewer)
            val resetCamera = Button(this).apply {
                text = "کمره بېرته برابره کړه"
                isAllCaps = false
                setOnClickListener { recreate() }
            }
            frame.addView(resetCamera, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply { topMargin = 18; marginEnd = 18 })

            val controls = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(10, 6, 10, 6)
                setBackgroundColor(0xCC17243A.toInt())
            }
            fun controlButton(text: String, action: () -> Unit) =
                Button(this).apply { this.text = text; isAllCaps = false; setOnClickListener { action() } }
            controls.addView(controlButton("مخکینی") { selectAnimation(-1) })
            val statusLabel = android.widget.TextView(this).apply {
                setTextColor(android.graphics.Color.WHITE)
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(10, 0, 10, 0)
            }
            animationStatus = statusLabel
            controls.addView(statusLabel, android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            controls.addView(controlButton("بل حرکت") { selectAnimation(1) })
            controls.addView(controlButton("ودرول / چلول") {
                val count = viewer.animator?.animationCount ?: 0
                if (count > 0) {
                    animationPlaying = !animationPlaying
                    animationStartTimeNanos = 0L
                    updateAnimationStatus()
                }
            })
            frame.addView(controls, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply { bottomMargin = 20 })
            updateAnimationStatus()
            title = intent.getStringExtra(EXTRA_MODEL_NAME) ?: "واقعي 3D ماډل"
        } catch (error: Exception) {
            Toast.makeText(this, error.message ?: "د 3D ماډل پرانیستل ناکام شول", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun selectAnimation(delta: Int) {
        val animator = modelViewer?.animator
        if (animator == null || animator.animationCount <= 0) {
            Toast.makeText(this, "په دې GLB ماډل کې ثبت شوی skeletal animation نشته", Toast.LENGTH_LONG).show()
            return
        }
        val count = animator.animationCount
        currentAnimationIndex = (currentAnimationIndex + delta + count) % count
        animationStartTimeNanos = 0L
        animator.applyAnimation(currentAnimationIndex, 0f)
        animator.updateBoneMatrices()
        updateAnimationStatus()
    }

    private fun updateAnimationStatus() {
        val count = modelViewer?.animator?.animationCount ?: 0
        animationStatus?.text = if (count == 0) {
            "حرکت نشته"
        } else {
            "حرکت " + (currentAnimationIndex + 1) + "/" + count +
                if (animationPlaying) " • چلېږي" else " • درېدلی"
        }
    }

    private fun applyObjectTransform(viewer: ModelViewer) {
        val asset = viewer.asset ?: return
        val manager = viewer.engine.transformManager
        val instance = manager.getInstance(asset.root)
        val matrix = FloatArray(16)
        android.opengl.Matrix.setRotateEulerM(
            matrix, 0, userRotationX, userRotationY, userRotationZ
        )
        for (column in 0..2) for (row in 0..2) matrix[column * 4 + row] *= userScale
        matrix[12] = userX
        matrix[13] = userY
        matrix[14] = userZ
        matrix[15] = 1f
        manager.setTransform(instance, matrix)
    }

    override fun onResume() {
        super.onResume()
        if (modelViewer != null) Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onPause() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        animationStartTimeNanos = 0L
        super.onPause()
    }

    companion object {
        const val EXTRA_MODEL_PATH = "model_path"
        const val EXTRA_MODEL_NAME = "model_name"
        const val EXTRA_X = "model_x"
        const val EXTRA_Y = "model_y"
        const val EXTRA_Z = "model_z"
        const val EXTRA_SCALE = "model_scale"
        const val EXTRA_ROTATION_X = "model_rotation_x"
        const val EXTRA_ROTATION_Y = "model_rotation_y"
        const val EXTRA_ROTATION_Z = "model_rotation_z"
    }
}
