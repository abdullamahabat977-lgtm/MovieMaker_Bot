package com.moviestudio.filmmaker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Stores all movie projects and their assets locally on the phone only. */
class ProjectStore(context: Context) {
    private val root = File(context.filesDir, "MovieProjects").apply { mkdirs() }

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
            .put("timeline", JSONObject().put("sceneOrder", JSONArray().put("scene_1")))
        writeSafely(File(folder, "project.json"), project.toString(2))
        return project
    }

    fun load(projectId: String): JSONObject {
        val file = File(projectFolder(projectId), "project.json")
        require(file.isFile) { "پروژه ونه موندل شوه" }
        return JSONObject(file.readText())
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

    fun addSceneObject(projectId: String, type: String, label: String): JSONObject {
        require(type in setOf("character", "cube", "environment", "prop")) { "د توکي ډول ناسم دی" }
        val project = load(projectId)
        val scene = firstScene(project)
        val objects = scene.optJSONArray("objects") ?: JSONArray().also { scene.put("objects", it) }
        val objectId = "object_" + System.currentTimeMillis().toString()
        val item = JSONObject()
            .put("id", objectId).put("type", type).put("name", label.take(60))
            .put("x", 0.0).put("y", 0.0).put("z", 0.0).put("scale", 1.0)
            .put("skin", 1).put("shirt", 0)
        objects.put(item)
        save(project)
        return item
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
        listOf("name", "x", "y", "z", "scale", "skin", "shirt").forEach { key ->
            if (changes.has(key)) item.put(key, changes.get(key))
        }
        item.put("scale", item.optDouble("scale", 1.0).coerceIn(0.25, 3.0))
        item.put("x", item.optDouble("x").coerceIn(-3.0, 3.0))
        item.put("y", item.optDouble("y").coerceIn(-1.0, 3.0))
        item.put("z", item.optDouble("z").coerceIn(-3.0, 3.0))
        save(project)
        return item
    }

    fun removeSceneObject(projectId: String, objectId: String) {
        val project = load(projectId)
        val old = firstScene(project).optJSONArray("objects") ?: JSONArray()
        val kept = JSONArray()
        var removed = false
        for (index in 0 until old.length()) {
            val item = old.optJSONObject(index) ?: continue
            if (item.optString("id") == objectId) removed = true else kept.put(item)
        }
        require(removed) { "د صحنې توکی ونه موندل شو" }
        firstScene(project).put("objects", kept)
        val clips = firstScene(project).optJSONArray("timeline") ?: JSONArray()
        val keptClips = JSONArray()
        for (index in 0 until clips.length()) {
            val clip = clips.optJSONObject(index) ?: continue
            if (clip.optString("objectId") != objectId) keptClips.put(clip)
        }
        firstScene(project).put("timeline", keptClips)
        save(project)
    }

    fun addTimelineClip(projectId: String, objectId: String, action: String, durationFrames: Int = 48): JSONObject {
        require(action in setOf("idle", "walk", "run", "talk", "custom")) { "د حرکت ډول ناسم دی" }
        val project = load(projectId)
        val scene = firstScene(project)
        val clips = scene.optJSONArray("timeline") ?: JSONArray().also { scene.put("timeline", it) }
        val start = (0 until clips.length()).mapNotNull { clips.optJSONObject(it) }
            .maxOfOrNull { it.optInt("startFrame") + it.optInt("durationFrames") } ?: 0
        val clip = JSONObject()
            .put("id", "clip_" + System.currentTimeMillis().toString())
            .put("objectId", objectId).put("action", action)
            .put("startFrame", start).put("durationFrames", durationFrames.coerceIn(1, 2400))
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
        val scene = scenes.optJSONObject(0) ?: JSONObject()
        if (!scene.has("objects")) scene.put("objects", JSONArray())
        if (!scene.has("timeline")) scene.put("timeline", JSONArray())
        return scene
    }

    private fun projectFolder(projectId: String): File {
        require(projectId.matches(Regex("project_[0-9]+"))) { "د پروژې پېژندنه ناسمه ده" }
        val folder = File(root, projectId).canonicalFile
        require(folder.parentFile == root.canonicalFile) { "د پروژې لاره ناسمه ده" }
        return folder
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

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
