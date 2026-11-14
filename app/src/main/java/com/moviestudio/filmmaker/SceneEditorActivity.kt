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
    private lateinit var cameraPanel: LinearLayout
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
        content.addView(label("د فلم د سټوډیو تنظیمات", 17, true))
        content.addView(button("📐 د ځمکې اندازه او د فلم وخت تنظیمول") { editProductionSettings() })
        content.addView(label("د صحنې توکي", 17, true))
        content.addView(label("د انسان، حیوان، موټر، کور، ونې، فرنیچر او نورو شیانو حقیقي 3D ماډلونه د GLB 2.0 په بڼه وارد کړه. کارتوني نمونه‌یي شکلونه د فلم د توکو په توګه نه کارېږي.", 12))
        content.addView(button("＋ حقیقي 3D توکی واردول (GLB)") { chooseModelType() })
        content.addView(button("🌐 د حقیقي 3D ماډلونو وړیا کتابتون") { openModelLibrary() })
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
        content.addView(label("د ثبت شوو Timeline کلیپونو حرکتونه په حقیقي 3D صحنه کې د وارد شوي GLB د animation نومونو له مخې پلی کېږي؛ د حرکت لپاره ماډل باید خپل skeletal animation ولري.", 12))
        val motionRow = horizontal()
        motionRow.addView(button("ولاړ") { addClip("idle") }, weighted())
        motionRow.addView(button("قدم") { addClip("walk") }, weighted())
        motionRow.addView(button("منډه") { addClip("run") }, weighted())
        motionRow.addView(button("خبرې") { addClip("talk") }, weighted())
        content.addView(motionRow)
        content.addView(button("🕒 د کرکټر دقیق وخت، حرکت او سرعت تنظیمول") { chooseTimedMotion() })
        timelinePanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(timelinePanel)

        content.addView(label("د فلم کمرې او شاټونه", 17, true).apply { setPadding(0, dp(14), 0, dp(4)) })
        content.addView(label("د کمرې نوم، ځای، د لید هدف، FOV او د شاټ وخت ثبت کړه. ثبت شوي شاټونه د ټولې 3D صحنې په لید کې لاسي فعالولای شې؛ د وخت له مخې اتومات بدلون او وروستی MP4 رینډر لا پاتې دي.", 12))
        content.addView(button("＋ نوې کمره / شاټ اضافه کړه") { addCameraShotDialog() })
        cameraPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(cameraPanel)

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
            val lineText = if (clip.has("startTime")) {
                "${clip.optString("action")}  •  ${clip.optString("startTime")} – ${clip.optString("endTime")}  •  سرعت ${clip.optDouble("speed", 1.0)}x"
            } else {
                "${clip.optString("action")}  •  ${clip.optInt("startFrame")}–${clip.optInt("startFrame") + clip.optInt("durationFrames")} فریم"
            }
            val line = label(lineText, 13)
            timelinePanel.addView(line)
            timelinePanel.addView(button("دا کلیپ حذف کړه") {
                try {
                    store.removeTimelineClip(projectId, clip.optString("id"))
                    refreshPanels()
                } catch (error: Exception) { toast(error.message ?: "حذف ناکام شو") }
            })
        }
        cameraPanel.removeAllViews()
        val cameras = scene.optJSONArray("cameraShots") ?: JSONArray()
        for (i in 0 until cameras.length()) {
            val shot = cameras.optJSONObject(i) ?: continue
            cameraPanel.addView(label(
                "🎥 ${shot.optString("name")} • ${shot.optString("startTime")} – ${shot.optString("endTime")}\nځای: ${shot.optDouble("x")}, ${shot.optDouble("y")}, ${shot.optDouble("z")} • FOV ${shot.optDouble("fieldOfView")}°",
                12
            ))
            cameraPanel.addView(button("✏️ د دې کمرې شاټ سمول") { addCameraShotDialog(shot) })
            cameraPanel.addView(button("دا کمره / شاټ حذف کړه") {
                try {
                    store.removeCameraShot(projectId, shot.optString("id"))
                    refreshPanels()
                    status.text = "د کمرې شاټ حذف شو."
                } catch (error: Exception) { toast(error.message ?: "د کمرې شاټ حذف نه شو") }
            })
        }
        if (cameras.length() == 0) cameraPanel.addView(label("تر اوسه د کمرې شاټ نشته.", 12))

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

    private fun openModelLibrary() {
        val names = arrayOf(
            "حقیقي انسان — Daniel (CC0، 26.3 MB، ثابت ماډل)",
            "د حرکت لپاره 53-هډوکيز انسان بنسټ (CC0، بې لباسه)",
            "د 163-هډوکيز انسان بنسټ (CC0، 4.3 MB؛ بې حرکت)",
            "د کورونو او ودانیو ماډلونه (CC BY 4.0)",
            "حقیقي موټر — Car Concept (CC BY 4.0)",
            "تاریخي او سیمه‌ییزې ودانۍ (CC BY-SA 4.0)",
            "د Khronos وړیا GLB ماډلونو کتلاګ (د هر ماډل جواز وګوره)",
            "د Poly Haven فوتوریالیستي ماډلونه (CC0؛ GLB بڼه وګوره)"
        )
        val urls = arrayOf(
            "https://www.innerscene.com/tools/library/3d-parts/daniel-3d-person-standing-casual-f788af5a",
            "https://www.innerscene.com/tools/library/3d-parts/human-base-mesh-with-editable-53-bone-rig-8e7c8ab1",
            "https://www.innerscene.com/tools/library/3d-parts/male-base-mesh-3d-person-rigged-a-pose-380e6d89",
            "https://www.sloyd.ai/free-3d-models/architecture-buildings/houses-homes",
            "https://github.com/KhronosGroup/glTF-Sample-Assets/blob/main/Models/CarConcept/glTF-Binary/CarConcept.glb",
            "https://github.com/bilol-makhmudov/monuments",
            "https://github.khronos.org/glTF-Assets/",
            "https://polyhaven.com/models"
        )
        AlertDialog.Builder(this)
            .setTitle("د حقیقي 3D ماډلونو کتابتون")
            .setMessage("له سرچینې څخه GLB فایل ډاونلوډ کړه، بیا بېرته فلم سټوډیو ته راشه او د «حقیقي 3D توکی واردول» تڼۍ وکاروه. ماډلونه د پروژې په محلي Film Studio فولډر کې ساتل کېږي. د هر ماډل د جواز او د لیکوال د کریډیټ شرطونه مراعت کړه. پام: ځینې انسان ماډلونه یوازې rig لري، خو تیار animation نه لري؛ د حرکت لپاره باید GLB خپله animation ولري.")
            .setItems(names) { _, which ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(urls[which])))
                } catch (_: Exception) {
                    toast("د ماډل سرچینه نه پرانیستل کېږي؛ انټرنېټ او براوزر وڅېړه")
                }
            }
            .setNegativeButton("بند", null)
            .show()
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
        val projectFolder = store.projectDirectory(projectId).canonicalFile
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

    private fun addCameraShotDialog(existing: JSONObject? = null) {
        val scene = store.load(projectId).optJSONArray("scenes")?.optJSONObject(0) ?: JSONObject()
        val production = scene.optJSONObject("production") ?: JSONObject()
        val fields = listOf(
            EditText(this).apply { setText(existing?.optString("name") ?: "اصلي کمره"); setSingleLine(true) },
            EditText(this).apply { setText(existing?.optDouble("x", 0.0)?.toString() ?: "0") },
            EditText(this).apply { setText(existing?.optDouble("y", 3.0)?.toString() ?: "3") },
            EditText(this).apply { setText(existing?.optDouble("z", 5.0)?.toString() ?: "5") },
            EditText(this).apply { setText(existing?.optDouble("targetX", 0.0)?.toString() ?: "0") },
            EditText(this).apply { setText(existing?.optDouble("targetY", 1.0)?.toString() ?: "1") },
            EditText(this).apply { setText(existing?.optDouble("targetZ", 0.0)?.toString() ?: "0") },
            EditText(this).apply { setText(existing?.optDouble("fieldOfView", 50.0)?.toString() ?: "50") },
            EditText(this).apply { setText(existing?.optString("startTime") ?: production.optString("startTime", "09:00:00")); setSingleLine(true) },
            EditText(this).apply { setText(existing?.optString("endTime") ?: production.optString("endTime", "09:30:00")); setSingleLine(true) }
        )
        val captions = listOf(
            "د کمرې نوم", "د کمرې X", "د کمرې Y", "د کمرې Z",
            "د لید هدف X", "د لید هدف Y", "د لید هدف Z",
            "د لید زاویه FOV (۲۰–۱۰۰)", "د شاټ پیل (HH:mm:ss)", "د شاټ پای (HH:mm:ss)"
        )
        fields.drop(1).take(7).forEach {
            it.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(4))
        }
        fields.forEachIndexed { index, field ->
            panel.addView(label(captions[index], 12, true))
            panel.addView(field)
        }
        AlertDialog.Builder(this).setTitle(if (existing == null) "د فلم کمره ځای پر ځای کول" else "د کمرې شاټ سمول")
            .setView(ScrollView(this).apply { addView(panel) })
            .setNegativeButton("لغوه", null)
            .setPositiveButton("کمره ثبت کړه") { _, _ ->
                try {
                    fun number(index: Int): Double = fields[index].text.toString().toDoubleOrNull()
                        ?: throw IllegalArgumentException("د ${captions[index]} شمېره ناسمه ده")
                    if (existing == null) {
                        store.addCameraShot(
                            projectId, fields[0].text.toString(),
                            number(1), number(2), number(3),
                            number(4), number(5), number(6),
                            number(7), fields[8].text.toString().trim(), fields[9].text.toString().trim()
                        )
                    } else {
                        store.updateCameraShot(
                            projectId, existing.optString("id"), fields[0].text.toString(),
                            number(1), number(2), number(3),
                            number(4), number(5), number(6),
                            number(7), fields[8].text.toString().trim(), fields[9].text.toString().trim()
                        )
                    }
                    refreshPanels()
                    status.text = "د کمرې ځای او شاټ وخت خوندي شو؛ د وخت له مخې اتومات کټ او MP4 رینډر لا پاتې دي."
                } catch (error: Exception) { toast(error.message ?: "کمره ثبت نه شوه") }
            }.show()
    }

    private fun editProductionSettings() {
        val scene = store.load(projectId).optJSONArray("scenes")?.optJSONObject(0) ?: JSONObject()
        val ground = scene.optJSONObject("ground") ?: JSONObject()
        val production = scene.optJSONObject("production") ?: JSONObject()
        val width = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(ground.optDouble("widthMeters", 15.0).toString())
        }
        val depth = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(ground.optDouble("depthMeters", 10.0).toString())
        }
        val start = EditText(this).apply { setSingleLine(true); setText(production.optString("startTime", "09:00:00")) }
        val end = EditText(this).apply { setSingleLine(true); setText(production.optString("endTime", "09:30:00")) }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(4))
        }
        listOf(
            "د ځمکې پلنوالی (متر)" to width,
            "د ځمکې اوږدوالی (متر)" to depth,
            "د فلم پیل (HH:mm:ss)" to start,
            "د فلم پای (HH:mm:ss)" to end
        ).forEach { (caption, field) ->
            panel.addView(label(caption, 12, true))
            panel.addView(field)
        }
        AlertDialog.Builder(this).setTitle("د فلم د تولید بنسټیز تنظیمات")
            .setView(ScrollView(this).apply { addView(panel) })
            .setNegativeButton("لغوه", null)
            .setPositiveButton("تنظیمات خوندي کړه") { _, _ ->
                try {
                    val w = width.text.toString().toDoubleOrNull()
                        ?: throw IllegalArgumentException("د ځمکې پلنوالی سم ولیکه")
                    val d = depth.text.toString().toDoubleOrNull()
                        ?: throw IllegalArgumentException("د ځمکې اوږدوالی سم ولیکه")
                    val saved = store.updateProductionSettings(
                        projectId, w, d, start.text.toString().trim(), end.text.toString().trim()
                    )
                    val area = saved.optJSONObject("ground")?.optDouble("areaSquareMeters") ?: (w * d)
                    status.text = "د ځمکې ساحه ${area} m² او د فلم وخت خوندي شو."
                    refreshPanels()
                } catch (error: Exception) { toast(error.message ?: "تنظیمات خوندي نه شول") }
            }.show()
    }

    private fun chooseTimedMotion() {
        val id = selectedObjectId ?: run { toast("لومړی کرکټر یا توکی وټاکه"); return }
        val labels = arrayOf("ولاړ", "قدم وهل", "منډه", "خبرې", "ځانګړی حرکت")
        val actions = arrayOf("idle", "walk", "run", "talk", "custom")
        AlertDialog.Builder(this).setTitle("د کوم حرکت وخت تنظیموې؟")
            .setItems(labels) { _, which -> showTimedMotionEditor(id, actions[which]) }
            .setNegativeButton("لغوه", null).show()
    }

    private fun showTimedMotionEditor(objectId: String, action: String) {
        val start = EditText(this).apply { setSingleLine(true); setText("09:01:00"); hint = "09:01:00" }
        val end = EditText(this).apply { setSingleLine(true); setText("09:05:06"); hint = "09:05:06" }
        val speed = EditText(this).apply {
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText("1.0")
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(4))
        }
        listOf("د حرکت پیل (HH:mm:ss)" to start, "د حرکت پای/سکون (HH:mm:ss)" to end, "د حرکت سرعت (۰.۱ تر ۵)" to speed)
            .forEach { (caption, field) ->
                panel.addView(label(caption, 12, true))
                panel.addView(field)
            }
        AlertDialog.Builder(this).setTitle("د کرکټر د حرکت دقیق مهالویش")
            .setView(panel).setNegativeButton("لغوه", null)
            .setPositiveButton("Timeline ته ثبت کړه") { _, _ ->
                try {
                    val velocity = speed.text.toString().toDoubleOrNull()
                        ?: throw IllegalArgumentException("د حرکت سرعت سم ولیکه")
                    store.addTimedTimelineClip(
                        projectId, objectId, action,
                        start.text.toString().trim(), end.text.toString().trim(), velocity
                    )
                    refreshPanels()
                    status.text = "حرکت د ټاکلي ساعت، پای او سرعت سره Timeline ته ثبت شو؛ د 3D صحنې د لید له «پلې / درول» کنټرول څخه یې وګوره."
                } catch (error: Exception) { toast(error.message ?: "د حرکت مهالویش ثبت نه شو") }
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
                    status.text = "د $action کلیپ د $frames فریمونو مودې سره ثبت شو؛ د 3D صحنې په لید کې یې د پلې / درول له لارې وګوره."
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
