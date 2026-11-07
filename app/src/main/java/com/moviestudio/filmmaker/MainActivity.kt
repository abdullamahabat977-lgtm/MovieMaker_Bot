package com.moviestudio.filmmaker

import android.os.Bundle
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(
        colors: IntArray,
        radius: Int,
        vertical: Boolean = false
    ): GradientDrawable {
        return GradientDrawable(
            if (vertical) GradientDrawable.Orientation.TOP_BOTTOM
            else GradientDrawable.Orientation.LEFT_RIGHT,
            colors
        ).apply { cornerRadius = dp(radius).toFloat() }
    }

    private fun darken(color: Int): Int = Color.rgb(
        (Color.red(color) * 0.42f).toInt(),
        (Color.green(color) * 0.42f).toInt(),
        (Color.blue(color) * 0.42f).toInt()
    )

    private fun showGuide() {
        AlertDialog.Builder(this)
            .setTitle("د فلم جوړولو لارښوونې")
            .setMessage("۱. لومړی نوې 3D پروژه جوړه کړه.\n\n۲. خپل 3D کرکټرونه او ماډلونه ورزیات کړه.\n\n۳. د فلم لپاره 3D چاپېریال او صحنې برابرې کړه.\n\n۴. کرکټرونو ته حرکتونه او انیمېشن ورکړه.\n\n۵. کمره، رڼا، رنګونه، غږ او ډیالوګ تنظیم کړه.\n\n۶. په مهال‌وېش کې صحنې سره ونښلوه، مخکتنه یې وکړه او وروسته فلم صادر کړه.\n\nیادونه: دا د لاسي فلم جوړولو سټوډیو ده؛ فلم د کاروونکي په لاس جوړېږي، نه د AI ویډیو جوړولو له لارې.")
            .setPositiveButton("سمه ده", null)
            .show()
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("زموږ په اړه")
            .setMessage("فلم سټوډیو د دې لپاره جوړېږي چې کاروونکي وکولای شي خپل 3D کرکټرونه، چاپېریالونه، حرکتونه، کمرې او غږونه پخپله تنظیم او خپل فلم جوړ کړي.\n\nموخه: لاسي 3D فلم جوړونه، د پروژو خوندي ساتل او د فلم ویډیو صادرول.\n\nدا پروژه لا د جوړېدو په حال کې ده.")
            .setPositiveButton("بند", null)
            .show()
    }

    private fun label(
        text: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun toolCard(
        icon: String,
        title: String,
        description: String,
        accent: Int,
        message: String
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(10), dp(16), dp(10), dp(14))
            background = rounded(
                intArrayOf(darken(accent), accent),
                18,
                true
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                when (message) {
                    "لارښوونې" -> showGuide()
                    "زموږ په اړه" -> showAbout()
                    else -> Toast.makeText(
                        this@MainActivity,
                        "$message برخه به د 3D سټوډیو په راتلونکو مرحلو کې فعاله شي",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val iconView = label(icon, 30f, Color.WHITE, true)
        iconView.background = rounded(
            intArrayOf(Color.argb(95, 15, 20, 38), Color.argb(65, 15, 20, 38)),
            16
        )
        iconView.setPadding(dp(13), dp(8), dp(13), dp(8))
        card.addView(iconView)

        val titleView = label(title, 15f, Color.WHITE, true)
        val titleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        titleParams.topMargin = dp(12)
        card.addView(titleView, titleParams)

        val descView = label(description, 11f, Color.rgb(174, 184, 205))
        val descParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        descParams.topMargin = dp(5)
        card.addView(descView, descParams)

        return card
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(13, 17, 31)
        window.navigationBarColor = Color.rgb(13, 17, 31)

        val scroll = ScrollView(this).apply {
            // د res/drawable/poster_background.jpg عکس د اپ شالید دی
            setBackgroundResource(R.drawable.poster_background)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            isFillViewport = true
        }

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(18), dp(18), dp(26))
            // د JPG پوسټر پر سر تیاره شفاف پوښ، څو لیکنې لوستل اسانه وي
            setBackgroundColor(Color.argb(105, 7, 11, 25))
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val brandTitle = label("🎬  فلم سټوډیو", 21f, Color.WHITE, true)
        brandTitle.gravity = Gravity.RIGHT
        brand.addView(brandTitle)

        val brandSub = label("د خپل فلم جوړولو ځای", 12f, Color.rgb(220, 228, 245))
        brandSub.gravity = Gravity.RIGHT
        val subParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        subParams.topMargin = dp(3)
        brand.addView(brandSub, subParams)

        topBar.addView(
            brand,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        val offline = label("● آفلاین", 12f, Color.rgb(112, 232, 177), true)
        offline.background = rounded(
            intArrayOf(Color.rgb(28, 62, 57), Color.rgb(24, 51, 52)),
            30
        )
        offline.setPadding(dp(13), dp(8), dp(13), dp(8))
        topBar.addView(offline)
        main.addView(topBar)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(23), dp(20), dp(22))
            background = rounded(
                intArrayOf(Color.rgb(91, 61, 190), Color.rgb(26, 112, 151)),
                24
            )
        }

        val heroIcon = label("🎥", 48f, Color.WHITE)
        hero.addView(heroIcon)

        val heroTitle = label("خپل 3D فلم، په خپل لاس جوړ کړه", 22f, Color.WHITE, true)
        val heroTitleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        heroTitleParams.topMargin = dp(8)
        hero.addView(heroTitle, heroTitleParams)

        val heroText = label(
            "3D کرکټرونه، چاپېریالونه او حرکتونه جوړ کړه؛ خپله کیسه په فلم بدله کړه.",
            13f,
            Color.rgb(239, 243, 255)
        )
        val heroTextParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        heroTextParams.topMargin = dp(8)
        hero.addView(heroText, heroTextParams)

        val heroParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        heroParams.topMargin = dp(22)
        main.addView(hero, heroParams)

        val info = label(
            "✦  بشپړه لاسي 3D فلم جوړونه  •  بې له AI ویډیو جوړولو",
            12f,
            Color.rgb(235, 240, 255),
            true
        )
        info.setPadding(0, dp(15), 0, dp(20))
        main.addView(info)

        val section = label("د 3D فلم جوړولو وسایل", 19f, Color.WHITE, true)
        section.gravity = Gravity.RIGHT
        main.addView(section)

        val sectionHint = label(
            "د فلم د جوړولو لپاره یوه برخه وټاکه",
            12f,
            Color.rgb(220, 228, 245)
        )
        sectionHint.gravity = Gravity.RIGHT
        val hintParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        hintParams.topMargin = dp(5)
        hintParams.bottomMargin = dp(12)
        main.addView(sectionHint, hintParams)

        val tools = listOf(
            arrayOf("🆕", "نوې 3D پروژه", "نوی فلم له صفره پیل کړه", "#48C6EF"),
            arrayOf("🧍", "3D کرکټرونه", "کرکټرونه جوړ او تنظیم کړه", "#FF8A65"),
            arrayOf("🌄", "3D چاپېریال", "ځایونه او صحنې جوړې کړه", "#66BB6A"),
            arrayOf("🧩", "3D ماډلونه", "خپل ماډلونه ورزیات کړه", "#AB8BFF"),
            arrayOf("🏃", "حرکت او انیمېشن", "کرکټرونو ته حرکت ورکړه", "#EC6FA9"),
            arrayOf("🎥", "کمره", "زاویه، زوم او حرکت تنظیم کړه", "#42A5F5"),
            arrayOf("💡", "رڼا", "د صحنې رڼا برابره کړه", "#FFD54F"),
            arrayOf("🎨", "مواد او رنګونه", "سطحې او بڼې بدلې کړه", "#26C6DA"),
            arrayOf("🎙️", "غږ او ډیالوګ", "خبرې، موسیقي او غږونه", "#F06292"),
            arrayOf("🎞️", "مهال‌وېش او ایډیټ", "صحنې او غږونه سره ونښلوه", "#7E9BFF"),
            arrayOf("▶️", "مخکتنه", "د فلم پایله وګوره", "#4DB6AC"),
            arrayOf("📤", "فلم صادرول", "وروستی فلم ویډیو کړه", "#FFA726"),
            arrayOf("📁", "زما فلمونه", "خوندي شوې پروژې پرانیزه", "#90A4AE"),
            arrayOf("📘", "لارښوونې", "د کارولو مرحلې زده کړه", "#5C9DED"),
            arrayOf("ℹ️", "زموږ په اړه", "د اپلیکیشن موخه او معلومات", "#9575CD")
        )

        for (i in tools.indices step 2) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            }

            for (j in i until minOf(i + 2, tools.size)) {
                val item = tools[j]
                val accent = Color.parseColor(item[3])
                val card = toolCard(item[0], item[1], item[2], accent, item[1])
                val params = LinearLayout.LayoutParams(0, dp(146), 1f)
                params.setMargins(dp(5), dp(5), dp(5), dp(5))
                row.addView(card, params)
            }
            main.addView(row)
        }

        val footer = label(
            "ستا 3D نړۍ  •  ستا کیسه  •  ستا فلم",
            13f,
            Color.rgb(220, 228, 245),
            true
        )
        val footerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        footerParams.topMargin = dp(24)
        main.addView(footer, footerParams)

        scroll.addView(main)
        setContentView(scroll)
    }
}
