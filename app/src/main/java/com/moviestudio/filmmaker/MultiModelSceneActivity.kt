package com.moviestudio.filmmaker

import android.app.Activity
import android.app.ActivityManager
import android.os.Bundle
import android.view.Choreographer
import android.view.Gravity
import android.view.SurfaceView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.filament.EntityManager
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel

/**
 * Displays every imported GLB model in the same Filament scene.
 * Models retain their original materials, textures, meshes and embedded animation.
 */
class MultiModelSceneActivity : Activity() {
    private data class SceneModel(
        val name: String,
        val asset: FilamentAsset,
        val resources: ResourceLoader,
        val animator: com.google.android.filament.gltfio.Animator?,
        var lightsAdded: Boolean = false,
        var animationStartNanos: Long = 0L
    )

    private var viewer: ModelViewer? = null
    private var assetLoader: AssetLoader? = null
    private var materialProvider: UbershaderProvider? = null
    private val models = mutableListOf<SceneModel>()
    private var loadingLabel: TextView? = null
    private var projectId: String = ""
    private var framePosted = false
    private var omittedModelCount = 0

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val modelViewer = viewer ?: return
            var readyCount = 0
            models.forEach { model ->
                model.resources.asyncUpdateLoad()
                var entity = model.asset.popRenderable()
                while (entity != 0) {
                    modelViewer.scene.addEntities(intArrayOf(entity))
                    entity = model.asset.popRenderable()
                }
                if (!model.lightsAdded && model.resources.asyncGetLoadProgress() >= 1f) {
                    modelViewer.scene.addEntities(model.asset.lightEntities)
                    model.lightsAdded = true
                }
                if (model.lightsAdded) {
                    readyCount++
                    val animator = model.animator
                    if (animator != null && animator.animationCount > 0) {
                        if (model.animationStartNanos == 0L) model.animationStartNanos = frameTimeNanos
                        val elapsed = (frameTimeNanos - model.animationStartNanos) / 1_000_000_000f
                        val duration = animator.getAnimationDuration(0)
                        val time = if (duration > 0f) elapsed % duration else elapsed
                        animator.applyAnimation(0, time)
                        animator.updateBoneMatrices()
                    }
                }
            }
            loadingLabel?.text = "د 3D توکو چمتووالی: $readyCount / ${models.size}" +
                if (omittedModelCount > 0) " • $omittedModelCount نور د حافظې د حد له امله نه ښودل کېږي" else ""
            modelViewer.render(frameTimeNanos)
            Choreographer.getInstance().postFrameCallback(this)
            framePosted = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectId = intent.getStringExtra(EXTRA_PROJECT_ID).orEmpty()
        if (projectId.isBlank()) {
            finish()
            return
        }
        val deviceInfo = (getSystemService(ACTIVITY_SERVICE) as ActivityManager).deviceConfigurationInfo
        if (deviceInfo == null || deviceInfo.reqGlEsVersion < 0x00030000) {
            Toast.makeText(this, "د حقیقي 3D صحنې لپاره OpenGL ES 3.0 ته اړتیا ده", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        try {
            Utils.init()
            val surface = SurfaceView(this)
            val modelViewer = ModelViewer(surface)
            viewer = modelViewer
            surface.setOnTouchListener(modelViewer)
            modelViewer.scene.skybox = com.google.android.filament.Skybox.Builder()
                .color(0.12f, 0.16f, 0.22f, 1.0f)
                .build(modelViewer.engine)

            val frame = FrameLayout(this)
            frame.addView(surface, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            ))
            loadingLabel = TextView(this).apply {
                text = "د حقیقي 3D ماډلونه لوډ کېږي..."
                setTextColor(android.graphics.Color.WHITE)
                textSize = 13f
                setPadding(14, 10, 14, 10)
                setBackgroundColor(0xAA17243A.toInt())
            }
            frame.addView(loadingLabel, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.START
            ).apply { topMargin = 16; marginStart = 16 })
            val reset = Button(this).apply {
                text = "کمره بېرته برابره کړه"
                isAllCaps = false
                setOnClickListener { recreate() }
            }
            frame.addView(reset, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply { topMargin = 16; marginEnd = 16 })
            setContentView(frame)
            title = "د صحنې حقیقي 3D لید"
        } catch (error: Exception) {
            Toast.makeText(this, error.message ?: "د 3D صحنې پرانیستل ناکام شول", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewer != null && models.isEmpty()) loadProjectModels()
        if (viewer != null && !framePosted) {
            Choreographer.getInstance().postFrameCallback(frameCallback)
            framePosted = true
        }
    }

