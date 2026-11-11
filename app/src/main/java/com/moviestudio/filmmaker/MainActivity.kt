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
    private var renderedLinkedState: Boolean? = null
    private val pageHistory = java.util.ArrayDeque<() -> Unit>()
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
        renderedLinkedState = linked
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
            accountStatus.setPadding(dp(16), dp(16), dp(16), dp(16))
            accountStatus.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF202E49.toInt())
                setStroke(dp(1), 0xFF526B9A.toInt())
                cornerRadius = dp(16).toFloat()
            }
            page.addView(accountStatus, marginParams(top = 8, bottom = 14))

            addHomeButtonRow(page,
                "🎬 زما جوړ شوي فلمونه" to { showProjects() },
                "📽️ نوی فلم جوړول" to { createProjectDialog() }
            )
            addHomeButtonRow(page,
                "🧰 د فلم لپاره کارېدونکي شیان" to { showMovieAssets() }
            )
            addHomeButtonRow(page,
                "ℹ️ زموږ په اړه" to { showAboutUs() },
                "🔒 محرميت تګلاره" to { showPrivacyPolicy() }
            )
            addHomeButtonRow(page,
                "📜 شرایط او اصول" to { showTerms() },
                "📘 معلومات" to { showAppInformation() }
            )
            addHomeButtonRow(page,
                "👤 زما حساب" to { showLinkedAccount() },
                "⚙️ تنظیمات" to { showSettings() }
            )
        }

        page.addView(text("د فلم سټوډیو • لاسي درې‌بعدي فلم جوړول", 12, 0xFF8996B2.toInt()).apply {
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(26), 0, 0)
        })
        scroll.addView(page)
        setContentView(scroll)
    }

    private fun addHomeButtonRow(parent: LinearLayout, vararg items: Pair<String, () -> Unit>) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        items.forEachIndexed { index, item ->
            val button = Button(this).apply {
                text = item.first
                isAllCaps = false
                textSize = 15f
                setTextColor(Color.WHITE)
                gravity = android.view.Gravity.CENTER
                minHeight = dp(68)
                setPadding(dp(8), dp(10), dp(8), dp(10))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(if (index % 2 == 0) 0xFF344F83.toInt() else 0xFF405D91.toInt())
                    setStroke(dp(1), 0xFF718BB9.toInt())
                    cornerRadius = dp(15).toFloat()
                }
                setOnClickListener { item.second() }
            }
            val params = LinearLayout.LayoutParams(0, dp(68), 1f).apply {
                if (index > 0) marginStart = dp(8)
            }
            row.addView(button, params)
        }
        parent.addView(row, marginParams(top = 5, bottom = 5))
    }

    private fun showMovieAssets() {
        pageHistory.addLast { buildHome() }
        val categories = listOf(
            "🧍 کرکټرونه" to "د نارینه وو، ښځو، ماشومانو، پولیسو، سرتېرو، پاچاهانو او نورو انساني کرکټرونو ماډلونه.",
            "🐉 موجودات" to "ډریگن، حیوانات، خیالي موجودات او نور غیرانساني کرکټرونه.",
            "🏞️ چاپېریالونه" to "ځنګلونه، غرونه، دښتې، سیندونه، ښارونه او نورې طبیعي یا ښاري صحنې.",
            "🏠 ودانۍ" to "کورونه، ماڼۍ، دوکانونه، دفترونه، ښوونځي او نورې ودانۍ.",
            "🪑 فرنیچر" to "چوکۍ، مېزونه، تختونه، المارۍ او د کور دننه نور شیان.",
            "🚗 موټرونه" to "موټرونه، بسونه، لارۍ، موټرسایکلونه، الوتکې او نور وسایط.",
            "👕 جامې" to "کالي، یونیفورمونه، بوټان، خولۍ او د کرکټرونو نور لوازم.",
            "🗡️ وسلې" to "د فلم د صحنو لپاره تاریخي، خیالي او نورې نمایشي وسلې.",
            "🏃 حرکتونه" to "قدم وهل، منډه، ټوپ، کښېناستل، جګړه او نور د بدن حرکتونه.",
            "🎭 احساسات" to "خندا، غوسه، خپګان، وېره، حیرانتیا او د مخ نور حالتونه.",
            "🎥 کمرې" to "ثابته کمره، تعقیبي کمره، نږدې او لرې شاټونه او د کمرې حرکتونه.",
            "🔊 غږونه او موسیقي" to "د چاپېریال غږونه، د قدمونو غږ، اغېزیز غږونه، خبرې او موسیقي.",
            "📻 مخابرې او ټیلیفونونه" to "مخابرې، ګرځنده ټیلیفونونه، پخواني ټیلیفونونه او د اړیکو وسایل.",
            "🧰 د کار وسایل" to "هامر، پیچ‌کش، زینې، ساختماني او ترمیمي وسایل.",
            "🩺 طبي وسایل" to "د ډاکټر وسایل، د لومړنۍ مرستې بکس، روغتون او طبي تجهیزات.",
            "🍎 خوراک او څښاک" to "مېوې، خواړه، بوتلونه، پیالې او د پخلنځي د فلمي صحنو توکي.",
            "💻 د دفتر توکي" to "کمپیوټرونه، کتابونه، کاغذونه، قلمونه او د دفتر وسایل.",
            "⚽ سپورټ او لوبې" to "توپونه، د لوبو وسایل، ورزشي تجهیزات او د لوبو صحنې.",
            "🎒 د سفر وسایل" to "بکسونه، خېمې، د کمپ وسایل او د سفر نور توکي.",
            "🚦 د سړک توکي" to "د ترافیک نښې، څراغونه، د سړک خنډونه او د ښار د لارې وسایل.",
            "💡 برېښنايي توکي" to "څراغونه، تلویزیونونه، سپیکرونه او نور برېښنايي وسایل.",
            "📦 نور فلمي توکي" to "هغه نور ماډلونه او د صحنې شیان چې په نورو کټګوریو کې نه وي."
        )

        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(12, 18, 34)) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(18), dp(16), dp(24))
        }
        addAssetPageHeader(page, "🧰 د فلم توکي") { onBackPressed() }
        page.addView(text("د فلم لپاره د اړتیا وړ توکي په لاندې برخو کې وټاکه.", 14, 0xFFD7DCEF.toInt())
            .apply { setPadding(dp(4), dp(8), dp(4), dp(14)) })

        categories.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                }
                val first = makeAssetCategoryButton(item.first, index) {
                    showAssetCategory(item.first, item.second)
                }
                row.addView(first, LinearLayout.LayoutParams(0, dp(76), 1f))
                if (index + 1 < categories.size) {
                    val next = categories[index + 1]
                    val second = makeAssetCategoryButton(next.first, index + 1) {
                        showAssetCategory(next.first, next.second)
                    }
                    val params = LinearLayout.LayoutParams(0, dp(76), 1f).apply {
                        marginStart = dp(8)
                    }
                    row.addView(second, params)
                }
                page.addView(row, marginParams(top = 5, bottom = 5))
            }
        }

        scroll.addView(page)
        setContentView(scroll)
    }

    private fun addAssetPageHeader(parent: LinearLayout, title: String, onBack: () -> Unit) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val back = Button(this).apply {
            text = "⬅️ شاته"
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF475569.toInt())
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener { onBack() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(104), dp(48)))
        val heading = text(title, 20, Color.WHITE, true)
        header.addView(heading, LinearLayout.LayoutParams(0, dp(52), 1f))
        parent.addView(header, marginParams(bottom = 12))
    }

    private fun makeAssetCategoryButton(label: String, index: Int, action: () -> Unit): Button {
        val colors = intArrayOf(
            0xFF315B8A.toInt(), 0xFF67509A.toInt(), 0xFF28736C.toInt(),
            0xFF8A5635.toInt(), 0xFF3D659B.toInt(), 0xFF7A4D72.toInt(),
            0xFF456D45.toInt(), 0xFF8A4545.toInt()
        )
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            maxLines = 2
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(dp(6), dp(8), dp(6), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(colors[index % colors.size])
                setStroke(dp(1), 0xFF9AAED0.toInt())
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener { action() }
        }
    }

    private fun showAssetCategory(title: String, description: String) {
        pageHistory.addLast { showMovieAssets() }
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(12, 18, 34)) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(20), dp(18), dp(24))
        }
        addAssetPageHeader(page, title) { onBackPressed() }
        page.addView(text(description, 16, 0xFFD7DCEF.toInt())
            .apply { setPadding(dp(8), dp(16), dp(8), dp(16)) })
        val empty = text(
            "د دې برخې د توکو لست لا نه دی ورزیات شوی. په راتلونکي پړاو کې به دلته د توکو انځورونه، درې‌بعدي مخکتنه، معلومات او د فلم لپاره د انتخاب تڼۍ اضافه شي.",
            14, 0xFFB9C5DE.toInt()
        )
        empty.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xFF202E49.toInt())
            setStroke(dp(1), 0xFF526B9A.toInt())
            cornerRadius = dp(14).toFloat()
        }
        empty.setPadding(dp(16), dp(18), dp(16), dp(18))
        page.addView(empty, marginParams(top = 8))
        scroll.addView(page)
        setContentView(scroll)
    }

    private fun showAboutUs() {
        showInfo("ℹ️ زموږ په اړه",
            "Movie Studio د لاسي درې‌بعدي فلم جوړولو اپلیکیشن دی. کارن به پخپله کرکټرونه، صحنې، حرکتونه، کمره او غږونه تنظیموي او خپل فلم جوړوي. فلمونه د مصنوعي ځیرکتیا په وسیله نه جوړېږي.")
    }

    private fun showPrivacyPolicy() {
        showInfo("🔒 محرميت تګلاره",
            "۱. ستا جوړ شوي فلمونه، پروژې، کرکټرونه، ماډلونه، غږونه او صادرې شوې ویډیوګانې د موبایل په داخلي ذخیره کې ساتل کېږي.\n\n" +
            "۲. دا شخصي رسنۍ زموږ کلاوډ ته نه پورته کېږي.\n\n" +
            "۳. کلاوډ یوازې د حساب، د بیلانس، پېرودونو او د امکاناتو د اجازو لپاره اړین محدود معلومات ساتي.\n\n" +
            "۴. د حساب د نښلولو او کارولو پر مهال د انټرنېټ اړیکه اړینه ده.\n\n" +
            "۵. د اپلیکیشن د معلوماتو پاکول یا حذفول کېدای شي د موبایل ځینې محلي معلومات هم له منځه یوسي؛ د خپلو مهمو فایلونو بیک‌اپ وساته.")
    }

    private fun showTerms() {
        showInfo("📜 شرایط او اصول",
            "۱. اپلیکیشن د لاسي درې‌بعدي فلم جوړولو لپاره دی؛ د AI فلم جوړولو خدمت نه دی.\n\n" +
            "۲. کارن د خپلو وارد شوو ماډلونو، انځورونو او غږونو د قانوني کارولو مسؤلیت لري.\n\n" +
            "۳. د حساب، بیلانس او ځانګړو امکاناتو کارول د سرور د تایید او د هر پیک د څرګندو شرایطو تابع دي.\n\n" +
            "۴. د موبایل د خرابېدو، ورک کېدو یا د اپلیکیشن د حذفېدو پر مهال محلي پروژې له خطر سره مخ کېدای شي؛ کارن باید خپل مهم فایلونه بیک‌اپ کړي.\n\n" +
            "۵. د اپلیکیشن د نویو نسخو سره شرایط او امکانات بدلېدای شي. د پېرود یا تادیې ځانګړي شرایط به د فعالېدو پر وخت روښانه ښودل کېږي.")
    }

    private fun showAppInformation() {
        showInfo("📘 د اپلیکیشن معلومات",
            "نوم: Movie Studio\n\n" +
            "موخه: په موبایل کې د درې‌بعدي فلمونو لاسي جوړول.\n\n" +
            "د پروژو او فلمونو ځای: د موبایل داخلي ذخیره.\n\n" +
            "د حساب خدمت: د انټرنېټ له لارې.\n\n" +
            "د جوړولو اوسنی پړاو: د کور پاڼه او د پروژو بنسټیز مدیریت. د 3D کرکټرونو، حرکتونو، صحنو، مهال‌وېش او ویډیو صادرولو برخې به په جلا پړاوونو کې جوړېږي.")
    }

    private fun showSettings() {
        showInfo("⚙️ تنظیمات",
            "د اپلیکیشن د ژبې، کیفیت، ذخیرې، غږ او نورو تنظیماتو برخې به په راتلونکو پړاوونو کې ورزیاتې شي.")
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
            val amount = java.math.BigDecimal.valueOf(balance.optDouble("usdt")).stripTrailingZeros().toPlainString()
            val content = android.text.SpannableStringBuilder()
            content.append("💼 زما د اپک حساب: نښلول شوی ✅\n\n")
            content.append("⭐ د اپک امتیازات: ").append(balance.optLong("points").toString())
            content.append("\n💵 د اپک USDT بیلانس: ").append(amount)
            content.append("\n\n👥 زما دعوتونه: ").append(result.optInt("referral_count").toString())
            content.append("\n✅ تایید شوي دعوتونه: ").append(result.optInt("verified_referral_count").toString())
            val view = TextView(this).apply {
                text = content
                textSize = 16f
                setTextColor(0xFFF1F5FF.toInt())
                setPadding(dp(22), dp(18), dp(22), dp(18))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFF202E49.toInt())
                    setStroke(dp(1), 0xFF526B9A.toInt())
                    cornerRadius = dp(16).toFloat()
                }
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

    @Deprecated("Deprecated in Android API; retained for Android 12 device compatibility")
    override fun onBackPressed() {
        if (!pageHistory.isEmpty()) {
            val previousPage = pageHistory.removeLast()
            previousPage.invoke()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        val linkedNow = !prefs.getString("session_token", null).isNullOrBlank()
        if (renderedLinkedState != linkedNow) buildHome()
        if (linkedNow && ::accountStatus.isInitialized) refreshAccount()
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
        val invitedCount = result.optInt("referral_count", 0)
        val verifiedCount = result.optInt("verified_referral_count", invitedCount)
        val amount = java.math.BigDecimal.valueOf(balance.optDouble("usdt")).stripTrailingZeros().toPlainString()
        val accountText = android.text.SpannableStringBuilder()
        accountText.append("💼 زما د اپک حساب\n")
        accountText.append("حالت: $state\n\n")
        accountText.append("⭐ د اپک امتیازات: ${balance.optLong("points")}\n")
        accountText.append("💵 د اپک USDT بیلانس: $amount\n\n")
        accountText.append("👥 زما دعوتونه: $invitedCount\n")
        accountText.append("✅ تایید شوي دعوتونه: $verifiedCount\n\n")
        accountText.append(if (offline) "دا د اپک وروستي خوندي شوي بیلانسونه دي؛ د تازه کولو لپاره انټرنېټ ته اړتیا ده." else "د اپک بیلانسونه تازه شول.")
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
            AlertDialog.Builder(this).setTitle("زما پروژې")
                .setMessage("تر اوسه پروژه نشته. لومړی نوې پروژه جوړه کړه.")
                .setPositiveButton("سمه ده", null).show()
            return
        }
        val names = projects.map { item ->
            val details = item.optJSONObject("project") ?: JSONObject()
            val title = details.optString("name", "بې نومه")
            val updated = details.optString("updatedAt", "")
            "$title  •  ${updated.take(10)}"
        }.toTypedArray()
        AlertDialog.Builder(this).setTitle("زما فلمونه او پروژې")
            .setItems(names) { _, which -> showProjectActions(projects[which]) }
            .setPositiveButton("＋ نوې پروژه") { _, _ -> createProjectDialog() }
            .setNegativeButton("بند", null).show()
    }

    private fun showProjectActions(projectData: JSONObject) {
        val details = projectData.optJSONObject("project") ?: JSONObject()
        val id = details.optString("id")
        val name = details.optString("name", "بې نومه")
        val created = details.optString("createdAt", "نامعلوم")
        val updated = details.optString("updatedAt", "نامعلوم")
        val options = arrayOf("📂 د پروژې معلومات", "✏️ نوم بدلول", "🗑️ پروژه حذفول")
        AlertDialog.Builder(this).setTitle(name).setItems(options) { _, which ->
            when (which) {
                0 -> AlertDialog.Builder(this).setTitle("د پروژې معلومات")
                    .setMessage("نوم: $name\n\nجوړه شوې: $created\n\nوروستی بدلون: $updated\n\nپروژه او اړوند فایلونه یوازې په همدې موبایل کې ساتل کېږي. د 3D صحنې ایډیټر لا د جوړولو په پړاو کې دی.")
                    .setPositiveButton("سمه ده", null).show()
                1 -> renameProjectDialog(id, name)
                2 -> confirmDeleteProject(id, name)
            }
        }.setNegativeButton("بند", null).show()
    }

    private fun renameProjectDialog(projectId: String, oldName: String) {
        val input = EditText(this).apply {
            setSingleLine(true)
            setText(oldName)
            setSelection(text.length)
        }
        AlertDialog.Builder(this).setTitle("د پروژې نوم بدلول")
            .setView(input).setNegativeButton("لغوه", null)
            .setPositiveButton("خوندي کول") { _, _ ->
                try {
                    projectStore.rename(projectId, input.text.toString())
                    Toast.makeText(this, "د پروژې نوم بدل او خوندي شو.", Toast.LENGTH_SHORT).show()
                    showProjects()
                } catch (error: Exception) {
                    Toast.makeText(this, error.message ?: "نوم بدل نه شو.", Toast.LENGTH_LONG).show()
                }
            }.show()
    }

    private fun confirmDeleteProject(projectId: String, name: String) {
        AlertDialog.Builder(this).setTitle("پروژه حذفول؟")
            .setMessage("ایا «$name» او د هغې ټول محلي فایلونه حذف کړې؟ دا کار بېرته نه راګرځي.")
            .setNegativeButton("نه", null)
            .setPositiveButton("هو، حذف یې کړه") { _, _ ->
                try {
                    projectStore.delete(projectId)
                    Toast.makeText(this, "پروژه حذف شوه.", Toast.LENGTH_SHORT).show()
                    showProjects()
                } catch (error: Exception) {
                    Toast.makeText(this, error.message ?: "پروژه حذف نه شوه.", Toast.LENGTH_LONG).show()
                }
            }.show()
    }
    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }
}
