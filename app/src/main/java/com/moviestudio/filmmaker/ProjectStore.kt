package com.moviestudio.filmmaker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Stores projects and movie assets locally on the phone only. */
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
        ?.sortedByDescending { it.optString("updatedAt") }
        ?: emptyList()

    fun create(name: String): JSONObject {
        val id = "project_" + System.currentTimeMillis().toString()
        val safeName = name.trim().ifBlank { "نوی فلم" }
        val folder = File(root, id).apply { mkdirs() }
        listOf("assets/characters", "assets/environments", "assets/props",
            "assets/images", "assets/audio", "exports").forEach {
            File(folder, it).mkdirs()
        }
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val project = JSONObject()
            .put("schemaVersion", 1)
            .put("project", JSONObject()
                .put("id", id)
                .put("name", safeName)
                .put("createdAt", now)
                .put("updatedAt", now)
                .put("settings", JSONObject()
                    .put("width", JSONObject.NULL)
                    .put("height", JSONObject.NULL)
                    .put("fps", JSONObject.NULL)))
            .put("assets", JSONArray())
            .put("scenes", JSONArray())
            .put("timeline", JSONObject().put("sceneOrder", JSONArray()))
        writeSafely(File(folder, "project.json"), project.toString(2))
        return project
    }

    private fun writeSafely(target: File, content: String) {
        val temp = File(target.parentFile, target.name + ".tmp")
        temp.writeText(content)
        if (target.exists() && !target.delete()) throw IllegalStateException("د پروژې پخوانی فایل نه حذفېږي")
        if (!temp.renameTo(target)) {
            temp.delete()
            throw IllegalStateException("د پروژې خوندي کول ناکام شول")
        }
    }
}
