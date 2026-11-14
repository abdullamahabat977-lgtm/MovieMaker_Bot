package com.moviestudio.filmmaker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.TimeZone
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Stores all movie projects and their assets locally on the phone only. */
class ProjectStore(context: Context) {
    // App-specific external storage needs no broad READ/WRITE storage permission.
    // Use private internal storage only if Android cannot provide the external app directory.
    private val root: File = createProjectRoot(context.applicationContext)

    val storageDescription: String
        get() = root.absolutePath

    val storageReady: Boolean
        get() {
            if (!root.isDirectory || !root.canRead() || !root.canWrite()) return false
            val probe = File(root, ".storage_check_" + UUID.randomUUID().toString())
            return try {
                probe.writeText("Film Studio storage check")
                val readable = probe.readText() == "Film Studio storage check"
                val deleted = probe.delete() && !probe.exists()
                readable && deleted
            } catch (_: Exception) {
                probe.delete()
                false
            }
        }

    fun projectDirectory(projectId: String): File = projectFolder(projectId)

    private fun createProjectRoot(context: Context): File {
        val external = context.getExternalFilesDir(null)?.let { File(it, "Film Studio/MovieProjects") }
        val internal = File(context.filesDir, "Film Studio/MovieProjects")
        val destination = listOfNotNull(external, internal).firstOrNull { candidate ->
            (candidate.isDirectory || candidate.mkdirs()) &&
                candidate.isDirectory && candidate.canRead() && candidate.canWrite()
        } ?: throw IllegalStateException("د Film Studio فولډر نه جوړېږي؛ د موبایل ذخیره وڅېړه")

        // Copy projects from the previous private filesDir location without overwriting newer data.
        val legacy = File(context.filesDir, "MovieProjects")
        if (legacy.isDirectory && legacy.canonicalFile != destination.canonicalFile) {
            legacy.listFiles()?.filter { it.isDirectory }?.forEach { oldProject ->
                val newProject = File(destination, oldProject.name)
                if (!newProject.exists()) oldProject.copyRecursively(newProject, overwrite = false)
            }
        }
        return destination
    }

    fun list(): List<JSONObject> = root.listFiles()
        ?.filter { it.isDirectory }
        ?.mapNotNull { folder ->
            try {
                val file = File(folder, "project.json")
                if (file.isFile) JSONObject(file.readText()) else null
            } catch (_: Exception) { null }
        }
        ?.sortedByDescending { it.optJSONObject("project")?.optString("updatedAt").orEmpty() }
        ?: emptyList()

    fun create(name: String): JSONObject {
        val id = "project_" + System.currentTimeMillis().toString()
        val safeName = name.trim().ifBlank { "نوی فلم" }.take(60)
        val folder = File(root, id).apply { mkdirs() }
        listOf("assets/characters", "assets/environments", "assets/props",
            "assets/images", "assets/audio", "exports").forEach { File(folder, it).mkdirs() }
        val now = timestamp()
        val scene = JSONObject()
            .put("id", "scene_1")
            .put("name", "لومړۍ صحنه")
            .put("objects", JSONArray())
            .put("timeline", JSONArray())
        val project = JSONObject()
            .put("schemaVersion", 1)
            .put("project", JSONObject()
                .put("id", id).put("name", safeName)
                .put("createdAt", now).put("updatedAt", now)
                .put("settings", JSONObject()
                    .put("width", 1280).put("height", 720)
                    .put("fps", 24).put("renderMode", "3d")))
            .put("assets", JSONArray())
            .put("scenes", JSONArray().put(scene))
            .put("activeSceneId", "scene_1")
            .put("timeline", JSONObject().put("sceneOrder", JSONArray().put("scene_1")))
        writeSafely(File(folder, "project.json"), project.toString(2))
        return project
    }

    fun load(projectId: String): JSONObject {
        val file = File(projectFolder(projectId), "project.json")
        require(file.isFile) { "پروژه ونه موندل شوه" }
        return JSONObject(file.readText())
    }

    fun listSceneObjects(projectId: String): List<JSONObject> {
        val objects = firstScene(load(projectId)).optJSONArray("objects") ?: JSONArray()
        return (0 until objects.length()).mapNotNull { index ->
            objects.optJSONObject(index)?.let { JSONObject(it.toString()) }
        }
    }

    fun activeScene(projectId: String): JSONObject = firstScene(load(projectId))

    fun listScenes(projectId: String): List<JSONObject> {
        val scenes = load(projectId).optJSONArray("scenes") ?: JSONArray()
        return (0 until scenes.length()).mapNotNull { index ->
            scenes.optJSONObject(index)?.let { JSONObject(it.toString()) }
        }
    }

    fun setActiveScene(projectId: String, sceneId: String): JSONObject {
        val project = load(projectId)
        val scenes = project.optJSONArray("scenes") ?: JSONArray()
        val selected = (0 until scenes.length()).mapNotNull { scenes.optJSONObject(it) }
            .firstOrNull { it.optString("id") == sceneId }
            ?: throw IllegalArgumentException("غوښتل شوې صحنه ونه موندل شوه")
        project.put("activeSceneId", sceneId)
        save(project)
        return selected
    }

