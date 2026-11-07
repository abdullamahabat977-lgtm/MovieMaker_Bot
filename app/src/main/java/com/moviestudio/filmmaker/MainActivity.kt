package com.moviestudio.filmmaker

import android.os.Bundle
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
                intArrayOf(Color.rgb(31, 37, 57), Color.rgb(25, 30, 47)),
                18,
                true
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "$message برخه به په راتلونکې مرحله کې فعاله شي",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val iconView = label(icon, 30f, accent, true)
        iconView.background = rounded(
            intArrayOf(Color.rgb(45, 53, 79), Color.rgb(38, 45, 68)),
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

        val heroTitle = label("خپل فلم، په خپل لاس جوړ کړه", 22f, Color.WHITE, true)
        val heroTitleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        heroTitleParams.topMargin = dp(8)
        hero.addView(heroTitle, heroTitleParams)

        val heroText = label(
            "کرکټرونه جوړ کړه، صحنې برابرې کړه او خپله کیسه فلم کړه.",
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
            "✦  لاسي فلم جوړونه  •  بې له AI ویډیو جوړولو",
            12f,
            Color.rgb(235, 240, 255),
            true
        )
        info.setPadding(0, dp(15), 0, dp(20))
        main.addView(info)

        val section = label("د فلم جوړولو وسایل", 19f, Color.WHITE, true)
        section.gravity = Gravity.RIGHT
        main.addView(section)

        val sectionHint = label(
            "د خپلې پروژې لپاره یوه برخه وټاکه",
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
            arrayOf("👤", "کرکټر جوړول", "څېرې او بڼه جوړه کړه", "#72C7FF"),
            arrayOf("🏞️", "د فلم صحنې", "د کیسې ځایونه برابر کړه", "#79D6B0"),
            arrayOf("🎭", "حرکتونه", "د کرکټر کړنې وټاکه", "#D7A5FF"),
            arrayOf("🗣️", "ډیالوګونه", "خبرې او متن ولیکه", "#FFC47D"),
            arrayOf("🎥", "کمره", "زاویه او نږدېوالی وټاکه", "#82B7FF"),
            arrayOf("🎵", "غږ او موسیقي", "غږونه او شالیدي موسیقي", "#F2A1C2"),
            arrayOf("✂️", "فلم ایډیټ", "برخې سره ونښلوه", "#FFB18C"),
            arrayOf("📁", "زما فلمونه", "خپلې پروژې وګوره", "#A9C0FF")
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
                val params = LinearLayout.LayoutParams(0, dp(150), 1f)
                params.setMargins(dp(5), dp(5), dp(5), dp(5))
                row.addView(card, params)
            }
            main.addView(row)
        }

        val footer = label(
            "ستا خیال  •  ستا کیسه  •  ستا فلم",
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
