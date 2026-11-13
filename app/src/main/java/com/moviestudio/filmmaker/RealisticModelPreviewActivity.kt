package com.moviestudio.filmmaker

import android.app.Activity
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
import java.nio.ByteBuffer

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
    private var animationStartTimeNanos = 0L
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val viewer = modelViewer ?: return
            val animator = viewer.animator
            if (animator != null && animator.animationCount > 0) {
                if (animationStartTimeNanos == 0L) animationStartTimeNanos = frameTimeNanos
                val elapsedSeconds = (frameTimeNanos - animationStartTimeNanos) / 1_000_000_000f
                val duration = animator.getAnimationDuration(0)
                val animationTime = if (duration > 0f) elapsedSeconds % duration else elapsedSeconds
                animator.applyAnimation(0, animationTime)
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
        try {
            Utils.init()
            val surface = SurfaceView(this)
            val viewer = ModelViewer(surface)
            modelViewer = viewer
            surface.setOnTouchListener(viewer)
            viewer.scene.skybox = Skybox.Builder()
                .color(0.12f, 0.16f, 0.22f, 1.0f)
                .build(viewer.engine)
            val bytes = file.readBytes()
            require(bytes.size > 12) { "GLB فایل تش دی" }
            viewer.loadModelGlb(ByteBuffer.wrap(bytes))
            viewer.transformToUnitCube()
            check(viewer.asset != null) { "د GLB ماډل لوستل ناکام شول" }
            userX = intent.getFloatExtra(EXTRA_X, 0f)
            userY = intent.getFloatExtra(EXTRA_Y, 0f)
            userZ = intent.getFloatExtra(EXTRA_Z, 0f)
            userScale = intent.getFloatExtra(EXTRA_SCALE, 1f).coerceIn(0.25f, 3f)
            applyObjectTransform(viewer)
            val resetCamera = Button(this).apply {
                text = "کمره بېرته برابره کړه"
                isAllCaps = false
                setOnClickListener { recreate() }
            }
            val frame = FrameLayout(this)
            frame.addView(surface, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            ))
            frame.addView(resetCamera, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply { topMargin = 18; marginEnd = 18 })
            setContentView(frame)
            title = intent.getStringExtra(EXTRA_MODEL_NAME) ?: "واقعي 3D ماډل"
        } catch (error: Exception) {
            Toast.makeText(this, error.message ?: "د 3D ماډل پرانیستل ناکام شول", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun applyObjectTransform(viewer: ModelViewer) {
        val asset = viewer.asset ?: return
        val manager = viewer.engine.transformManager
        val instance = manager.getInstance(asset.root)
        val matrix = manager.getTransform(instance, FloatArray(16))
        for (column in 0..2) for (row in 0..2) matrix[column * 4 + row] *= userScale
        matrix[12] += userX
        matrix[13] += userY
        matrix[14] += userZ
        manager.setTransform(instance, matrix)
    }

    override fun onResume() {
        super.onResume()
        if (modelViewer != null) Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onPause() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        super.onPause()
    }

    companion object {
        const val EXTRA_MODEL_PATH = "model_path"
        const val EXTRA_MODEL_NAME = "model_name"
        const val EXTRA_X = "model_x"
        const val EXTRA_Y = "model_y"
        const val EXTRA_Z = "model_z"
        const val EXTRA_SCALE = "model_scale"
    }
}
