package com.moviestudio.filmmaker

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
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
import org.json.JSONObject

/**
 * Displays every imported GLB model in the same Filament scene.
 * Models retain their original materials, textures, meshes and embedded animation.
 */
class MultiModelSceneActivity : Activity() {
    private data class SceneModel(
        val objectId: String,
        val name: String,
        val asset: FilamentAsset,
        val resources: ResourceLoader,
        val animator: com.google.android.filament.gltfio.Animator?,
        var lightsAdded: Boolean = false,
        var animationStartNanos: Long = 0L,
        var lastTimelineClipId: String? = null
    )

    private data class PlaybackClip(
        val id: String,
        val objectId: String,
        val action: String,
        val startSeconds: Double,
        val endSeconds: Double,
        val speed: Float,
        val animationIndex: Int,
        val animationName: String
    )

    private data class PlaybackCameraShot(
        val id: String,
        val startSeconds: Double,
        val endSeconds: Double,
        val data: JSONObject
    )

    private var viewer: ModelViewer? = null
    private var assetLoader: AssetLoader? = null
    private var materialProvider: UbershaderProvider? = null
    private val models = mutableListOf<SceneModel>()
    private var loadingLabel: TextView? = null
    private var projectId: String = ""
    private var framePosted = false
    private var omittedModelCount = 0
    private var timelineClips: List<PlaybackClip> = emptyList()
    private var cameraShots: List<PlaybackCameraShot> = emptyList()
    private var activeCameraShotId: String? = null
    private var timelineDurationSeconds = 0.0
    private var timelinePositionSeconds = 0.0
    private var timelineStartNanos = 0L
    private var timelinePlaying = true
    private var resumeTimelineAfterPause = false
    private var timelineStatus: TextView? = null

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
                        if (timelineClips.isNotEmpty()) {
                            val activeClip = timelineClips.firstOrNull {
                                it.objectId == model.objectId &&
                                    timelinePositionSeconds >= it.startSeconds &&
                                    timelinePositionSeconds < it.endSeconds
                            }
                            if (activeClip != null) {
                                val animationIndex = resolveAnimationIndex(animator, activeClip)
                                val duration = animator.getAnimationDuration(animationIndex)
                                val clipTime = ((timelinePositionSeconds - activeClip.startSeconds) * activeClip.speed).toFloat()
                                val animationTime = if (duration > 0f) clipTime % duration else clipTime
                                animator.applyAnimation(animationIndex, animationTime)
                                animator.updateBoneMatrices()
                                model.lastTimelineClipId = activeClip.id
                            } else if (model.lastTimelineClipId != null) {
                                animator.applyAnimation(0, 0f)
                                animator.updateBoneMatrices()
                                model.lastTimelineClipId = null
                            }
                        } else {
                            // Keep the previous preview behavior for projects without timeline clips.
                            if (model.animationStartNanos == 0L) model.animationStartNanos = frameTimeNanos
                            val elapsed = (frameTimeNanos - model.animationStartNanos) / 1_000_000_000f
                            val duration = animator.getAnimationDuration(0)
                            val time = if (duration > 0f) elapsed % duration else elapsed
                            animator.applyAnimation(0, time)
                            animator.updateBoneMatrices()
                        }
                    }
                }
            }
            if ((timelineClips.isNotEmpty() || cameraShots.isNotEmpty()) && timelinePlaying) {
                if (timelineStartNanos == 0L) timelineStartNanos = frameTimeNanos -
                    (timelinePositionSeconds * 1_000_000_000.0).toLong()
                timelinePositionSeconds = ((frameTimeNanos - timelineStartNanos).coerceAtLeast(0L) / 1_000_000_000.0)
                    .coerceAtMost(timelineDurationSeconds)
                if (timelinePositionSeconds >= timelineDurationSeconds) timelinePlaying = false
                applyCameraForTimelinePosition()
                updateTimelineStatus()
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
            val cameraShots = Button(this).apply {
                text = "د ثبت شوو کمرې شاټونه"
                isAllCaps = false
                setOnClickListener { showCameraShotPicker() }
            }
            frame.addView(cameraShots, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply { bottomMargin = 24; marginEnd = 16 })
            val playbackControls = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(10, 8, 10, 8)
                setBackgroundColor(0xAA17243A.toInt())
            }
            timelineStatus = TextView(this).apply {
                text = "Timeline نه دی لوډ شوی"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 12f
            }
            playbackControls.addView(timelineStatus)
            val playbackButtons = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
            }
            playbackButtons.addView(Button(this).apply {
                text = "پلې / درول"
                isAllCaps = false
                setOnClickListener { toggleTimelinePlayback() }
            })
            playbackButtons.addView(Button(this).apply {
                text = "له پیل څخه"
                isAllCaps = false
                setOnClickListener { restartTimeline() }
            })
            playbackControls.addView(playbackButtons)
            frame.addView(playbackControls, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.START
            ).apply { bottomMargin = 24; marginStart = 16 })
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
        if (resumeTimelineAfterPause && (timelineClips.isNotEmpty() || cameraShots.isNotEmpty()) &&
            timelinePositionSeconds < timelineDurationSeconds) {
            timelinePlaying = true
            timelineStartNanos = System.nanoTime() -
                (timelinePositionSeconds * 1_000_000_000.0).toLong()
        }
        resumeTimelineAfterPause = false
        if (viewer != null && !framePosted) {
            Choreographer.getInstance().postFrameCallback(frameCallback)
            framePosted = true
        }
    }

    private fun showCameraShotPicker() {
        try {
            val scene = ProjectStore(this).activeScene(projectId)
            val shots = scene?.optJSONArray("cameraShots")
            if (shots == null || shots.length() == 0) {
                Toast.makeText(this, "په دې پروژه کې د کمرې ثبت شوی شاټ نشته؛ لومړی یې په Scene Editor کې جوړ کړه", Toast.LENGTH_LONG).show()
                return
            }
            val available = (0 until shots.length()).mapNotNull { shots.optJSONObject(it) }
            if (available.isEmpty()) return
            val labels = available.mapIndexed { index, shot ->
                "${index + 1}. ${shot.optString("name", "کمره")} • FOV ${shot.optDouble("fieldOfView", 50.0)}°"
            }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("د صحنې د کمرې شاټ وټاکه")
                .setItems(labels) { _, which ->
                    available.getOrNull(which)?.let { applyCameraShot(it) }
                }
                .setNegativeButton("بند", null)
                .show()
        } catch (error: Exception) {
            Toast.makeText(this, error.message ?: "د کمرې شاټونه نه لوستل کېږي", Toast.LENGTH_LONG).show()
        }
    }

    private fun resolveAnimationIndex(
        animator: com.google.android.filament.gltfio.Animator,
        clip: PlaybackClip
    ): Int {
        if (clip.animationIndex in 0 until animator.animationCount) return clip.animationIndex
        if (clip.animationName.isNotBlank()) {
            for (index in 0 until animator.animationCount) {
                if (animator.getAnimationName(index).equals(clip.animationName, ignoreCase = true)) return index
            }
        }
        val keywords = when (clip.action.lowercase()) {
            "idle" -> listOf("idle", "stand", "rest", "wait", "breath")
            "walk" -> listOf("walk", "walking")
            "run" -> listOf("run", "running", "jog")
            "talk" -> listOf("talk", "speak", "speech", "gesture")
            else -> emptyList()
        }
        if (keywords.isEmpty()) return 0
        for (index in 0 until animator.animationCount) {
            val name = animator.getAnimationName(index).lowercase()
            if (keywords.any { keyword -> name.contains(keyword) }) return index
        }
        // A single-animation GLB has no ambiguity, regardless of its animation label.
        return 0
    }

    private fun toggleTimelinePlayback() {
        if (timelineClips.isEmpty() && cameraShots.isEmpty()) {
            Toast.makeText(this, "د دې پروژې Timeline او د کمرې شاټونه تش دي", Toast.LENGTH_LONG).show()
            return
        }
        if (timelinePlaying) {
            timelinePlaying = false
            timelineStartNanos = 0L
        } else if (timelinePositionSeconds >= timelineDurationSeconds) {
            restartTimeline()
            return
        } else {
            timelinePlaying = true
            timelineStartNanos = System.nanoTime() -
                (timelinePositionSeconds * 1_000_000_000.0).toLong()
        }
        updateTimelineStatus()
    }

    private fun restartTimeline() {
        if (timelineClips.isEmpty() && cameraShots.isEmpty()) {
            Toast.makeText(this, "د دې پروژې Timeline تش دی", Toast.LENGTH_SHORT).show()
            return
        }
        timelinePositionSeconds = 0.0
        timelineStartNanos = 0L
        timelinePlaying = true
        models.forEach { it.lastTimelineClipId = null }
        updateTimelineStatus()
    }

    private fun updateTimelineStatus() {
        val total = timelineDurationSeconds.coerceAtLeast(0.0)
        val current = timelinePositionSeconds.coerceAtLeast(0.0)
        val state = when {
            timelineClips.isEmpty() && cameraShots.isEmpty() -> "Timeline تش دی"
            timelinePlaying -> "چلېږي"
            current >= total -> "پای ته ورسېد"
            else -> "درېدلی"
        }
        timelineStatus?.text = String.format(
            java.util.Locale.US,
            "Timeline: %s  •  %.1f / %.1f ثانیې",
            state, current, total
        )
    }

    private fun clockSeconds(value: String): Int {
        if (!value.matches(Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$"))) {
            throw IllegalArgumentException("د فلم وخت ناسمه بڼه لري")
        }
        val parts = value.split(":").map { it.toInt() }
        return parts[0] * 3600 + parts[1] * 60 + parts[2]
    }

    private fun applyCameraForTimelinePosition() {
        val shot = cameraShots.firstOrNull {
            timelinePositionSeconds >= it.startSeconds && timelinePositionSeconds < it.endSeconds
        }
        val nextId = shot?.id
        if (nextId == activeCameraShotId) return
        activeCameraShotId = nextId
        if (shot != null) applyCameraShot(shot.data, showMessage = false)
    }

    private fun applyCameraShot(shot: JSONObject, showMessage: Boolean = true) {
        val modelViewer = viewer ?: return
        val eyeX = shot.optDouble("x", 0.0)
        val eyeY = shot.optDouble("y", 3.0)
        val eyeZ = shot.optDouble("z", 5.0) - 4.0
        val targetX = shot.optDouble("targetX", 0.0)
        val targetY = shot.optDouble("targetY", 1.0)
        val targetZ = shot.optDouble("targetZ", 0.0) - 4.0
        val fov = shot.optDouble("fieldOfView", 50.0).coerceIn(20.0, 100.0)
        val metrics = resources.displayMetrics
        val aspect = (metrics.widthPixels.toDouble() / metrics.heightPixels.toDouble()).coerceAtLeast(0.1)
        modelViewer.camera.lookAt(
            eyeX, eyeY, eyeZ,
            targetX, targetY, targetZ,
            0.0, 1.0, 0.0
        )
        modelViewer.camera.setProjection(fov, aspect, 0.1, 1000.0, com.google.android.filament.Camera.Fov.VERTICAL)
        if (showMessage) Toast.makeText(this, "د «${shot.optString("name", "کمره")}» شاټ فعال شو", Toast.LENGTH_SHORT).show()
    }

    private fun loadProjectModels() {
        val modelViewer = viewer ?: return
        try {
            val store = ProjectStore(this)
            val project = store.load(projectId)
            val scene = store.activeScene(projectId)
            val projectFps = (project.optJSONObject("project")?.optJSONObject("settings")?.optInt("fps", 24) ?: 24)
                .coerceIn(1, 120)
            val production = scene?.optJSONObject("production") ?: JSONObject()
            val filmStartSeconds = production.optString("startTime", "00:00:00")
                .let { runCatching { clockSeconds(it) }.getOrDefault(0) }
            val rawShots = scene?.optJSONArray("cameraShots") ?: org.json.JSONArray()
            cameraShots = (0 until rawShots.length()).mapNotNull { index ->
                val shot = rawShots.optJSONObject(index) ?: return@mapNotNull null
                val start = runCatching { clockSeconds(shot.optString("startTime")) - filmStartSeconds }.getOrNull()
                    ?: return@mapNotNull null
                val end = runCatching { clockSeconds(shot.optString("endTime")) - filmStartSeconds }.getOrNull()
                    ?: return@mapNotNull null
                if (end <= start) return@mapNotNull null
                PlaybackCameraShot(shot.optString("id"), start.toDouble(), end.toDouble(), shot)
            }.sortedBy { it.startSeconds }
            val rawClips = scene?.optJSONArray("timeline") ?: org.json.JSONArray()
            timelineClips = (0 until rawClips.length()).mapNotNull { index ->
                val clip = rawClips.optJSONObject(index) ?: return@mapNotNull null
                val start = if (clip.has("startOffsetSeconds")) clip.optDouble("startOffsetSeconds")
                    else clip.optInt("startFrame").toDouble() / projectFps.toDouble()
                val duration = if (clip.has("durationSeconds")) clip.optDouble("durationSeconds")
                    else clip.optInt("durationFrames").toDouble() / projectFps.toDouble()
                if (clip.optString("objectId").isBlank() || duration <= 0.0) return@mapNotNull null
                PlaybackClip(
                    clip.optString("id"),
                    clip.optString("objectId"),
                    clip.optString("action", "custom"),
                    start.coerceAtLeast(0.0),
                    (start + duration).coerceAtLeast(0.0),
                    clip.optDouble("speed", 1.0).toFloat().coerceIn(0.1f, 5f),
                    clip.optInt("animationIndex", -1),
                    clip.optString("animationName")
                )
            }.sortedBy { it.startSeconds }
            val clipEnd = timelineClips.maxOfOrNull { it.endSeconds } ?: 0.0
            val cameraEnd = cameraShots.maxOfOrNull { it.endSeconds } ?: 0.0
            val productionEnd = production.optString("endTime").takeIf { it.isNotBlank() }
                ?.let { runCatching { (clockSeconds(it) - filmStartSeconds).toDouble() }.getOrNull() } ?: 0.0
            timelineDurationSeconds = maxOf(clipEnd, cameraEnd, productionEnd)
            val hasPlayback = timelineClips.isNotEmpty() || cameraShots.isNotEmpty()
            if (!hasPlayback) {
                timelinePositionSeconds = 0.0
                timelineStartNanos = 0L
                timelinePlaying = false
            } else if (timelinePositionSeconds >= timelineDurationSeconds) {
                timelinePositionSeconds = 0.0
                timelinePlaying = true
                timelineStartNanos = 0L
            } else if (!timelinePlaying && timelinePositionSeconds == 0.0) {
                timelinePlaying = true
            }
            activeCameraShotId = null
            applyCameraForTimelinePosition()
            updateTimelineStatus()
            val objects = store.listSceneObjects(projectId)
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
                    models.add(SceneModel(item.optString("id"), item.optString("name"), createdAsset, createdResources, animator))
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
        // Keep renderer scale limits consistent with ProjectStore's editable 0.05..100 range.
        val objectScale = item.optDouble("scale", 1.0).toFloat().coerceIn(0.05f, 100f)
        val matrix = FloatArray(16)
        android.opengl.Matrix.setRotateEulerM(
            matrix, 0,
            item.optDouble("rotationX").toFloat(),
            item.optDouble("rotationY").toFloat(),
            item.optDouble("rotationZ").toFloat()
        )
        val finalScale = fitScale * objectScale
        for (column in 0..2) for (row in 0..2) matrix[column * 4 + row] *= finalScale

        // Translate the rotated/scaled model center to its scene position. Using only
        // fitScale here leaves off-center GLB origins orbiting when the object rotates.
        val centerX = matrix[0] * center[0] + matrix[4] * center[1] + matrix[8] * center[2]
        val centerY = matrix[1] * center[0] + matrix[5] * center[1] + matrix[9] * center[2]
        val centerZ = matrix[2] * center[0] + matrix[6] * center[1] + matrix[10] * center[2]
        matrix[12] = -centerX + item.optDouble("x").toFloat()
        matrix[13] = -centerY + item.optDouble("y").toFloat()
        matrix[14] = -centerZ - 4f + item.optDouble("z").toFloat()
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
        if (timelinePlaying && timelineStartNanos > 0L) {
            timelinePositionSeconds = ((System.nanoTime() - timelineStartNanos).coerceAtLeast(0L) /
                1_000_000_000.0).coerceAtMost(timelineDurationSeconds)
            resumeTimelineAfterPause = timelinePositionSeconds < timelineDurationSeconds
            timelinePlaying = false
            timelineStartNanos = 0L
            updateTimelineStatus()
        }
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
