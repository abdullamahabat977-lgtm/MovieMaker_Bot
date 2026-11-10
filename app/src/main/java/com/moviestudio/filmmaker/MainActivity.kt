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

        page.addView(text("🎬 د فلم سټوډیو", 25, Color.WHITE, true))
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
                "۱. لومړی له @AI_MovieMaker_Bot سره په ټېلېګرام کې /start وکړه.\n\n۲. دلته د ټېلېګرام بوټ ونښلوه وټاکه او خپله عددي پېژندشمېره ولیکه.\n\n۳. د کوډ غوښتنه وکړه؛ شپږ عددي کوډ به په ټېلېګرام کې درولېږل شي.\n\n۴. کوډ تایید کړه. له بریالي تایید وروسته به وړیا امکانات فعال او د ځانګړو امکاناتو اجازه به ستا د حساب له شرایطو سره سم وکتل شي.\n\n۵. پروژې او فلمونه ستا په موبایل کې ساتل کېږي."
            ) }
            addButton(page, "🔒  محرمیت او معلومات") { showInfo(
                "محرمیت او معلومات",
                "ستاسې فلمونه، پروژې، کرکټرونه، ماډلونه او صادر شوي ویډیوګانې یوازې د موبایل په داخلي ذخیره کې ساتل کېږي او زموږ کلاوډ ته نه پورته کېږي.\n\nکلاوډ یوازې د حساب، د امتيازاتو او USDT 💲 بیلانسونو، پلورنځي، پېرودونو او د امکاناتو د اجازو اړوند محدود معلومات ساتي. د حساب د تایید لپاره د ټېلېګرام له لارې یو ځل کارېدونکی کوډ کارول کېږي."
            ) }
            addButton(page, "💚  زموږ په اړه") { showInfo(
                "زموږ په اړه",
                "د فلم سټوډیو د لاسي درې‌بعدي فلم جوړولو اپلیکیشن دی. موخه یې دا ده چې کارن خپل کرکټرونه، چاپېریالونه، حرکتونه، کمره، غږ او فلمونه پخپله جوړ او تنظیم کړي. دا اپلیکیشن د AI په وسیله فلم نه جوړوي."
            ) }
        } else {
            accountStatus = text("حساب په دې موبایل کې خوندي دی. د بیلانس او اجازو تازه معلومات د انټرنېټ په وصلېدو ترلاسه کېږي.", 13, 0xFFB9C5DE.toInt())
            val cached = prefs.getString("cached_profile", null)
            if (cached != null) {
                try { updateAccountText(JSONObject(cached), false) } catch (_: Exception) { }
            }
            accountStatus.setPadding(dp(12), dp(14), dp(12), dp(14))
            page.addView(accountStatus, marginParams(top = 8, bottom = 10))
            addButton(page, "👤  زما حساب") { showLinkedAccount() }
            addButton(page, "⭐  وړیا امکانات") { showInfo("وړیا امکانات", "د حساب له تایید وروسته وړیا امکانات په اتومات ډول فعالېږي. د دغو امکاناتو لپاره امتيازات نه لګېږي.") }
            addButton(page, "💎  ځانګړي امکانات") { showInfo("ځانګړي امکانات", "ځانګړي امکانات د هر پیک د شرایطو، د سرور له اجازې او د اړوند بیلانس له مخې فعالېږي. امتيازات او USDT 💲 جلا حسابېږي.") }
            addButton(page, "🛒  د امکاناتو پلورنځی") { showInfo("د امکاناتو پلورنځی", "د پلورنځي او پېرودلو خوندي سیستم به د پیکونو او تادیاتو له بشپړېدو وروسته فعال شي.") }
            addButton(page, "＋  نوې پروژه جوړه کړه") { createProjectDialog() }
            addButton(page, "📁  زما فلمونه او پروژې") { showProjects() }
            addButton(page, "🧊  درې‌بعدي مخکتنه") { show3DPreview() }
            addButton(page, "⚙️  تنظیمات") { showInfo("تنظیمات", "د اپلیکیشن نور تنظیمات به په راتلونکو پړاوونو کې ورزیاتېږي.") }
        }

        page.addView(text("د فلم سټوډیو • لاسي درې‌بعدي فلم جوړول", 12, 0xFF8996B2.toInt()).apply {
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

    private fun showLinkedAccount() {
        val raw = prefs.getString("cached_profile", null)
        if (raw.isNullOrBlank()) {
            showInfo("زما حساب", "د حساب معلومات لا نه دي زېرمه شوي. انټرنېټ ته وصل شه او څو شېبې وروسته بیا هڅه وکړه.")
            refreshAccount()
            return
        }
        try {
            val result = JSONObject(raw)
            val user = result.optJSONObject("user") ?: JSONObject()
            val balance = result.optJSONObject("balance") ?: JSONObject()
            val id = user.optString("telegram_id")
            val amount = java.math.BigDecimal.valueOf(balance.optDouble("usdt")).stripTrailingZeros().toPlainString()
            val content = android.text.SpannableStringBuilder()
            content.append("حساب: نښلول شوی ✅\\n")
            content.append("نوم: ").append(user.optString("display_name").ifBlank { "—" }).append("\\n")
            content.append("د ټېلېګرام پېژندشمېره: ")
            val start = content.length
            content.append(id)
            val end = content.length
            content.append("\\n\\n⭐ امتيازات: ").append(balance.optLong("points").toString())
            content.append("\\nUSDT 💲: ").append(amount)
            content.append("\\n\\n👥 ټول بلل شوي ملګري: ").append(result.optInt("referral_count").toString())
            content.append("\\nتایید شوي: ").append(result.optInt("verified_referral_count").toString())
            val view = TextView(this).apply {
                text = content
                textSize = 16f
                setTextColor(Color.WHITE)
                setPadding(dp(22), dp(14), dp(22), dp(14))
            }
            if (id.isNotBlank() && end > start) {
                content.setSpan(object : android.text.style.ClickableSpan() {
                    override fun onClick(widget: View) {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("د ټېلېګرام پېژندشمېره", id))
                        Toast.makeText(this@MainActivity, "د ټېلېګرام پېژندشمېره کاپي شوه.", Toast.LENGTH_SHORT).show()
                    }
                    override fun updateDrawState(ds: android.text.TextPaint) {
                        ds.color = 0xFF62C7FF.toInt()
                        ds.isUnderlineText = false
                        ds.isFakeBoldText = true
                    }
                }, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            view.text = content
            view.movementMethod = android.text.method.LinkMovementMethod.getInstance()
            view.highlightColor = Color.TRANSPARENT
            AlertDialog.Builder(this).setTitle("👤 زما حساب").setView(view)
                .setPositiveButton("سمه ده", null).show()
        } catch (_: Exception) {
            showInfo("زما حساب", "د حساب معلومات سم نه لوستل کېږي؛ انټرنېټ ته وصل شه او بیا هڅه وکړه.")
        }
    }

    private fun showAccountDialog() {
        startActivity(Intent(this, AccountLinkActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        if (::accountStatus.isInitialized) refreshAccount()
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
                    } else {
                        val cached = prefs.getString("cached_profile", null)
                        if (cached != null) {
                            try { updateAccountText(JSONObject(cached), true) } catch (_: Exception) { }
                        }
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
        val telegramId = user.optString("telegram_id")
        val invitedCount = result.optInt("referral_count", 0)
        val verifiedCount = result.optInt("verified_referral_count", invitedCount)
        val amount = java.math.BigDecimal.valueOf(balance.optDouble("usdt")).stripTrailingZeros().toPlainString()
        val accountText = android.text.SpannableStringBuilder()
        accountText.append("حساب: $state\nد ټېلېګرام پېژندشمېره: ")
        val idStart = accountText.length
        accountText.append(telegramId)
        val idEnd = accountText.length
        accountText.append("\nامتيازات: ${balance.optLong("points")}  |  USDT 💲: $amount")
        accountText.append("\nټول بلل شوي ملګري: $invitedCount  |  تایید شوي: $verifiedCount\n")
        accountText.append(if (offline) "دا وروستي خوندي شوي معلومات دي؛ د بیلانس تازه کولو لپاره انټرنېټ ته اړتیا ده." else "معلومات تازه شول.")
        if (telegramId.isNotBlank() && idEnd > idStart) {
            accountText.setSpan(object : android.text.style.ClickableSpan() {
                override fun onClick(widget: View) {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("د ټېلېګرام پېژندشمېره", telegramId))
                    Toast.makeText(this@MainActivity, "د ټېلېګرام پېژندشمېره کاپي شوه.", Toast.LENGTH_SHORT).show()
                }
                override fun updateDrawState(ds: android.text.TextPaint) {
                    ds.color = 0xFF62C7FF.toInt()
                    ds.isUnderlineText = false
                    ds.isFakeBoldText = true
                }
            }, idStart, idEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        accountStatus.text = accountText
        accountStatus.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        accountStatus.highlightColor = Color.TRANSPARENT
        accountStatus.linksClickable = true
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
