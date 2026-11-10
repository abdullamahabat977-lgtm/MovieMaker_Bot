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
            text = "یوه ګوته: کمره تاوول  •  دوه ګوتې: زوم"
            textSize = 13f
            setTextColor(0xFFD7DCEF.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        root.addView(instructions)

        try {
            val renderer = Scene3DView(this)
            sceneView = renderer
            root.addView(renderer, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ))
        } catch (error: RuntimeException) {
            android.util.Log.e("MovieStudio3D", "Could not create GLSurfaceView", error)
            showRendererFailure()
            return
        }

        val footer = TextView(this).apply {
            text = "اوس د ازموینې مکعب او ځمکنۍ شبکه ښکاري؛ د کرکټرونو او GLB ماډلونو واردول لا د جوړولو په پړاو کې دي."
            textSize = 12f
            setTextColor(0xFFD7DCEF.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(10))
        }
        root.addView(footer)
        setContentView(root)
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
