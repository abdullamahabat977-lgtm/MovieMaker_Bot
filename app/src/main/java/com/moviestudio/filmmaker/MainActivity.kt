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
        window.statusBarColor = Color.rgb(12, 18, 34)
        window.navigationBarColor = Color.rgb(12, 18, 34)
        buildHome()
        refreshAccount()
    }

    private fun buildHome() {
        val linked = !prefs.getString("session_token", null).isNullOrBlank()
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(12, 18, 34)) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(28), dp(20), dp(30))
        }

        page.addView(text("🎬 MOVIE STUDIO", 25, Color.WHITE, true))
        page.addView(text("د خپل فلم جوړولو سټوډیو", 19, 0xFFBFCBFF.toInt(), true))
        page.addView(text(
            if (linked) "ښه راغلاست! ستا حساب په دې موبایل کې خوندي دی."
            else "خپل فلم په خپل لاس جوړ کړه؛ د پیل لپاره خپل ټېلېګرام بوټ ونښلوه.",
            14, 0xFFD7DCEF.toInt()
        ).apply { setPadding(dp(8), dp(10), dp(8), dp(18)) })

        if (!linked) {
            // Before account linking, show exactly the four approved entry buttons.
            addButton(page, "♻️  ټېلېګرام بوټ ونښلوه") { showAccountDialog() }
            addButton(page, "📖  د کارولو لارښود") { showInfo(
                "د کارولو لارښود",
                "۱. لومړی له @AI_MovieMaker_Bot سره په ټېلېګرام کې /start وکړه.\n\n۲. دلته د ټېلېګرام بوټ ونښلوه وټاکه او خپل عددي Telegram ID ولیکه.\n\n۳. د کوډ غوښتنه وکړه؛ شپږ عددي کوډ به په ټېلېګرام کې درولېږل شي.\n\n۴. کوډ تایید کړه. له بریالي تایید وروسته به وړیا امکانات فعال او د ځانګړو امکاناتو اجازه به ستا د حساب له شرایطو سره سم وکتل شي.\n\n۵. پروژې او فلمونه ستا په موبایل کې ساتل کېږي."
            ) }
            addButton(page, "🔒  محرمیت او معلومات") { showInfo(
                "محرمیت او معلومات",
                "ستاسې فلمونه، پروژې، کرکټرونه، ماډلونه او صادر شوي ویډیوګانې یوازې د موبایل په داخلي ذخیره کې ساتل کېږي او زموږ کلاوډ ته نه پورته کېږي.\n\nکلاوډ یوازې د حساب، د Points او USDT بیلانسونو، پلورنځي، پېرودونو او د امکاناتو د اجازو اړوند محدود معلومات ساتي. د حساب د تایید لپاره د ټېلېګرام له لارې یو ځل کارېدونکی کوډ کارول کېږي."
            ) }
            addButton(page, "💚  زموږ په اړه") { showInfo(
                "زموږ په اړه",
                "Movie Studio د لاسي 3D فلم جوړولو اپلیکیشن دی. موخه یې دا ده چې کارن خپل کرکټرونه، چاپېریالونه، حرکتونه، کمره، غږ او فلمونه پخپله جوړ او تنظیم کړي. دا اپلیکیشن د AI په وسیله فلم نه جوړوي."
            ) }
        } else {
            accountStatus = text("حساب په دې موبایل کې خوندي دی. د بیلانس او اجازو تازه معلومات د انټرنېټ په وصلېدو ترلاسه کېږي.", 13, 0xFFB9C5DE.toInt())
            val cached = prefs.getString("cached_profile", null)
            if (cached != null) {
                try { updateAccountText(JSONObject(cached), false) } catch (_: Exception) { }
            }
            accountStatus.setPadding(dp(12), dp(14), dp(12), dp(14))
            page.addView(accountStatus, marginParams(top = 8, bottom = 10))
            addButton(page, "👤  زما حساب او بیلانس") { showAccountDialog() }
            addButton(page, "⭐  وړیا امکانات") { showInfo("وړیا امکانات", "د حساب له تایید وروسته وړیا امکانات په اتومات ډول فعالېږي. د دغو امکاناتو لپاره Points نه لګېږي.") }
            addButton(page, "💎  ځانګړي امکانات") { showInfo("ځانګړي امکانات", "ځانګړي امکانات د هر پیک د شرایطو، د سرور له اجازې او د اړوند بیلانس له مخې فعالېږي. Points او USDT جلا حسابېږي.") }
            addButton(page, "🛒  د امکاناتو پلورنځی") { showInfo("د امکاناتو پلورنځی", "د پلورنځي او پېرودلو خوندي سیستم به د پیکونو او تادیاتو له بشپړېدو وروسته فعال شي.") }
            addButton(page, "＋  نوې پروژه جوړه کړه") { createProjectDialog() }
            addButton(page, "📁  زما فلمونه او پروژې") { showProjects() }
            addButton(page, "🧊  3D مخکتنه") { show3DPreview() }
            addButton(page, "⚙️  تنظیمات") { showInfo("تنظیمات", "د اپلیکیشن نور تنظیمات به په راتلونکو پړاوونو کې ورزیاتېږي.") }
        }

        page.addView(text("Movie Studio • لاسي 3D فلم جوړول", 12, 0xFF8996B2.toInt()).apply {
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(26), 0, 0)
        })
        scroll.addView(page)
        setContentView(scroll)
    }

    private fun showInfo(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setPositiveButton("سمه ده", null).show()
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
                            mainHandler.post { 
                                prefs.edit().putString("cached_profile", profile.toString()).apply()
                                dialog.dismiss()
                                buildHome()
                            }
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
        val token = prefs.getString("session_token", null) ?: return
        worker.execute {
            try {
                val result = ApiClient.me(token)
                mainHandler.post {
                    if (result.optBoolean("ok")) {
                        prefs.edit().putString("cached_profile", result.toString()).apply()
                        updateAccountText(result)
                        buildHome()
                    } else {
                        prefs.edit().remove("session_token").remove("cached_profile").apply()
                        buildHome()
                    }
                }
            } catch (_: Exception) {
                mainHandler.post {
                    val cached = prefs.getString("cached_profile", null)
                    if (cached != null) {
                        try { updateAccountText(JSONObject(cached), true) }
                        catch (_: Exception) { accountStatus.text = "آفلاین حالت: حساب په موبایل کې خوندي دی." }
                    } else accountStatus.text = "آفلاین حالت: حساب په موبایل کې خوندي دی؛ د بیلانس معلومات لا نه دي زېرمه شوي."
                }
            }
        }
    }

    private fun updateAccountText(result: JSONObject, offline: Boolean = false) {
        val user = result.optJSONObject("user") ?: JSONObject()
        val balance = result.optJSONObject("balance") ?: JSONObject()
        val state = if (offline) "نښلول شوی ✅ • آفلاین" else "نښلول شوی ✅ • تازه"
        accountStatus.text = "حساب: $state\nTelegram ID: ${user.optString("telegram_id")}\nPoints: ${balance.optLong("points")}  |  USDT: ${balance.optDouble("usdt")}\n" +
            if (offline) "دا وروستي خوندي شوي معلومات دي؛ د بیلانس تازه کولو لپاره انټرنېټ ته اړتیا ده." else "معلومات تازه شول."
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
