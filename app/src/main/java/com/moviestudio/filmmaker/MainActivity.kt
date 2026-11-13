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
            else "پروژې او فلمونه په موبایل کې ساتل کېږي؛ د حساب او امتیازاتو لپاره ټېلېګرام بوټ ونښلوه.",
            14, 0xFFD7DCEF.toInt()
        ).apply { setPadding(dp(8), dp(10), dp(8), dp(18)) })

        if (linked) {
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
        }

        // محلي فلمي وسایل د حساب له نښلولو پرته هم کار کوي.
        addHomeButtonRow(page,
            "🎬 زما جوړ شوي فلمونه" to { showProjects() },
            "📽️ نوی فلم جوړول" to { createProjectDialog() }
        )
        addHomeButtonRow(page,
            "🧰 د فلم لپاره کارېدونکي شیان" to { showMovieAssets() }
        )

        if (!linked) {
            addButton(page, "♻️  ټېلېګرام بوټ ونښلوه") { showAccountDialog() }
            addButton(page, "📖  د کارولو لارښود") { showInfo(
                "د کارولو لارښود",
                "۱. د فلم نوې پروژه جوړه کړه؛ پروژه او فایلونه په موبایل کې ساتل کېږي.\n\n۳. د حساب، بیلانس او ځانګړو حسابي خدمتونو لپاره له @AI_MovieMaker_Bot سره /start وکړه، بیا دلته بوټ ونښلوه.\n\n۴. د فلم جوړولو محلي برخې له انټرنېټ پرته هم کار کوي."
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
        renderMovieAssets()
    }

    private fun renderMovieAssets() {
        val categories = listOf(
            "🧍 کرکټرونه" to "د انسانانو بېلابېل کرکټرونه.",
            "🐉 موجودات" to "کورني او ځنګلي حیوانات، مرغان او خیالي موجودات.",
            "🏞️ چاپېریالونه" to "ځنګلونه، غرونه، دښتې، اوبه، کلي او ښارونه.",
            "🏠 ودانۍ" to "کورونه، ماڼۍ، عامه ودانۍ او د ځمکې بېلابېلې سطحې.",
            "🪑 فرنیچر" to "چوکۍ، مېزونه، تختونه، المارۍ او د کور دننه نور شیان.",
            "🚗 موټرونه" to "موټرونه، بسونه، لارۍ، موټرسایکلونه، الوتکې او نور وسایط.",
            "👕 جامې" to "کالي، یونیفورمونه، بوټان، خولۍ او د کرکټرونو نور لوازم.",
            "🗡️ وسلې" to "د فلم د صحنو لپاره تاریخي، خیالي او نورې نمایشي وسلې.",
            "🏃 حرکتونه" to "قدم وهل، منډه، ټوپ، کښېناستل او نور د بدن حرکتونه.",
            "🎭 احساسات" to "خندا، غوسه، خپګان، وېره او د مخ نور حالتونه.",
            "🎥 کمرې" to "ثابته کمره، تعقیبي کمره، نږدې او لرې شاټونه.",
            "🔊 غږونه او موسیقي" to "د چاپېریال غږونه، اغېزیز غږونه، خبرې او موسیقي.",
            "📻 مخابرې او ټیلیفونونه" to "مخابرې او د اړیکو وسایل.",
            "🧰 د کار وسایل" to "هامر، پیچ‌کش، زینې او ساختماني وسایل.",
            "🩺 طبي وسایل" to "د ډاکټر وسایل او طبي تجهیزات.",
            "🍎 خوراک او څښاک" to "مېوې، خواړه، بوتلونه او د پخلنځي توکي.",
            "💻 د دفتر توکي" to "کمپیوټرونه، کتابونه، کاغذونه او قلمونه.",
            "⚽ سپورټ او لوبې" to "توپونه او ورزشي تجهیزات.",
            "🎒 د سفر وسایل" to "بکسونه، خېمې او د سفر نور توکي.",
            "🚦 د سړک توکي" to "د ترافیک نښې او د سړک وسایل.",
            "💡 برېښنايي توکي" to "څراغونه، تلویزیونونه او نور برېښنايي وسایل.",
            "📦 نور فلمي توکي" to "نور ماډلونه او د صحنې شیان."
        )
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(12, 18, 34)) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(18), dp(16), dp(24))
        }
        addAssetPageHeader(page, "🧰 د فلم توکي") { onBackPressed() }
        page.addView(text(
            "د فلم لپاره توکي په لاندې برخو کې وټاکه. ټولې ۲۲ برخې د هرې برخې یوازې ۵ جلا فلمي توکي لري.",
            14, 0xFFD7DCEF.toInt()
        ).apply { setPadding(dp(4), dp(8), dp(4), dp(14)) })
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
        pageHistory.addLast { renderMovieAssets() }
        val categoryId = MovieAssetLibrary.categoryIdForTitle(title)
        val entries = categoryId?.let { MovieAssetLibrary.forCategory(it) }.orEmpty()
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(12, 18, 34)) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), dp(16), dp(12), dp(24))
        }
        addAssetPageHeader(page, title) { onBackPressed() }
        if (entries.isNotEmpty()) {
            val isWideCategory = categoryId == MovieAssetLibrary.ENVIRONMENTS ||
                categoryId == MovieAssetLibrary.BUILDINGS
            val imageHeight = if (isWideCategory) dp(104) else dp(142)
            val tileHeight = if (isWideCategory) dp(136) else dp(174)
            page.addView(text(
                "د توکي د لوی لیدلو او بیې لپاره پر انځور ټچ وکړه. په هره برخه کې ۵ توکي دي؛ اوس ټول توکي وړیا دي.",
                14, 0xFFD7DCEF.toInt()
            ).apply { setPadding(dp(6), dp(8), dp(6), dp(14)) })

            entries.indices.chunked(2).forEach { pair ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                }
                pair.forEach { index ->
                    val image = AssetIllustrationView(this, categoryId!!, index, entries[index].name).apply {
                        contentDescription = "د وړیا فلمي توکي انځور"
                        setOnClickListener { showAssetPreview(categoryId!!, index) }
                    }
                    val tile = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(5), dp(5), dp(5), dp(5))
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(0xFF202E49.toInt())
                            setStroke(dp(1), 0xFF526B9A.toInt())
                            cornerRadius = dp(13).toFloat()
                        }
                        setOnClickListener { showAssetPreview(categoryId!!, index) }
                    }
                    tile.addView(image, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, imageHeight
                    ))
                    val free = text("وړیا • ۰", 13, 0xFF8FE0B1.toInt(), true).apply {
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, dp(6), 0, dp(4))
                    }
                    tile.addView(free)
                    val params = LinearLayout.LayoutParams(0, tileHeight, 1f).apply {
                        if (row.childCount > 0) marginStart = dp(8)
                    }
                    row.addView(tile, params)
                }
                if (pair.size == 1) {
                    row.addView(View(this), LinearLayout.LayoutParams(0, tileHeight, 1f))
                }
                page.addView(row, marginParams(top = 4, bottom = 4))
            }
        } else {
            page.addView(text(
                "د دې برخې انځوریز توکي به په راتلونکو پړاوونو کې ورزیات شي.",
                14, 0xFFB9C5DE.toInt()
            ).apply { setPadding(dp(10), dp(18), dp(10), dp(18)) })
        }
        scroll.addView(page)
        setContentView(scroll)
    }

    private fun showAssetPreview(categoryId: String, index: Int) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(14), dp(18), dp(8))
            setBackgroundColor(0xFF17243A.toInt())
        }
        val previewName = MovieAssetLibrary.forCategory(categoryId).getOrNull(index)?.name.orEmpty()
        val preview = AssetIllustrationView(this, categoryId, index, previewName)
        val isWidePreview = categoryId == MovieAssetLibrary.ENVIRONMENTS ||
            categoryId == MovieAssetLibrary.BUILDINGS
        content.addView(preview, LinearLayout.LayoutParams(
            if (isWidePreview) dp(320) else dp(280),
            if (isWidePreview) dp(190) else dp(260)
        ))
        content.addView(text("وړیا", 20, 0xFF8FE0B1.toInt(), true).apply {
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(12), 0, dp(3))
        })
        content.addView(text("بیه: ۰", 16, Color.WHITE, true).apply {
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(2), 0, dp(8))
        })
        AlertDialog.Builder(this)
            .setView(content)
            .setPositiveButton("بندول", null)
            .show()
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
