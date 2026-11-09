package com.moviestudio.filmmaker

import android.app.Activity
import android.content.Intent
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var accountStatus: TextView
    private lateinit var projectStore: ProjectStore
    private val prefs by lazy { getSharedPreferences("movie_studio_account", MODE_PRIVATE) }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectStore = ProjectStore(this)
        window.statusBarColor = Color.rgb(16, 21, 37)
        window.navigationBarColor = Color.rgb(16, 21, 37)
        buildHome()
        refreshAccount()
    }

    private fun buildHome() {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(16, 21, 37)) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(24), dp(18), dp(28))
        }
        page.addView(text("🎬 فلم سټوډیو", 25, Color.WHITE, true))
        page.addView(text("خپل فلم په خپل لاس جوړ کړه؛ فلمونه او پروژې دې په همدې موبایل کې خوندي کېږي.", 14, 0xFFD7DCEF.toInt()))
        accountStatus = text("د حساب حالت: نه دی نښلول شوی", 14, 0xFFB9C5DE.toInt(), true)
        accountStatus.setPadding(dp(12), dp(14), dp(12), dp(14))
        page.addView(accountStatus, marginParams(top = 18, bottom = 8))
        addButton(page, "ټیلیګرام حساب ونښلوه / بیلانس وګوره") { showAccountDialog() }
        addButton(page, "＋ نوې پروژه جوړه کړه") { createProjectDialog() }
        addButton(page, "📁 زما فلمونه او پروژې") { showProjects() }
        addButton(page, "🧊 3D مخکتنه") { show3DPreview() }
        addButton(page, "د فلم جوړولو نور وسایل") {
            AlertDialog.Builder(this).setTitle("د جوړولو پړاو")
                .setMessage("اوس د نوې نسخې بنسټ جوړېږي. د کرکټرونو، صحنو، حرکتونو، کمرې، غږ او MP4 صادرولو وسایل به په جلا ازمویل کېدونکو پړاوونو کې ورزیاتېږي.")
                .setPositiveButton("سمه ده", null).show()
        }
        page.addView(text("نسخه 2.0.0 • لومړی بنسټیز پړاو", 12, 0xFF8996B2.toInt()).apply {
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        })
        scroll.addView(page)
        setContentView(scroll)
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value; textSize = size.toFloat(); setTextColor(color)
            gravity = android.view.Gravity.CENTER
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(6), dp(7), dp(6), dp(7))
        }

    private fun marginParams(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top); bottomMargin = dp(bottom)
        }

    private fun addButton(parent: LinearLayout, title: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = title; isAllCaps = false; textSize = 15f
            setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(75, 75, 145))
            setOnClickListener { action() }
        }
        parent.addView(button, marginParams(top = 7, bottom = 5))
    }

    private fun showAccountDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }
        box.addView(text("۱. خپل Telegram ID ولیکه. لومړی باید بوټ ته /start استولی وي.", 14, Color.DKGRAY))
        val idInput = EditText(this).apply {
            hint = "Telegram ID"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setSingleLine(true)
        }
        box.addView(idInput)
        box.addView(text("۲. د کوډ غوښتنه وکړه؛ شپږ عددي کوډ به په ټېلېګرام کې درشي.", 13, Color.DKGRAY))
        val codeInput = EditText(this).apply {
            hint = "شپږ عددي تایید کوډ"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setSingleLine(true)
        }
        box.addView(codeInput)
        val status = text("", 13, Color.rgb(40, 80, 60))
        box.addView(status)
        val dialog = AlertDialog.Builder(this).setTitle("د ټېلېګرام حساب نښلول")
            .setView(box).setNegativeButton("بند", null).create()
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "کوډ راولېږه") { _, _ -> }
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "حساب ونښلوه") { _, _ -> }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val id = idInput.text.toString().trim()
                if (!id.matches(Regex("[0-9]{5,20}"))) { status.text = "سم Telegram ID ولیکه."; return@setOnClickListener }
                status.text = "کوډ غوښتل کېږي..."
                worker.execute {
                    try {
                        val result = ApiClient.requestCode(id)
                        mainHandler.post { status.text = if (result.optBoolean("ok")) result.optString("message") else result.optString("error", "کوډ ونه لېږل شو.") }
                    } catch (_: Exception) {
                        mainHandler.post { status.text = "له سرور سره اړیکه ناکامه شوه؛ انټرنېټ وګوره." }
                    }
                }
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val id = idInput.text.toString().trim()
                val code = codeInput.text.toString().trim()
                if (!id.matches(Regex("[0-9]{5,20}")) || !code.matches(Regex("[0-9]{6}"))) {
                    status.text = "Telegram ID او شپږ عددي کوډ سم ولیکه."; return@setOnClickListener
                }
                status.text = "حساب نښلول کېږي..."
                worker.execute {
                    try {
                        val result = ApiClient.verifyCode(id, code)
                        if (result.optBoolean("ok")) {
                            val token = result.optString("session_token")
                            prefs.edit().putString("session_token", token).apply()
                            val profile = ApiClient.me(token)
                            mainHandler.post { updateAccountText(profile); dialog.dismiss() }
                        } else mainHandler.post { status.text = result.optString("error", "حساب ونه نښلول شو.") }
                    } catch (_: Exception) {
                        mainHandler.post { status.text = "له سرور سره اړیکه ناکامه شوه؛ بیا هڅه وکړه." }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun refreshAccount() {
        val token = prefs.getString("session_token", null) ?: run {
            accountStatus.text = "د حساب حالت: نه دی نښلول شوی"; return
        }
        worker.execute {
            try {
                val result = ApiClient.me(token)
                mainHandler.post {
                    if (result.optBoolean("ok")) updateAccountText(result)
                    else { prefs.edit().remove("session_token").apply(); accountStatus.text = "د حساب حالت: بیا نښلول اړین دي" }
                }
            } catch (_: Exception) {
                mainHandler.post { accountStatus.text = "حساب نه دی تازه شوی؛ آفلاین کار دوام لري." }
            }
        }
    }

    private fun updateAccountText(result: JSONObject) {
        val user = result.optJSONObject("user") ?: JSONObject()
        val balance = result.optJSONObject("balance") ?: JSONObject()
        accountStatus.text = "حساب: نښلول شوی\nTelegram ID: ${user.optString("telegram_id")}\nPoints: ${balance.optLong("points")}  |  USDT: ${balance.optDouble("usdt")}"
    }

    private fun createProjectDialog() {
        val input = EditText(this).apply { hint = "د فلم نوم"; setSingleLine(true) }
        AlertDialog.Builder(this).setTitle("نوې پروژه")
            .setMessage("پروژه او فایلونه به یوازې په موبایل کې خوندي شي.")
            .setView(input).setNegativeButton("لغوه", null)
            .setPositiveButton("جوړه کړه") { _, _ ->
                try {
                    val project = projectStore.create(input.text.toString())
                    val name = project.optJSONObject("project")?.optString("name") ?: "نوی فلم"
                    Toast.makeText(this, "پروژه «$name» په موبایل کې خوندي شوه.", Toast.LENGTH_LONG).show()
                } catch (_: Exception) {
                    Toast.makeText(this, "پروژه خوندي نه شوه؛ بیا هڅه وکړه.", Toast.LENGTH_LONG).show()
                }
            }.show()
    }

    private fun showProjects() {
        val projects = projectStore.list()
        if (projects.isEmpty()) {
            AlertDialog.Builder(this).setTitle("زما پروژې").setMessage("تر اوسه پروژه نشته. لومړی نوې پروژه جوړه کړه.")
                .setPositiveButton("سمه ده", null).show(); return
        }
        val names = projects.map { it.optJSONObject("project")?.optString("name", "بې نومه") ?: "بې نومه" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("زما پروژې").setItems(names) { _, which ->
            val project = projects[which].optJSONObject("project") ?: JSONObject()
            AlertDialog.Builder(this).setTitle(project.optString("name"))
                .setMessage("د پروژې فایل په موبایل کې خوندي دی. د صحنو او ایډیټ وسایل به په راتلونکو پړاوونو کې ورزیاتېږي.")
                .setPositiveButton("سمه ده", null).show()
        }.setNegativeButton("بند", null).show()
    }

    private fun show3DPreview() {
        // A dedicated Activity gives the OpenGL surface its own Android lifecycle.
        startActivity(Intent(this, Scene3DActivity::class.java))
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }
}