    private fun loadProjectModels() {
        val modelViewer = viewer ?: return
        try {
            val objects = ProjectStore(this).listSceneObjects(projectId)
                .filter { it.optString("modelPath").isNotBlank() }
            omittedModelCount = 0
            if (objects.isEmpty()) {
                loadingLabel?.text = "په دې پروژه کې حقیقي GLB ماډل نشته."
                return
            }
            val projectFolder = ProjectStore(this).projectDirectory(projectId).canonicalFile
            val candidates = mutableListOf<Pair<org.json.JSONObject, File>>()
            var totalBytes = 0L
            for (item in objects) {
                if (candidates.size >= MAX_MODELS) break
                val file = File(projectFolder, item.optString("modelPath")).canonicalFile
                if (!file.path.startsWith(projectFolder.path + File.separator) || !file.isFile) continue
                if (totalBytes + file.length() > MAX_TOTAL_BYTES) continue
                totalBytes += file.length()
                candidates.add(item to file)
            }
            omittedModelCount = objects.size - candidates.size
            if (candidates.isEmpty()) {
                loadingLabel?.text = "د ښودلو لپاره سم GLB فایل ونه موندل شو."
                return
            }

            materialProvider = UbershaderProvider(modelViewer.engine)
            assetLoader = AssetLoader(modelViewer.engine, materialProvider!!, EntityManager.get())
            candidates.forEach { (item, file) ->
                var resourceLoader: ResourceLoader? = null
                var asset: FilamentAsset? = null
                try {
                    val buffer = FileInputStream(file).channel.use { channel ->
                        channel.map(FileChannel.MapMode.READ_ONLY, 0L, file.length())
                    }
                    val createdAsset = assetLoader!!.createAsset(buffer)
                        ?: throw IllegalArgumentException("د ${item.optString("name")} GLB نه لوستل کېږي")
                    asset = createdAsset
                    val createdResources = ResourceLoader(modelViewer.engine, true)
                    resourceLoader = createdResources
                    check(createdResources.asyncBeginLoad(createdAsset)) { "د ${item.optString("name")} ماډل لوډ نه شو" }
                    applySceneTransform(modelViewer, createdAsset, item)
                    val animator = createdAsset.instance.animator
                    createdAsset.releaseSourceData()
                    models.add(SceneModel(item.optString("name"), createdAsset, createdResources, animator))
                } catch (error: Exception) {
                    resourceLoader?.asyncCancelLoad()
                    resourceLoader?.destroy()
                    if (asset != null) assetLoader?.destroyAsset(asset)
                    loadingLabel?.text = "ځینې ماډلونه ونه لوډېدل؛ پاتې ماډلونه چمتو کېږي."
                }
            }
            if (models.isEmpty()) {
                loadingLabel?.text = "د پروژې هېڅ GLB ماډل رینډر نه شو."
            }
        } catch (error: Exception) {
            releaseModels()
            loadingLabel?.text = error.message ?: "د صحنې لوډول ناکام شول"
        }
    }

    private fun applySceneTransform(
        modelViewer: ModelViewer,
        asset: FilamentAsset,
        item: org.json.JSONObject
    ) {
        val box = asset.boundingBox
        val center = box.center
        val half = box.halfExtent
        val maxExtent = 2f * maxOf(half[0], half[1], half[2])
        val fitScale = if (maxExtent > 0.0001f) 2f / maxExtent else 1f
        val objectScale = item.optDouble("scale", 1.0).toFloat().coerceIn(0.25f, 3f)
        val matrix = FloatArray(16)
        android.opengl.Matrix.setRotateEulerM(
            matrix, 0,
            item.optDouble("rotationX").toFloat(),
            item.optDouble("rotationY").toFloat(),
            item.optDouble("rotationZ").toFloat()
        )
        val finalScale = fitScale * objectScale
        for (column in 0..2) for (row in 0..2) matrix[column * 4 + row] *= finalScale
        matrix[12] = -center[0] * fitScale + item.optDouble("x").toFloat()
        matrix[13] = -center[1] * fitScale + item.optDouble("y").toFloat()
        matrix[14] = -center[2] * fitScale - 4f + item.optDouble("z").toFloat()
        matrix[15] = 1f
        val transforms = modelViewer.engine.transformManager
        transforms.setTransform(transforms.getInstance(asset.root), matrix)
    }

    private fun releaseModels() {
        val modelViewer = viewer
        val loader = assetLoader
        models.forEach { model ->
            try { model.resources.asyncCancelLoad() } catch (_: Exception) { }
            try { model.resources.evictResourceData() } catch (_: Exception) { }
            try { modelViewer?.scene?.removeEntities(model.asset.entities) } catch (_: Exception) { }
            try { loader?.destroyAsset(model.asset) } catch (_: Exception) { }
            try { model.resources.destroy() } catch (_: Exception) { }
        }
        models.clear()
        try { assetLoader?.destroy() } catch (_: Exception) { }
        assetLoader = null
        try { materialProvider?.destroy() } catch (_: Exception) { }
        materialProvider = null
    }

    override fun onPause() {
        if (framePosted) {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            framePosted = false
        }
        releaseModels()
        super.onPause()
    }

    override fun onDestroy() {
        releaseModels()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PROJECT_ID = "multi_scene_project_id"
        private const val MAX_MODELS = 8
        private const val MAX_TOTAL_BYTES = 80L * 1024L * 1024L
    }
}
