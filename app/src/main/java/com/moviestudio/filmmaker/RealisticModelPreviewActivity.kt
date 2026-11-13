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
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val viewer = modelViewer ?: return
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
            val resetCamera = Button(this).apply {
                text = "کمره بېرته برابره کړه"
                isAllCaps = false
                setOnClickListener { viewer.resetToDefaultState() }
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
    }
}
