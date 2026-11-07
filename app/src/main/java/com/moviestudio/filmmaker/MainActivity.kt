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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun background(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(13, 15, 28)
        window.navigationBarColor = Color.rgb(13, 15, 28)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.rgb(13, 15, 28))
        scroll.layoutDirection = View.LAYOUT_DIRECTION_RTL

        val main = LinearLayout(this)
        main.orientation = LinearLayout.VERTICAL
        main.setPadding(dp(18), dp(22), dp(18), dp(25))

        val title = TextView(this)
        title.text = "🎬 د فلم جوړولو سټوډیو"
        title.textSize = 25f
        title.setTextColor(Color.WHITE)
        title.typeface = Typeface.DEFAULT_BOLD
        title.gravity = Gravity.CENTER
        title.setPadding(0, dp(10), 0, dp(18))
        main.addView(title)

        val banner = LinearLayout(this)
        banner.orientation = LinearLayout.VERTICAL
        banner.gravity = Gravity.CENTER
        banner.setPadding(dp(18), dp(25), dp(18), dp(25))
        banner.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.rgb(75, 45, 170), Color.rgb(20, 125, 170))
        ).apply {
            cornerRadius = dp(22).toFloat()
        }

        val bannerIcon = TextView(this)
        bannerIcon.text = "🎥"
        bannerIcon.textSize = 48f
        bannerIcon.gravity = Gravity.CENTER
        banner.addView(bannerIcon)

        val bannerTitle = TextView(this)
        bannerTitle.text = "خپل فلم پخپله جوړ کړئ"
        bannerTitle.textSize = 23f
        bannerTitle.setTextColor(Color.WHITE)
        bannerTitle.typeface = Typeface.DEFAULT_BOLD
        bannerTitle.gravity = Gravity.CENTER
        bannerTitle.setPadding(0, dp(10), 0, dp(8))
        banner.addView(bannerTitle)

        val bannerSubtitle = TextView(this)
        bannerSubtitle.text = "کرکټرونه، صحنې، حرکتونه او بشپړ فلم"
        bannerSubtitle.textSize = 14f
        bannerSubtitle.setTextColor(Color.WHITE)
        bannerSubtitle.gravity = Gravity.CENTER
        banner.addView(bannerSubtitle)

        val bannerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        bannerParams.bottomMargin = dp(22)
        main.addView(banner, bannerParams)

        val sectionTitle = TextView(this)
        sectionTitle.text = "🛠️ د فلم جوړولو وسایل"
        sectionTitle.textSize = 19f
        sectionTitle.setTextColor(Color.WHITE)
        sectionTitle.typeface = Typeface.DEFAULT_BOLD
        sectionTitle.gravity = Gravity.RIGHT
        sectionTitle.setPadding(0, dp(5), 0, dp(15))
        main.addView(sectionTitle)

        val buttons = listOf(
            "👤 کرکټر جوړول",
            "🏞️ د فلم صحنې",
            "🎭 د کرکټر حرکتونه",
            "🗣️ خبرې او ډیالوګونه",
            "🎥 د کامرې تنظیمات",
            "🎵 غږ او موسیقي",
            "✂️ د فلم ایډیټ",
            "📁 زما فلمونه"
        )

        for (i in buttons.indices step 2) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutDirection = View.LAYOUT_DIRECTION_RTL

            for (j in i until minOf(i + 2, buttons.size)) {
                val button = Button(this)
                button.text = buttons[j]
                button.textSize = 15f
                button.isAllCaps = false
                button.setTextColor(Color.WHITE)
                button.typeface = Typeface.DEFAULT_BOLD
                button.gravity = Gravity.CENTER
                button.background = background(Color.rgb(35, 42, 65), 16)
                button.setPadding(dp(5), dp(12), dp(5), dp(12))

                val params = LinearLayout.LayoutParams(0, dp(105), 1f)
                params.setMargins(dp(5), dp(6), dp(5), dp(6))

                button.setOnClickListener {
                    Toast.makeText(
                        this,
                        "${buttons[j]} برخه به په راتلونکو مرحلو کې فعاله شي",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                row.addView(button, params)
            }
            main.addView(row)
        }

        val footer = TextView(this)
        footer.text = "🎬 ستا خیال، ستا فلم، ستا سټوډیو"
        footer.textSize = 14f
        footer.setTextColor(Color.LTGRAY)
        footer.gravity = Gravity.CENTER
        footer.setPadding(0, dp(25), 0, dp(10))
        main.addView(footer)

        scroll.addView(main)
        setContentView(scroll)
    }
}