    fun addScene(projectId: String, name: String): JSONObject {
        val project = load(projectId)
        val scenes = project.optJSONArray("scenes") ?: JSONArray().also { project.put("scenes", it) }
        require(scenes.length() < MAX_SCENES) { "په یوه فلم کې تر ۳۰ زیاتې صحنې نه شي جوړېدای" }
        val cleanName = name.trim().ifBlank { "صحنه ${scenes.length() + 1}" }.take(60)
        val current = firstScene(project)
        val scene = JSONObject()
            .put("id", "scene_" + UUID.randomUUID().toString())
            .put("name", cleanName)
            .put("objects", JSONArray())
            .put("timeline", JSONArray())
            .put("cameraShots", JSONArray())
        current.optJSONObject("ground")?.let { scene.put("ground", JSONObject(it.toString())) }
        current.optJSONObject("production")?.let { scene.put("production", JSONObject(it.toString())) }
        scenes.put(scene)
        project.put("activeSceneId", scene.optString("id"))
        val order = JSONArray()
        for (index in 0 until scenes.length()) order.put(scenes.optJSONObject(index)?.optString("id"))
        project.optJSONObject("timeline")?.put("sceneOrder", order)
        save(project)
        return scene
    }

    fun renameScene(projectId: String, sceneId: String, name: String): JSONObject {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "د صحنې نوم تش نه شي پاتې کېدای" }
        require(cleanName.length <= 60) { "د صحنې نوم تر ۶۰ تورو زیات نه شي کېدای" }
        val project = load(projectId)
        val scenes = project.optJSONArray("scenes") ?: JSONArray()
        val scene = (0 until scenes.length()).mapNotNull { scenes.optJSONObject(it) }
            .firstOrNull { it.optString("id") == sceneId }
            ?: throw IllegalArgumentException("صحنه ونه موندل شوه")
        scene.put("name", cleanName)
        save(project)
        return scene
    }

    fun removeScene(projectId: String, sceneId: String) {
        val project = load(projectId)
        val old = project.optJSONArray("scenes") ?: JSONArray()
        require(old.length() > 1) { "وروستۍ صحنه نه شي حذف کېدای؛ لږ تر لږه یوه صحنه باید پاتې وي" }
        val kept = JSONArray()
        var removedScene: JSONObject? = null
        for (index in 0 until old.length()) {
            val scene = old.optJSONObject(index) ?: continue
            if (scene.optString("id") == sceneId) removedScene = scene else kept.put(scene)
        }
        require(removedScene != null) { "صحنه ونه موندل شوه" }
        project.put("scenes", kept)
        val removedObjectIds = removedScene?.optJSONArray("objects")?.let { objects ->
            (0 until objects.length()).mapNotNull { objects.optJSONObject(it)?.optString("id") }.toSet()
        } ?: emptySet()
        val assets = project.optJSONArray("assets") ?: JSONArray()
        val retainedAssets = JSONArray()
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            if (asset.optString("id") !in removedObjectIds) retainedAssets.put(asset)
        }
        project.put("assets", retainedAssets)
        if (project.optString("activeSceneId") == sceneId) {
            project.put("activeSceneId", kept.optJSONObject(0)?.optString("id"))
        }
        val order = JSONArray()
        for (index in 0 until kept.length()) order.put(kept.optJSONObject(index)?.optString("id"))
        project.optJSONObject("timeline")?.put("sceneOrder", order)
        save(project)
    }

    /** Reads embedded GLB animation names without loading the model into the renderer. */
    fun listModelAnimations(projectId: String, objectId: String): List<String> {
        val project = load(projectId)
        val objects = firstScene(project).optJSONArray("objects") ?: JSONArray()
        val item = (0 until objects.length()).mapNotNull { objects.optJSONObject(it) }
            .firstOrNull { it.optString("id") == objectId }
            ?: throw IllegalArgumentException("ټاکل شوی 3D توکی ونه موندل شو")
        val relativePath = item.optString("modelPath")
        require(relativePath.isNotBlank()) { "دا توکی وارد شوی GLB فایل نه لري" }
        val folder = projectFolder(projectId).canonicalFile
        val file = File(folder, relativePath).canonicalFile
        require(file.path.startsWith(folder.path + File.separator) && file.isFile) {
            "د توکي GLB فایل ونه موندل شو"
        }
        require(file.length() in 20L..MAX_MODEL_BYTES) { "د GLB فایل اندازه ناسمه ده" }
        RandomAccessFile(file, "r").use { input ->
            val header = ByteArray(20)
            input.readFully(header)
            val info = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            require(String(header, 0, 4, Charsets.US_ASCII) == "glTF" && info.getInt(4) == 2) {
                "یوازې GLB 2.0 ماډلونه ملاتړ کېږي"
            }
            val declaredLength = info.getInt(8).toLong() and 0xffffffffL
            val jsonLength = info.getInt(12).toLong() and 0xffffffffL
            require(declaredLength == file.length() && info.getInt(16) == 0x4E4F534A &&
                jsonLength in 2L..(8L * 1024L * 1024L) && jsonLength % 4L == 0L &&
                20L + jsonLength <= file.length()) {
                "د GLB د JSON برخه ناسمه ده"
            }
            val jsonBytes = ByteArray(jsonLength.toInt())
            input.readFully(jsonBytes)
            val document = JSONObject(String(jsonBytes, Charsets.UTF_8).trim { it.code <= 32 })
            val animations = document.optJSONArray("animations") ?: JSONArray()
            return (0 until animations.length()).map { index ->
                val name = animations.optJSONObject(index)?.optString("name").orEmpty().trim()
                name.ifBlank { "حرکت ${index + 1}" }.take(80)
            }
        }
    }

    fun save(project: JSONObject): JSONObject {
        val details = project.optJSONObject("project")
            ?: throw IllegalArgumentException("د پروژې معلومات نشته")
        val id = details.optString("id")
        val folder = projectFolder(id)
        details.put("updatedAt", timestamp())
        writeSafely(File(folder, "project.json"), project.toString(2))
        return project
    }

    /**
     * Imports a real GLB 2.0 model into the project's private local asset folder.
     * Supported types: character, animal, vehicle, building, tree, furniture, prop, environment.
     */
    fun importSceneModel(
        projectId: String,
        source: InputStream,
        displayName: String,
        modelType: String
    ): JSONObject {
        val folders = mapOf(
            "character" to "characters",
            "animal" to "characters",
            "vehicle" to "props",
            "building" to "environments",
            "tree" to "environments",
            "furniture" to "props",
            "prop" to "props",
            "environment" to "environments",
            "clothing" to "props",
            "weapon" to "props",
            "camera" to "props",
            "tool" to "props",
            "medical" to "props",
            "food" to "props",
            "office" to "props",
            "sports" to "props",
            "travel" to "props",
            "road" to "props",
            "electronics" to "props",
            "other" to "props"
        )
        val folderName = folders[modelType]
            ?: throw IllegalArgumentException("د 3D ماډل ډول ناسم دی")
        val projectDirectory = projectFolder(projectId)
        val assetDirectory = File(projectDirectory, "assets/$folderName").apply { mkdirs() }
        val modelFile = File(assetDirectory, modelType + "_" + UUID.randomUUID() + ".glb")

        try {
            source.use { input ->
                modelFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var totalBytes = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        require(totalBytes <= MAX_MODEL_BYTES) {
                            "د GLB ماډل اندازه له ۴۰ MB څخه زیاته ده؛ کوچنی ماډل وټاکه"
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
            validateGlb2(modelFile)
        } catch (error: Exception) {
            modelFile.delete()
            throw error
        }

        val project = try { load(projectId) } catch (error: Exception) {
            modelFile.delete()
            throw error
        }
        val existingBytes = project.optJSONArray("assets")?.let { assets ->
            (0 until assets.length()).mapNotNull { index ->
                assets.optJSONObject(index)?.let { it.optString("path") to it.optLong("sizeBytes") }
            }.distinctBy { it.first }.sumOf { it.second }
        } ?: 0L
        if (existingBytes + modelFile.length() > MAX_PROJECT_MODEL_BYTES) {
            modelFile.delete()
            throw IllegalArgumentException("د دې پروژې د 3D فایلونو ټول حجم له ۲۵۶ MB څخه زیات نه شي کېدای")
        }
        val existingObjects = firstScene(project).optJSONArray("objects") ?: JSONArray()
        if (existingObjects.length() >= MAX_SCENE_OBJECTS) {
            modelFile.delete()
            throw IllegalArgumentException("په یوه صحنه کې تر ۳۲ زیات توکي مه واردوه")
        }

        val cleanName = displayName.substringBeforeLast('.', displayName)
            .replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(60)
            .ifBlank { modelType }
        val relativePath = "assets/$folderName/${modelFile.name}"
        val scene = firstScene(project)
        val objects = scene.optJSONArray("objects") ?: JSONArray().also { scene.put("objects", it) }
        val objectId = "object_" + UUID.randomUUID().toString()
        val slot = objects.length()
        val layout = arrayOf(
            0.0 to 0.0, -2.2 to 0.0, 2.2 to 0.0,
            0.0 to -2.2, -2.2 to -2.2, 2.2 to -2.2
        )
        val position = layout[slot % layout.size]
        val depth = position.second - (slot / layout.size) * 4.4
        val item = JSONObject()
            .put("id", objectId)
            .put("type", modelType)
            .put("name", cleanName)
            .put("format", "glb")
            .put("modelPath", relativePath)
            .put("x", position.first).put("y", 0.0).put("z", depth).put("scale", 1.0)
            .put("rotationX", 0.0).put("rotationY", 0.0).put("rotationZ", 0.0)
        try {
            objects.put(item)
            val assets = project.optJSONArray("assets") ?: JSONArray().also { project.put("assets", it) }
            assets.put(JSONObject()
                .put("id", objectId)
                .put("type", modelType)
                .put("format", "glb")
                .put("name", cleanName)
                .put("path", relativePath)
                .put("sizeBytes", modelFile.length()))
            save(project)
        } catch (error: Exception) {
            modelFile.delete()
            throw error
        }
        return item
    }

    /** Backward-compatible helper for older callers. */
    fun importCharacterModel(projectId: String, source: InputStream, displayName: String): JSONObject =
        importSceneModel(projectId, source, displayName, "character")

    fun duplicateSceneObject(projectId: String, objectId: String): JSONObject {
        val project = load(projectId)
        val objects = firstScene(project).optJSONArray("objects") ?: JSONArray()
        require(objects.length() < MAX_SCENE_OBJECTS) {
            "په یوه صحنه کې تر ۳۲ زیات توکي نه شي کېدای"
        }
        val source = (0 until objects.length()).mapNotNull { objects.optJSONObject(it) }
            .firstOrNull { it.optString("id") == objectId }
            ?: throw IllegalArgumentException("د صحنې توکی ونه موندل شو")
        val ground = firstScene(project).optJSONObject("ground") ?: JSONObject()
        val halfWidth = (ground.optDouble("widthMeters", 6.0) / 2.0).coerceAtLeast(0.5)
        val halfDepth = (ground.optDouble("depthMeters", 6.0) / 2.0).coerceAtLeast(0.5)
        val copy = JSONObject(source.toString())
            .put("id", "object_" + UUID.randomUUID().toString())
            .put("name", source.optString("name") + " - کاپي")
            .put("x", (source.optDouble("x") + 0.35).coerceIn(-halfWidth, halfWidth))
            .put("z", (source.optDouble("z") + 0.35).coerceIn(-halfDepth, halfDepth))
        objects.put(copy)
        val assets = project.optJSONArray("assets") ?: JSONArray()
        val sourceAsset = (0 until assets.length()).mapNotNull { assets.optJSONObject(it) }
            .firstOrNull { it.optString("id") == objectId }
        if (sourceAsset != null) {
            assets.put(JSONObject(sourceAsset.toString()).put("id", copy.optString("id")))
        }
        save(project)
        return copy
    }

    fun updateSceneObject(projectId: String, objectId: String, changes: JSONObject): JSONObject {
        val project = load(projectId)
        val objects = firstScene(project).optJSONArray("objects") ?: JSONArray()
        var found: JSONObject? = null
        for (index in 0 until objects.length()) {
            val item = objects.optJSONObject(index) ?: continue
            if (item.optString("id") == objectId) { found = item; break }
        }
        val item = found ?: throw IllegalArgumentException("د صحنې توکی ونه موندل شو")
        listOf("name", "x", "y", "z", "scale", "rotationX", "rotationY", "rotationZ").forEach { key ->
            if (changes.has(key)) item.put(key, changes.get(key))
        }
        val ground = firstScene(project).optJSONObject("ground") ?: JSONObject()
        val halfWidth = (ground.optDouble("widthMeters", 6.0) / 2.0).coerceAtLeast(0.5)
        val halfDepth = (ground.optDouble("depthMeters", 6.0) / 2.0).coerceAtLeast(0.5)
        item.put("scale", item.optDouble("scale", 1.0).coerceIn(0.05, 100.0))
        item.put("x", item.optDouble("x").coerceIn(-halfWidth, halfWidth))
        item.put("y", item.optDouble("y").coerceIn(-10.0, 100.0))
        item.put("z", item.optDouble("z").coerceIn(-halfDepth, halfDepth))
        listOf("rotationX", "rotationY", "rotationZ").forEach { key ->
            val normalized = ((item.optDouble(key) % 360.0) + 360.0) % 360.0
            item.put(key, normalized)
        }
        save(project)
        return item
    }

    fun removeSceneObject(projectId: String, objectId: String) {
        val project = load(projectId)
        val old = firstScene(project).optJSONArray("objects") ?: JSONArray()
        val kept = JSONArray()
        var removed = false
        var deletedPath: String? = null
        for (index in 0 until old.length()) {
            val item = old.optJSONObject(index) ?: continue
            if (item.optString("id") == objectId) {
                removed = true
                deletedPath = item.optString("modelPath").ifBlank { null }
            } else {
                kept.put(item)
            }
        }
        require(removed) { "د صحنې توکی ونه موندل شو" }
        firstScene(project).put("objects", kept)
        val assets = project.optJSONArray("assets") ?: JSONArray()
        val keptAssets = JSONArray()
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            if (asset.optString("id") != objectId) keptAssets.put(asset)
        }
        project.put("assets", keptAssets)
        val clips = firstScene(project).optJSONArray("timeline") ?: JSONArray()
        val keptClips = JSONArray()
        for (index in 0 until clips.length()) {
            val clip = clips.optJSONObject(index) ?: continue
            if (clip.optString("objectId") != objectId) keptClips.put(clip)
        }
        firstScene(project).put("timeline", keptClips)
        save(project)
        if (!deletedPath.isNullOrBlank()) {
            val stillUsed = (0 until kept.length()).any {
                kept.optJSONObject(it)?.optString("modelPath") == deletedPath
            }
            if (!stillUsed) {
                val base = projectFolder(projectId).canonicalFile
                val candidate = File(base, deletedPath!!).canonicalFile
                if (candidate.path.startsWith(base.path + File.separator) && candidate.isFile) candidate.delete()
            }
        }
    }

    fun addCameraShot(
        projectId: String,
        name: String,
        x: Double, y: Double, z: Double,
        targetX: Double, targetY: Double, targetZ: Double,
        fieldOfView: Double,
        startTime: String,
        endTime: String
    ): JSONObject {
        require(fieldOfView in 20.0..100.0) { "د کمرې FOV باید له ۲۰ تر ۱۰۰ درجو وي" }
        listOf(x, y, z, targetX, targetY, targetZ).forEach {
            require(it.isFinite() && it in -1000.0..1000.0) { "د کمرې موقعیت له ټاکلي حد څخه بهر دی" }
        }
        val start = clockSeconds(startTime)
        val end = clockSeconds(endTime)
        require(end > start) { "د کمرې د شاټ پای وخت باید د پیل له وخت وروسته وي" }
        val project = load(projectId)
        val scene = firstScene(project)
        val production = scene.optJSONObject("production")
        if (production != null) {
            val filmStart = clockSeconds(production.optString("startTime"))
            val filmEnd = clockSeconds(production.optString("endTime"))
            require(start >= filmStart && end <= filmEnd) { "د کمرې شاټ باید د فلم د ټاکلي وخت دننه وي" }
        }
        val shot = JSONObject()
            .put("id", "camera_" + UUID.randomUUID().toString())
            .put("name", name.trim().ifBlank { "کمره" }.take(60))
            .put("x", x).put("y", y).put("z", z)
            .put("targetX", targetX).put("targetY", targetY).put("targetZ", targetZ)
            .put("fieldOfView", fieldOfView)
            .put("startTime", startTime).put("endTime", endTime)
        val cameras = scene.optJSONArray("cameraShots") ?: JSONArray().also { scene.put("cameraShots", it) }
        require(cameras.length() < 32) { "په یوه صحنه کې تر ۳۲ زیات د کمرې شاټونه نه شي ثبتېدای" }
        cameras.put(shot)
        save(project)
        return shot
    }

    fun updateCameraShot(
        projectId: String,
        cameraId: String,
        name: String,
        x: Double, y: Double, z: Double,
        targetX: Double, targetY: Double, targetZ: Double,
        fieldOfView: Double,
        startTime: String,
        endTime: String
    ): JSONObject {
        require(fieldOfView in 20.0..100.0) { "د کمرې FOV باید له ۲۰ تر ۱۰۰ درجو وي" }
        listOf(x, y, z, targetX, targetY, targetZ).forEach {
            require(it.isFinite() && it in -1000.0..1000.0) { "د کمرې موقعیت له ټاکلي حد څخه بهر دی" }
        }
        val start = clockSeconds(startTime)
        val end = clockSeconds(endTime)
        require(end > start) { "د کمرې د شاټ پای وخت باید د پیل له وخت وروسته وي" }
        val project = load(projectId)
        val scene = firstScene(project)
        val cameras = scene.optJSONArray("cameraShots") ?: JSONArray()
        val shot = (0 until cameras.length()).mapNotNull { cameras.optJSONObject(it) }
            .firstOrNull { it.optString("id") == cameraId }
            ?: throw IllegalArgumentException("د کمرې شاټ ونه موندل شو")
        val production = scene.optJSONObject("production")
        if (production != null) {
            val filmStart = clockSeconds(production.optString("startTime"))
            val filmEnd = clockSeconds(production.optString("endTime"))
            require(start >= filmStart && end <= filmEnd) { "د کمرې شاټ باید د فلم د ټاکلي وخت دننه وي" }
        }
        shot.put("name", name.trim().ifBlank { "کمره" }.take(60))
            .put("x", x).put("y", y).put("z", z)
            .put("targetX", targetX).put("targetY", targetY).put("targetZ", targetZ)
            .put("fieldOfView", fieldOfView)
            .put("startTime", startTime).put("endTime", endTime)
        save(project)
        return shot
    }

    fun removeCameraShot(projectId: String, cameraId: String) {
        val project = load(projectId)
        val scene = firstScene(project)
        val old = scene.optJSONArray("cameraShots") ?: JSONArray()
        val kept = JSONArray()
        var removed = false
        for (index in 0 until old.length()) {
            val shot = old.optJSONObject(index) ?: continue
            if (shot.optString("id") == cameraId) removed = true else kept.put(shot)
        }
        require(removed) { "د کمرې شاټ ونه موندل شو" }
        scene.put("cameraShots", kept)
        save(project)
    }

    fun updateProductionSettings(
        projectId: String,
        groundWidthMeters: Double,
        groundDepthMeters: Double,
        filmStartTime: String,
        filmEndTime: String
    ): JSONObject {
        require(groundWidthMeters in 1.0..1000.0 && groundDepthMeters in 1.0..1000.0) {
            "د ځمکې هر اړخ باید له ۱ تر ۱۰۰۰ مترو وي"
        }
        val start = clockSeconds(filmStartTime)
        val end = clockSeconds(filmEndTime)
        require(end > start) { "د فلم د پای وخت باید د پیل له وخت وروسته وي" }
        require(end - start <= 24 * 60 * 60) { "د فلم موده تر ۲۴ ساعتونو زیاته نه شي کېدای" }
        val project = load(projectId)
        val scene = firstScene(project)

        // Do not silently invalidate camera shots or timed movement when the film clock changes.
        val existingClips = scene.optJSONArray("timeline") ?: JSONArray()
        for (index in 0 until existingClips.length()) {
            val clip = existingClips.optJSONObject(index) ?: continue
            if (!clip.has("startTime") || !clip.has("endTime")) continue
            val clipStart = clockSeconds(clip.optString("startTime"))
            val clipEnd = clockSeconds(clip.optString("endTime"))
            require(clipStart >= start && clipEnd <= end) {
                "د فلم نوی وخت د موجود Timeline حرکت له وخت سره ټکر لري؛ لومړی هغه کلیپ سم یا حذف کړه"
            }
        }
        val existingCameras = scene.optJSONArray("cameraShots") ?: JSONArray()
        for (index in 0 until existingCameras.length()) {
            val shot = existingCameras.optJSONObject(index) ?: continue
            val shotStart = clockSeconds(shot.optString("startTime"))
            val shotEnd = clockSeconds(shot.optString("endTime"))
            require(shotStart >= start && shotEnd <= end) {
                "د فلم نوی وخت د موجودې کمرې له شاټ سره ټکر لري؛ لومړی هغه شاټ سم یا حذف کړه"
            }
        }

        scene.put("ground", JSONObject()
            .put("widthMeters", groundWidthMeters)
            .put("depthMeters", groundDepthMeters)
            .put("areaSquareMeters", groundWidthMeters * groundDepthMeters))

        // When the ground is resized, keep every existing object center inside its new bounds.
        val halfWidth = groundWidthMeters / 2.0
        val halfDepth = groundDepthMeters / 2.0
        val objects = scene.optJSONArray("objects") ?: JSONArray()
        for (index in 0 until objects.length()) {
            val item = objects.optJSONObject(index) ?: continue
            item.put("x", item.optDouble("x", 0.0).coerceIn(-halfWidth, halfWidth))
            item.put("z", item.optDouble("z", 0.0).coerceIn(-halfDepth, halfDepth))
        }

        scene.put("production", JSONObject()
            .put("startTime", filmStartTime)
            .put("endTime", filmEndTime)
            .put("durationSeconds", end - start))
        save(project)
        return scene
    }

    fun addTimedTimelineClip(
        projectId: String,
        objectId: String,
        action: String,
        startTime: String,
        endTime: String,
        speed: Double,
        animationIndex: Int = -1,
        animationName: String = ""
    ): JSONObject {
        require(action in setOf("idle", "walk", "run", "talk", "custom")) { "د حرکت ډول ناسم دی" }
        require(speed in 0.1..5.0) { "د حرکت سرعت باید له ۰.۱ تر ۵ برابرو وي" }
        require(animationIndex in -1..255) { "د Animation ټاکنه ناسمه ده" }
        val project = load(projectId)
        val scene = firstScene(project)
        val objects = scene.optJSONArray("objects") ?: JSONArray()
        require((0 until objects.length()).any { objects.optJSONObject(it)?.optString("id") == objectId }) {
            "ټاکل شوی کرکټر یا توکی په صحنه کې نشته"
        }
        val start = clockSeconds(startTime)
        val end = clockSeconds(endTime)
        require(end > start) { "د حرکت د پای وخت باید د پیل له وخت وروسته وي" }
        val production = scene.optJSONObject("production")
            ?: throw IllegalStateException("لومړی د فلم د پیل او پای وختونه تنظیم کړه")
        val filmStart = clockSeconds(production.optString("startTime"))
        val filmEnd = clockSeconds(production.optString("endTime"))
        require(start >= filmStart && end <= filmEnd) { "د حرکت وخت باید د فلم د ټاکلي وخت دننه وي" }
        // Keep frame math safe even if an older or manually edited project has invalid FPS.
        val fps = (project.optJSONObject("project")?.optJSONObject("settings")?.optInt("fps", 24) ?: 24)
            .coerceIn(1, 120)
        val clips = scene.optJSONArray("timeline") ?: JSONArray().also { scene.put("timeline", it) }
        require(clips.length() < MAX_TIMELINE_CLIPS) {
            "په یوه صحنه کې تر ۵۰۰ زیات Timeline کلیپونه نه شي ثبتېدای"
        }
        val newStartOffset = (start - filmStart).toDouble()
        val newEndOffset = (end - filmStart).toDouble()
        for (index in 0 until clips.length()) {
            val existing = clips.optJSONObject(index) ?: continue
            if (existing.optString("objectId") != objectId) continue
            val existingStart: Double
            val existingEnd: Double
            if (existing.has("startTime") && existing.has("endTime")) {
                existingStart = (clockSeconds(existing.optString("startTime")) - filmStart).toDouble()
                existingEnd = (clockSeconds(existing.optString("endTime")) - filmStart).toDouble()
            } else {
                existingStart = existing.optInt("startFrame").toDouble() / fps.toDouble()
                existingEnd = (existing.optInt("startFrame") + existing.optInt("durationFrames")).toDouble() / fps.toDouble()
            }
            require(newEndOffset <= existingStart || newStartOffset >= existingEnd) {
                "د همدې کرکټر بل حرکت له دې وخت سره ټکر لري؛ وختونه جلا کړه"
            }
        }
        val clip = JSONObject()
            .put("id", "clip_" + UUID.randomUUID().toString())
            .put("objectId", objectId)
            .put("action", action)
            .put("startTime", startTime)
            .put("endTime", endTime)
            .put("startOffsetSeconds", start - filmStart)
            .put("durationSeconds", end - start)
            .put("startFrame", (start - filmStart) * fps)
            .put("durationFrames", (end - start) * fps)
            .put("speed", speed)
            .put("animationIndex", animationIndex)
            .put("animationName", animationName.take(80))
            .put("timingMode", "film_clock")
        val ordered = (0 until clips.length()).mapNotNull { clips.optJSONObject(it) } + clip
        val sorted = JSONArray()
        ordered.sortedBy { item ->
            if (item.has("startOffsetSeconds")) item.optDouble("startOffsetSeconds")
            else item.optInt("startFrame").toDouble() / fps.toDouble()
        }.forEach { sorted.put(it) }
        scene.put("timeline", sorted)
        save(project)
        return clip
    }

    private fun clockSeconds(value: String): Int {
        require(value.matches(Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$"))) {
            "وخت باید د HH:mm:ss په بڼه وي، لکه 09:01:00"
        }
        val parts = value.split(":").map { it.toInt() }
        return parts[0] * 3600 + parts[1] * 60 + parts[2]
    }

    fun addTimelineClip(
        projectId: String,
        objectId: String,
        action: String,
        durationFrames: Int = 48,
        animationIndex: Int = -1,
        animationName: String = ""
    ): JSONObject {
        require(action in setOf("idle", "walk", "run", "talk", "custom")) { "د حرکت ډول ناسم دی" }
        require(durationFrames in 1..2400) { "د Timeline موده باید له ۱ تر ۲۴۰۰ فریمونو وي" }
        require(animationIndex in -1..255) { "د Animation ټاکنه ناسمه ده" }
        val project = load(projectId)
        val scene = firstScene(project)
        val objects = scene.optJSONArray("objects") ?: JSONArray()
        require((0 until objects.length()).any { objects.optJSONObject(it)?.optString("id") == objectId }) {
            "ټاکل شوی کرکټر یا توکی په صحنه کې نشته"
        }
        val clips = scene.optJSONArray("timeline") ?: JSONArray().also { scene.put("timeline", it) }
        require(clips.length() < MAX_TIMELINE_CLIPS) {
            "په یوه صحنه کې تر ۵۰۰ زیات Timeline کلیپونه نه شي ثبتېدای"
        }
        val start = (0 until clips.length()).mapNotNull { clips.optJSONObject(it) }
            .maxOfOrNull { it.optInt("startFrame") + it.optInt("durationFrames") } ?: 0
        val clip = JSONObject()
            .put("id", "clip_" + UUID.randomUUID().toString())
            .put("objectId", objectId).put("action", action)
            .put("startFrame", start).put("durationFrames", durationFrames)
            .put("animationIndex", animationIndex)
            .put("animationName", animationName.take(80))
        clips.put(clip)
        save(project)
        return clip
    }

    fun removeTimelineClip(projectId: String, clipId: String) {
        val project = load(projectId)
        val scene = firstScene(project)
        val old = scene.optJSONArray("timeline") ?: JSONArray()
        val kept = JSONArray()
        var removed = false
        for (index in 0 until old.length()) {
            val clip = old.optJSONObject(index) ?: continue
            if (clip.optString("id") == clipId) removed = true else kept.put(clip)
        }
        require(removed) { "د Timeline کلیپ ونه موندل شو" }
        scene.put("timeline", kept)
        save(project)
    }

    fun rename(projectId: String, newName: String): JSONObject {
        val project = load(projectId)
        val safeName = newName.trim()
        require(safeName.isNotEmpty()) { "د پروژې نوم تش نه شي پاتې کېدای" }
        require(safeName.length <= 60) { "د پروژې نوم ډېر اوږد دی" }
        project.getJSONObject("project").put("name", safeName)
        return save(project)
    }

    fun delete(projectId: String) {
        val folder = projectFolder(projectId)
        require(File(folder, "project.json").isFile) { "پروژه ونه موندل شوه" }
        if (!folder.deleteRecursively()) throw IllegalStateException("د پروژې حذف ناکام شو")
    }

    private fun firstScene(project: JSONObject): JSONObject {
        val scenes = project.optJSONArray("scenes") ?: JSONArray().also { project.put("scenes", it) }
        if (scenes.length() == 0) {
            val scene = JSONObject().put("id", "scene_1").put("name", "لومړۍ صحنه")
                .put("objects", JSONArray()).put("timeline", JSONArray())
            scenes.put(scene)
        }
        val activeId = project.optString("activeSceneId")
        val scene = (0 until scenes.length()).mapNotNull { scenes.optJSONObject(it) }
            .firstOrNull { activeId.isNotBlank() && it.optString("id") == activeId }
            ?: scenes.optJSONObject(0) ?: JSONObject()
        if (!scene.has("objects")) scene.put("objects", JSONArray())
        if (!scene.has("timeline")) scene.put("timeline", JSONArray())
        if (!scene.has("cameraShots")) scene.put("cameraShots", JSONArray())
        return scene
    }

    private fun projectFolder(projectId: String): File {
        require(projectId.matches(Regex("project_[0-9]+"))) { "د پروژې پېژندنه ناسمه ده" }
        val folder = File(root, projectId).canonicalFile
        require(folder.parentFile == root.canonicalFile) { "د پروژې لاره ناسمه ده" }
        return folder
    }

    private fun validateGlb2(file: File) {
        val fileLength = file.length()
        require(fileLength >= 20L) { "GLB فایل ډېر کوچنی یا تش دی" }
        java.io.RandomAccessFile(file, "r").use { input ->
            val header = ByteArray(20)
            input.readFully(header)
            require(String(header, 0, 4, Charsets.US_ASCII) == "glTF") { "دا GLB فایل نه دی" }
            val headerBytes = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            require(headerBytes.getInt(4) == 2) { "یوازې GLB 2.0 ملاتړ کېږي" }
            val declaredLength = headerBytes.getInt(8).toLong() and 0xffffffffL
            require(declaredLength == fileLength) { "د GLB فایل اندازه له سرلیک سره برابره نه ده" }

            val jsonLength = headerBytes.getInt(12).toLong() and 0xffffffffL
            require(headerBytes.getInt(16) == 0x4E4F534A) {
                "د GLB لومړی chunk باید JSON وي"
            }
            require(jsonLength in 2L..(8L * 1024L * 1024L) && jsonLength % 4L == 0L) {
                "د GLB JSON برخه ناسمه یا ډېره لویه ده"
            }
            require(20L + jsonLength <= fileLength) { "د GLB JSON برخه نیمګړې ده" }

            val jsonBytes = ByteArray(jsonLength.toInt())
            input.readFully(jsonBytes)
            val jsonText = String(jsonBytes, Charsets.UTF_8).trim { it.code <= 32 }
            val document = JSONObject(jsonText)
            val assetVersion = document.optJSONObject("asset")?.optString("version")
            require(assetVersion == "2.0") { "په GLB کې د asset.version باید 2.0 وي" }

            // Walk every remaining chunk to reject truncated or out-of-range chunk data.
            var offset = 20L + jsonLength
            var binaryChunkLength = -1L
            while (offset < fileLength) {
                require(fileLength - offset >= 8L) { "د GLB د chunk سرلیک نیمګړی دی" }
                input.seek(offset)
                val chunkHeader = ByteArray(8)
                input.readFully(chunkHeader)
                val chunkInfo = ByteBuffer.wrap(chunkHeader).order(ByteOrder.LITTLE_ENDIAN)
                val chunkLength = chunkInfo.getInt(0).toLong() and 0xffffffffL
                val chunkType = chunkInfo.getInt(4)
                require(chunkLength % 4L == 0L) { "د GLB د chunk اندازه باید د ۴ بایټونو مضرب وي" }
                require(chunkLength <= fileLength - offset - 8L) {
                    "د GLB یوه برخه له فایل څخه بهر اشاره کوي"
                }
                if (chunkType == 0x004E4942) {
                    require(binaryChunkLength < 0L) { "په GLB کې له یوې زیاتې BIN برخې نشي کېدای" }
                    binaryChunkLength = chunkLength
                }
                offset += 8L + chunkLength
            }
            require(offset == fileLength) { "د GLB د برخو ترتیب یا اندازه ناسمه ده" }

            val buffers = document.optJSONArray("buffers") ?: JSONArray()
            for (index in 0 until buffers.length()) {
                val buffer = buffers.optJSONObject(index) ?: continue
                if (!buffer.has("uri")) {
                    require(binaryChunkLength >= 0L) { "د GLB داخلي buffer شته، خو BIN برخه نشته" }
                    val requiredBytes = buffer.optLong("byteLength", -1L)
                    require(requiredBytes >= 0L && requiredBytes <= binaryChunkLength) {
                        "د GLB داخلي buffer د BIN برخې له اندازې لوی دی"
                    }
                }
            }
        }
    }

    private companion object {
        const val MAX_MODEL_BYTES = 40L * 1024L * 1024L
        const val MAX_PROJECT_MODEL_BYTES = 256L * 1024L * 1024L
        const val MAX_SCENE_OBJECTS = 32
        const val MAX_TIMELINE_CLIPS = 500
        const val MAX_SCENES = 30
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

    private fun writeSafely(target: File, content: String) {
        val temp = File(target.parentFile, target.name + ".tmp")
        val backup = File(target.parentFile, target.name + ".bak")
        temp.writeText(content)
        if (backup.exists()) backup.delete()
        val hadOriginal = target.exists()
        if (hadOriginal && !target.renameTo(backup)) {
            temp.delete()
            throw IllegalStateException("د پخوانۍ پروژې خوندي کاپي نه جوړېږي")
        }
        if (!temp.renameTo(target)) {
            if (hadOriginal && backup.exists()) backup.renameTo(target)
            temp.delete()
            throw IllegalStateException("د پروژې خوندي کول ناکام شول")
        }
        if (backup.exists()) backup.delete()
    }
}
