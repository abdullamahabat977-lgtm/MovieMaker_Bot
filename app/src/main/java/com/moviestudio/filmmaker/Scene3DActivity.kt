package com.moviestudio.filmmaker

import android.app.Activity
import android.app.ActivityManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Full-screen 3D workspace. The GLSurfaceView is owned by this Activity,
 * so Android can pause/resume its GL thread with the Activity lifecycle.
 */
class Scene3DActivity : Activity() {
    private var sceneView: Scene3DView? = null
    private var glViewResumed = false

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.i("MovieStudio3D", "Activity onCreate")
        window.statusBarColor = Color.rgb(16, 21, 37)
        window.navigationBarColor = Color.rgb(16, 21, 37)

        if (!supportsOpenGles2()) {
            showUnsupportedMessage()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.rgb(20, 28, 43))
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setBackgroundColor(Color.rgb(16, 21, 37))
        }
        val back = Button(this).apply {
            text = "← شاته"
            isAllCaps = false
            setOnClickListener { finish() }
        }
        val title = TextView(this).apply {
            text = "د 3D صحنې کاري پاڼه"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(8), 0)
        }
        toolbar.addView(back, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        toolbar.addView(title, LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ))
        root.addView(toolbar)

        val instructions = TextView(this).apply {
            text = "یوه ګوته: کمره تاوول  •  دوه ګوتې: زوم  •  د 3D کرکټر او مکعب ترمنځ انتخاب"
            textSize = 13f
            setTextColor(0xFFD7DCEF.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        root.addView(instructions)

        val renderer = try {
            Scene3DView(this).also { sceneView = it }
        } catch (error: RuntimeException) {
            android.util.Log.e("MovieStudio3D", "Could not create GLSurfaceView", error)
            showRendererFailure()
            return
        }

        val modeBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }
        fun modeButton(label: String, mode: Int): Button = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            minHeight = dp(44)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(if (mode == Scene3DView.SCENE_CHARACTER) 0xFF315D91.toInt() else 0xFF40516C.toInt())
                setStroke(dp(1), 0xFF8199BD.toInt())
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener { renderer.setSceneMode(mode) }
        }
        modeBar.addView(modeButton("🧍  3D کرکټر", Scene3DView.SCENE_CHARACTER),
            LinearLayout.LayoutParams(0, dp(48), 1f))
        modeBar.addView(modeButton("🧊  مکعب", Scene3DView.SCENE_CUBE),
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(8) })
        root.addView(modeBar)

        // د کرکټر د بڼې ساده رنګ‌بدلوونکی؛ دا انتخابونه اوس یوازې د مخکتنې لپاره دي.
        val palettePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(10), dp(3), dp(10), dp(3))
            setBackgroundColor(0xFF1A263B.toInt())
        }
        addPaletteRow(
            palettePanel,
            "د پوستکي رنګ",
            listOf("#E8B98F", "#B87A56", "#8A583C", "#5B3828")
        ) { renderer.setSkinPalette(it) }
        addPaletteRow(
            palettePanel,
            "د کمیس رنګ",
            listOf("#2963B0", "#B63D42", "#26835A", "#7350A8", "#C58A2A", "#38465B")
        ) { renderer.setShirtPalette(it) }
        root.addView(palettePanel)

        root.addView(renderer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val footer = TextView(this).apply {
            text = "د پوستکي او کمیس رنګونه بدل کړه. دا رنګونه اوس یوازې په مخکتنه کې ښکاري؛ د کرکټر خوندي کول، ریګ او حرکتونه په راتلونکو پړاوونو کې جوړېږي."
            textSize = 12f
            setTextColor(0xFFD7DCEF.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(10))
        }
        root.addView(footer)
        setContentView(root)
    }

    private fun addPaletteRow(
        parent: LinearLayout,
        titleText: String,
        colors: List<String>,
        onSelect: (Int) -> Unit
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
        }
        val label = TextView(this).apply {
            text = titleText
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            maxLines = 2
        }
        row.addView(label, LinearLayout.LayoutParams(dp(98), dp(42)))

        val scroller = android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val chips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
        }
        colors.forEachIndexed { index, hex ->
            val chip = Button(this).apply {
                text = "●"
                textSize = 17f
                isAllCaps = false
                minWidth = dp(42)
                minHeight = dp(40)
                setPadding(0, 0, 0, 0)
                setTextColor(Color.WHITE)
                contentDescription = "$titleText ${index + 1}"
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor(hex))
                    setStroke(dp(1), 0xFFE5EAF5.toInt())
                    cornerRadius = dp(10).toFloat()
                }
                setOnClickListener { onSelect(index) }
            }
            chips.addView(chip, LinearLayout.LayoutParams(dp(44), dp(40)).apply {
                marginStart = dp(5)
            })
        }
        scroller.addView(chips)
        row.addView(scroller, LinearLayout.LayoutParams(0, dp(44), 1f))
        parent.addView(row, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(46)
        ).apply { topMargin = dp(1) })
    }

    private fun supportsOpenGles2(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val info = manager.deviceConfigurationInfo
        return info != null && info.reqGlEsVersion >= 0x00020000
    }

    private fun showUnsupportedMessage() {
        val message = TextView(this).apply {
            text = "دا موبایل د OpenGL ES 2.0 ملاتړ نه لري؛ له همدې امله 3D مخکتنه نه شي پرانیستل کېدای."
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        val back = Button(this).apply {
            text = "شاته"
            isAllCaps = false
            setOnClickListener { finish() }
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(20, 28, 43))
            addView(message)
            addView(back)
        }
        setContentView(page)
    }

    private fun pauseGlViewIfNeeded() {
        if (!glViewResumed) return
        try {
            sceneView?.onPause()
        } catch (error: RuntimeException) {
            android.util.Log.e("MovieStudio3D", "GLSurfaceView pause failed", error)
        } finally {
            glViewResumed = false
        }
    }

    // GLSurfaceView owns its SurfaceHolder callbacks; the Activity owns thread pause/resume.
    override fun onResume() {
        super.onResume()
        android.util.Log.i("MovieStudio3D", "Activity onResume")
        try {
            sceneView?.onResume()
            glViewResumed = sceneView != null
        } catch (error: RuntimeException) {
            android.util.Log.e("MovieStudio3D", "GLSurfaceView resume failed", error)
            showRendererFailure()
        }
    }

    override fun onPause() {
        android.util.Log.i("MovieStudio3D", "Activity onPause")
        pauseGlViewIfNeeded()
        super.onPause()
    }

    override fun onDestroy() {
        android.util.Log.i("MovieStudio3D", "Activity onDestroy")
        // Defensive fallback if destruction occurs while the GL view is still resumed.
        pauseGlViewIfNeeded()
        sceneView = null
        super.onDestroy()
    }

    private fun showRendererFailure() {
        val message = TextView(this).apply {
            text = "د 3D چاپېریال پیل نه شو. اپلیکیشن بند نه شو؛ بېرته لاړ شه او بیا هڅه وکړه."
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        val back = Button(this).apply {
            text = "شاته"
            isAllCaps = false
            setOnClickListener { finish() }
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(20, 28, 43))
            addView(message)
            addView(back)
        }
        setContentView(page)
    }
}
