package com.moviestudio.filmmaker

import android.app.Activity
import android.content.Intent
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject

/** Local-first scene editor: objects and timeline metadata are persisted in project.json. */
class SceneEditorActivity : Activity() {
    private lateinit var store: ProjectStore
    private lateinit var projectId: String
    private lateinit var previewNotice: TextView
    private lateinit var objectPanel: LinearLayout
    private lateinit var timelinePanel: LinearLayout
    private lateinit var status: TextView
    private var selectedObjectId: String? = null
    private var selectedObject: JSONObject? = null
    private var pendingModelType = "character"

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ProjectStore(this)
        projectId = intent.getStringExtra(EXTRA_PROJECT_ID).orEmpty()
        if (projectId.isBlank()) { finish(); return }
        try { store.load(projectId) } catch (_: Exception) {
            Toast.makeText(this, "پروژه ونه موندل شوه", Toast.LENGTH_LONG).show()
            finish(); return
        }
        buildEditor()
        refreshPanels()
    }

    private fun buildEditor() {
        window.statusBarColor = Color.rgb(12, 18, 34)
        window.navigationBarColor = Color.rgb(12, 18, 34)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(0xFF0C1222.toInt())
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        header.addView(button("شاته") { finish() }, LinearLayout.LayoutParams(dp(78), dp(46)))
        val projectName = store.load(projectId).optJSONObject("project")?.optString("name") ?: "فلم"
        header.addView(label("🎬 $projectName", 18, true), LinearLayout.LayoutParams(0, dp(46), 1f))
        header.addView(button("خوندي") { saveAndRefresh() }, LinearLayout.LayoutParams(dp(86), dp(46)))
        root.addView(header)

        previewNotice = label("دلته به د کارتوني نمونې پر ځای یوازې د وارد شوي حقیقي GLB ماډل بشپړ 3D لید پرانیستل کېږي.", 13, true).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF17243A.toInt())
            minHeight = dp(86)
        }
        root.addView(previewNotice, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(86)
        ))

        val scroll = ScrollView(this).apply { isFillViewport = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(18))
        }
        content.addView(label("د صحنې توکي", 17, true))
        content.addView(label("د انسان، حیوان، موټر، کور، ونې، فرنیچر او نورو شیانو حقیقي 3D ماډلونه د GLB 2.0 په بڼه وارد کړه. کارتوني نمونه‌یي شکلونه د فلم د توکو په توګه نه کارېږي.", 12))
        content.addView(button("＋ حقیقي 3D توکی واردول (GLB)") { chooseModelType() })
        content.addView(button("ټاکل شوی حقیقي ماډل په 3D کې وګوره") { openSelectedRealisticModel() })
        content.addView(button("ټول حقیقي توکي په یوه 3D صحنه کې وګوره") { openWholeScene() })
        objectPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(objectPanel)

        content.addView(label("د ټاکل شوي توکي بدلون", 17, true).apply { setPadding(0, dp(12), 0, dp(4)) })
        val transformRow = horizontal()
        transformRow.addView(button("X −") { nudge("x", -0.25) }, weighted())
        transformRow.addView(button("X ＋") { nudge("x", 0.25) }, weighted())
        transformRow.addView(button("Y −") { nudge("y", -0.25) }, weighted())
        transformRow.addView(button("Y ＋") { nudge("y", 0.25) }, weighted())
        content.addView(transformRow)
        val depthRow = horizontal()
        depthRow.addView(button("Z نږدې") { nudge("z", 0.25) }, weighted())
        depthRow.addView(button("Z لرې") { nudge("z", -0.25) }, weighted())
        depthRow.addView(button("دقیق بدلون") { editExactTransform() }, weighted())
        content.addView(depthRow)
        content.addView(label("د توکي څرخول • هر ځل ۱۵ درجې", 13, true))
        val rotateX = horizontal()
        rotateX.addView(button("X ↶") { rotate("rotationX", -15.0) }, weighted())
        rotateX.addView(button("X ↷") { rotate("rotationX", 15.0) }, weighted())
        rotateX.addView(button("Y ↶") { rotate("rotationY", -15.0) }, weighted())
        rotateX.addView(button("Y ↷") { rotate("rotationY", 15.0) }, weighted())
        content.addView(rotateX)
        val rotateZ = horizontal()
        rotateZ.addView(button("Z ↶") { rotate("rotationZ", -15.0) }, weighted())
        rotateZ.addView(button("Z ↷") { rotate("rotationZ", 15.0) }, weighted())
        rotateZ.addView(button("بېرته صفر") { resetRotation() }, weighted())
        content.addView(rotateZ)
        val scaleRow = horizontal()
        scaleRow.addView(button("کوچنی −") { scale(-0.1) }, weighted())
        scaleRow.addView(button("لوی ＋") { scale(0.1) }, weighted())
        scaleRow.addView(button("حذف") { deleteSelected() }, weighted())
        content.addView(scaleRow)
        val editRow = horizontal()
        editRow.addView(button("نوم بدلول") { renameSelected() }, weighted())
        editRow.addView(button("کاپي") { duplicateSelected() }, weighted())
        content.addView(editRow)

        content.addView(label("د حرکت Timeline", 17, true).apply { setPadding(0, dp(14), 0, dp(4)) })
        content.addView(label("دا مهال د Timeline کلیپونه یوازې د حرکت پلان ثبتوي؛ که وارد شوی GLB خپل هډوکي او animation ولري، لومړی حرکت یې په 3D مخکتنه کې پلی کېږي.", 12))
        val motionRow = horizontal()
        motionRow.addView(button("ولاړ") { addClip("idle") }, weighted())
        motionRow.addView(button("قدم") { addClip("walk") }, weighted())
        motionRow.addView(button("منډه") { addClip("run") }, weighted())
        motionRow.addView(button("خبرې") { addClip("talk") }, weighted())
        content.addView(motionRow)
        timelinePanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(timelinePanel)

        status = label("ټول بدلونونه محلي دي.", 12)
        status.setPadding(0, dp(10), 0, dp(4))
        content.addView(status)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(360)
        ))
        setContentView(root)
    }

    private fun refreshPanels() {
        val project = store.load(projectId)
        val scene = project.optJSONArray("scenes")?.optJSONObject(0) ?: JSONObject()
        val objects = scene.optJSONArray("objects") ?: JSONArray()
        val preferredSelection = selectedObjectId
        objectPanel.removeAllViews()
        selectedObject = null
        selectedObjectId = null
        for (i in 0 until objects.length()) {
            val item = objects.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val type = item.optString("type")
            val name = item.optString("name", type)
            val row = horizontal()
            val select = button((if (id == preferredSelection) "✓ " else "") + name + " • " + type) {
                selectedObjectId = id
                selectedObject = item
                applySelectedTransform()
                status.text = "ټاکل شوی: $name"
                refreshObjectSelection()
            }
            row.addView(select, LinearLayout.LayoutParams(0, dp(48), 1f))
            objectPanel.addView(row)
        }
        timelinePanel.removeAllViews()
        val clips = scene.optJSONArray("timeline") ?: JSONArray()
        for (i in 0 until clips.length()) {
            val clip = clips.optJSONObject(i) ?: continue
            val line = label(
                "${clip.optString("action")}  •  ${clip.optInt("startFrame")}–${clip.optInt("startFrame") + clip.optInt("durationFrames")} فریم",
                13
            )
            timelinePanel.addView(line)
            timelinePanel.addView(button("دا کلیپ حذف کړه") {
                try {
                    store.removeTimelineClip(projectId, clip.optString("id"))
                    refreshPanels()
                } catch (error: Exception) { toast(error.message ?: "حذف ناکام شو") }
            })
        }
        val selectedIndex = (0 until objects.length()).firstOrNull {
            objects.optJSONObject(it)?.optString("id") == preferredSelection
        } ?: if (objects.length() > 0) 0 else -1
        if (selectedIndex >= 0) {
            selectedObject = objects.optJSONObject(selectedIndex)
            selectedObjectId = selectedObject?.optString("id")
            applySelectedTransform()
        }
        if (objects.length() == 0) objectPanel.addView(label("تر اوسه توکي نشته؛ له پورته تڼیو یو توکی ورزیات کړه.", 13))
        if (clips.length() == 0) timelinePanel.addView(label("Timeline تش دی؛ لومړی یو توکی وټاکه او حرکت ورزیات کړه.", 13))
    }

    private fun refreshObjectSelection() {
        val project = store.load(projectId)
        val scene = project.optJSONArray("scenes")?.optJSONObject(0) ?: return
        val objects = scene.optJSONArray("objects") ?: return
        objectPanel.removeAllViews()
        for (i in 0 until objects.length()) {
            val item = objects.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val row = horizontal()
            row.addView(button((if (id == selectedObjectId) "✓ " else "") + item.optString("name")) {
                selectedObjectId = id
                selectedObject = item
                    applySelectedTransform()
                refreshObjectSelection()
            }, LinearLayout.LayoutParams(0, dp(46), 1f))
            objectPanel.addView(row)
        }
    }

    private fun chooseModelType() {
        val labels = arrayOf(
            "انسان", "حیوان", "موټر / وسیله", "کور / ودانۍ", "ونه / بوټی", "فرنیچر",
            "جامې او لوازم", "نمایشي وسله", "کمره", "د کار وسایل", "طبي وسایل",
            "خوراک او څښاک", "د دفتر وسایل", "سپورټ", "د سفر وسایل",
            "د سړک توکي", "برېښنايي وسایل", "چاپېریال", "نور شیان"
        )
        val types = arrayOf(
            "character", "animal", "vehicle", "building", "tree", "furniture",
            "clothing", "weapon", "camera", "tool", "medical", "food",
            "office", "sports", "travel", "road", "electronics", "environment", "other"
        )
        AlertDialog.Builder(this)
            .setTitle("کوم حقیقي 3D توکی واردوې؟")
            .setItems(labels) { _, which -> pendingModelType = types[which]; pickRealisticModel() }
            .setNegativeButton("لغوه", null)
            .show()
    }

    private fun pickRealisticModel() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("model/gltf-binary", "application/octet-stream"))
        }
        startActivityForResult(intent, REQUEST_IMPORT_GLB)
    }

    private fun openWholeScene() {
        startActivity(Intent(this, MultiModelSceneActivity::class.java)
            .putExtra(MultiModelSceneActivity.EXTRA_PROJECT_ID, projectId))
    }

    private fun openSelectedRealisticModel() {
        val item = selectedObject ?: run { toast("لومړی یو وارد شوی حقیقي 3D ماډل وټاکه"); return }
        val relativePath = item.optString("modelPath")
        if (relativePath.isBlank()) {
            toast("دا پخوانی ساده توکی دی؛ د حقیقي لید لپاره GLB ماډل وارد کړه")
            return
        }
        val projectFolder = java.io.File(filesDir, "MovieProjects/$projectId").canonicalFile
        val file = java.io.File(projectFolder, relativePath).canonicalFile
        if (!file.path.startsWith(projectFolder.path + java.io.File.separator) || !file.isFile) {
            toast("د 3D ماډل GLB فایل ونه موندل شو")
            return
        }
        startActivity(Intent(this, RealisticModelPreviewActivity::class.java)
            .putExtra(RealisticModelPreviewActivity.EXTRA_MODEL_PATH, file.absolutePath)
            .putExtra(RealisticModelPreviewActivity.EXTRA_MODEL_NAME, item.optString("name"))
            .putExtra(RealisticModelPreviewActivity.EXTRA_X, item.optDouble("x").toFloat())
            .putExtra(RealisticModelPreviewActivity.EXTRA_Y, item.optDouble("y").toFloat())
            .putExtra(RealisticModelPreviewActivity.EXTRA_Z, item.optDouble("z").toFloat())
            .putExtra(RealisticModelPreviewActivity.EXTRA_SCALE, item.optDouble("scale", 1.0).toFloat())
            .putExtra(RealisticModelPreviewActivity.EXTRA_ROTATION_X, item.optDouble("rotationX").toFloat())
            .putExtra(RealisticModelPreviewActivity.EXTRA_ROTATION_Y, item.optDouble("rotationY").toFloat())
            .putExtra(RealisticModelPreviewActivity.EXTRA_ROTATION_Z, item.optDouble("rotationZ").toFloat()))
    }

    @Deprecated("Uses the compatible document picker API for older Android versions")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_IMPORT_GLB || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            val stream = contentResolver.openInputStream(uri) ?: throw IllegalStateException("فایل نه پرانیستل کېږي")
            val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "3D-${pendingModelType}.glb"
            val imported = store.importSceneModel(projectId, stream, name, pendingModelType)
            selectedObjectId = imported.optString("id")
            selectedObject = imported
            refreshPanels()
            status.text = "حقیقي GLB ماډل په موبایل کې وارد او خوندي شو."
            toast("حقیقي 3D ماډل وارد شو")
        } catch (error: Exception) {
            toast(error.message ?: "د حقیقي 3D ماډل واردول ناکام شول")
        }
    }

    private fun nudge(axis: String, delta: Double) {
        val item = selectedObject ?: run { toast("لومړی یو توکی وټاکه"); return }
        val changes = JSONObject().put(axis, item.optDouble(axis) + delta)
        updateSelected(changes)
    }

    private fun scale(delta: Double) {
        val item = selectedObject ?: run { toast("لومړی یو توکی وټاکه"); return }
        updateSelected(JSONObject().put("scale", item.optDouble("scale", 1.0) + delta))
    }

    private fun rotate(axis: String, delta: Double) {
        val item = selectedObject ?: run { toast("لومړی یو توکی وټاکه"); return }
        updateSelected(JSONObject().put(axis, item.optDouble(axis) + delta))
    }

    private fun resetRotation() {
        updateSelected(JSONObject().put("rotationX", 0).put("rotationY", 0).put("rotationZ", 0))
    }

    private fun editExactTransform() {
        val item = selectedObject ?: run { toast("لومړی یو توکی وټاکه"); return }
        val fields = listOf("x", "y", "z", "scale", "rotationX", "rotationY", "rotationZ")
        val labels = listOf("X موقعیت", "Y موقعیت", "Z موقعیت", "اندازه", "X څرخول", "Y څرخول", "Z څرخول")
        val inputs = fields.map { key ->
            EditText(this).apply {
                setSingleLine(true)
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(item.optDouble(key, if (key == "scale") 1.0 else 0.0).toString())
            }
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(4))
        }
        inputs.forEachIndexed { index, input ->
            panel.addView(label(labels[index], 12, true))
            panel.addView(input)
        }
        AlertDialog.Builder(this).setTitle("د توکي دقیق موقعیت، اندازه او څرخول")
            .setView(ScrollView(this).apply { addView(panel) })
            .setNegativeButton("لغوه", null)
            .setPositiveButton("خوندي کول") { _, _ ->
                try {
                    val changes = JSONObject()
                    fields.forEachIndexed { index, key ->
                        val value = inputs[index].text.toString().toDoubleOrNull()
                            ?: throw IllegalArgumentException("د ${labels[index]} شمېره ناسمه ده")
                        changes.put(key, value)
                    }
                    updateSelected(changes)
                } catch (error: Exception) { toast(error.message ?: "بدلون خوندي نه شو") }
            }.show()
    }

    private fun updateSelected(changes: JSONObject) {
        val id = selectedObjectId ?: run { toast("لومړی یو توکی وټاکه"); return }
        try {
            selectedObject = store.updateSceneObject(projectId, id, changes)
            applySelectedTransform()
            refreshObjectSelection()
            status.text = "بدلون خوندي شو."
        } catch (error: Exception) { toast(error.message ?: "بدلون خوندي نه شو") }
    }

    private fun applySelectedTransform() {
        val item = selectedObject ?: run {
            previewNotice.text = "د صحنې د حقیقي لید لپاره لومړی GLB 2.0 ماډل وارد کړه."
            return
        }
        previewNotice.text = if (item.optString("modelPath").isNotBlank()) {
            "حقیقي 3D ماډل ټاکل شوی: ${item.optString("name")}\nد بشپړ لید لپاره د «3D کې وګوره» تڼۍ وکاروه."
        } else {
            "دا پخوانی ساده توکی دی؛ کارتوني یا هندسي انسان نه ښودل کېږي. د حقیقي ماډل لپاره GLB وارد کړه."
        }
    }

    private fun renameSelected() {
        val item = selectedObject ?: run { toast("لومړی یو توکی وټاکه"); return }
        val input = EditText(this).apply {
            setSingleLine(true)
            setText(item.optString("name"))
            setSelection(text.length)
        }
        AlertDialog.Builder(this).setTitle("د توکي نوم بدلول")
            .setView(input).setNegativeButton("لغوه", null)
            .setPositiveButton("خوندي کول") { _, _ ->
                val id = selectedObjectId ?: return@setPositiveButton
                try {
                    selectedObject = store.updateSceneObject(projectId, id,
                        JSONObject().put("name", input.text.toString().trim()))
                    refreshPanels()
                    status.text = "د توکي نوم بدل او خوندي شو."
                } catch (error: Exception) { toast(error.message ?: "نوم بدل نه شو") }
            }.show()
    }

    private fun duplicateSelected() {
        val id = selectedObjectId ?: run { toast("لومړی یو توکی وټاکه"); return }
        try {
            val copy = store.duplicateSceneObject(projectId, id)
            selectedObjectId = copy.optString("id")
            selectedObject = copy
            refreshPanels()
            status.text = "د توکي کاپي جوړه او خوندي شوه."
        } catch (error: Exception) { toast(error.message ?: "کاپي نه شوه") }
    }

    private fun deleteSelected() {
        val id = selectedObjectId ?: run { toast("لومړی یو توکی وټاکه"); return }
        AlertDialog.Builder(this).setTitle("توکی حذفول")
            .setMessage("ټاکل شوی توکی او د هغه د Timeline کلیپونه حذف شي؟")
            .setNegativeButton("نه", null)
            .setPositiveButton("هو") { _, _ ->
                try {
                    store.removeSceneObject(projectId, id)
                    selectedObjectId = null; selectedObject = null
                    refreshPanels()
                    status.text = "توکی حذف شو."
                } catch (error: Exception) { toast(error.message ?: "حذف ناکام شو") }
            }.show()
    }

    private fun addClip(action: String) {
        val id = selectedObjectId ?: run { toast("لومړی یو کرکټر یا توکی وټاکه"); return }
        val durationInput = EditText(this).apply {
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("48")
            hint = "د فریمونو شمېر"
        }
        val prompt = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(label("د کلیپ موده په فریمونو کې (۲۴ فریم = ۱ ثانیه)", 13))
            addView(durationInput)
        }
        AlertDialog.Builder(this).setTitle("د حرکت کلیپ موده")
            .setView(prompt)
            .setNegativeButton("لغوه", null)
            .setPositiveButton("Timeline ته ورزیاتول") { _, _ ->
                val frames = durationInput.text.toString().toIntOrNull()
                if (frames == null || frames !in 1..2400) {
                    toast("موده باید له ۱ تر ۲۴۰۰ فریمونو وي")
                    return@setPositiveButton
                }
                try {
                    store.addTimelineClip(projectId, id, action, frames)
                    refreshPanels()
                    status.text = "د $action کلیپ د $frames فریمونو مودې سره ثبت شو. دا لا د حقیقي skeletal playback سره نه دی تړل شوی."
                } catch (error: Exception) { toast(error.message ?: "کلیپ نه شو ورزیاتېدای") }
            }.show()
    }

    private fun saveAndRefresh() {
        try { store.save(store.load(projectId)); status.text = "پروژه په موبایل کې خوندي شوه." }
        catch (error: Exception) { toast(error.message ?: "خوندي کول ناکام شول") }
    }

    private fun horizontal() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    private fun weighted() = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
        marginStart = dp(4); marginEnd = dp(4)
    }

    private fun label(value: String, size: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(Color.WHITE)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(dp(5), dp(5), dp(5), dp(5))
    }

    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value; isAllCaps = false; textSize = 13f; setTextColor(Color.WHITE)
        minHeight = dp(44)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xFF344F83.toInt()); setStroke(dp(1), 0xFF718BB9.toInt())
            cornerRadius = dp(12).toFloat()
        }
        setOnClickListener { action() }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    companion object { const val EXTRA_PROJECT_ID = "project_id"; private const val REQUEST_IMPORT_GLB = 7301 }
}
