package com.moviestudio.filmmaker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.util.concurrent.Executors

class AccountLinkActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("movie_studio_account", MODE_PRIVATE) }
    private val referralLink = "https://t.me/AI_MovieMaker_Bot?start=ref_7851941608"
    private var telegramId = ""
    private var timer: CountDownTimer? = null
    private var secondsLeft = 300
    private lateinit var page: LinearLayout
    private lateinit var status: TextView
    private val digits = mutableListOf<EditText>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 18, 34)
        window.navigationBarColor = Color.rgb(12, 18, 34)
        telegramId = prefs.getString("pending_telegram_id", "") ?: ""
        showIdPage()
    }

    private fun root(title: String, subtitle: String) {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(12,18,34)) }
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(22), dp(34), dp(22), dp(28))
        }
        page.addView(label("🎬 د فلم سټوډیو", 25, Color.WHITE, true))
        page.addView(label(title, 20, 0xFFBFCBFF.toInt(), true))
        page.addView(label(subtitle, 14, 0xFFD7DCEF.toInt(), false).apply {
            setPadding(dp(4), dp(12), dp(4), dp(20))
        })
        scroll.addView(page)
        setContentView(scroll)
    }

    private fun label(value: String, size: Int, color: Int, bold: Boolean) =
        TextView(this).apply {
            text = value; textSize = size.toFloat(); setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(6), dp(8), dp(6), dp(8))
        }

    private fun button(value: String, action: () -> Unit) =
        Button(this).apply {
            text = value; isAllCaps = false; textSize = 15f
            setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(75,75,145))
            setOnClickListener { action() }
        }

    private fun showIdPage() {
        timer?.cancel()
        root("۱ ـ د ټېلېګرام پېژندشمېره", "خپل ټېلېګرام حساب ته لومړی د بوټ /start پیغام واستوه، بیا خپله عددي پېژندشمېره ولیکه.")
        val idInput = EditText(this).apply {
            hint = "د ټېلېګرام عددي پېژندشمېره"
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setHintTextColor(0xFF8996B2.toInt())
            setBackgroundColor(0xFF26324A.toInt())
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setText(telegramId)
        }
        page.addView(idInput, params())
        status = label("", 13, 0xFFFFD28A.toInt(), false)
        page.addView(status)
        page.addView(button("📨 کوډ راولېږه") {
            val id = idInput.text.toString().trim()
            if (!id.matches(Regex("[0-9]{5,20}"))) {
                status.text = "سمه عددي پېژندشمېره ولیکه."
                return@button
            }
            telegramId = id
            requestCode()
        }, params(12))
        page.addView(button("🤖 لومړی بوټ فعال کړه") {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(referralLink))) }
            catch (_: Exception) { status.text = "د ټېلېګرام لینک نه پرانیستل شو." }
        }, params(8))
        page.addView(label("د بوټ لینک: $referralLink", 12, 0xFF62C7FF.toInt(), false))
    }

    private fun requestCode() {
        status.text = "کوډ غوښتل کېږي..."
        worker.execute {
            try {
                val result = ApiClient.requestCode(telegramId)
                runOnUiThread {
                    if (result.optBoolean("ok")) {
                        prefs.edit().putString("pending_telegram_id", telegramId).apply()
                        secondsLeft = result.optInt("expires_in_seconds", 300).coerceAtLeast(1)
                        showCodePage()
                    } else if (result.optBoolean("code_active")) {
                        secondsLeft = result.optInt("expires_in_seconds", 300).coerceAtLeast(1)
                        showCodePage()
                        status.text = result.optString("error", "پخوانی کوډ لا اعتبار لري.")
                    } else {
                        status.text = result.optString("error", "کوډ ونه لېږل شو.")
                        if (result.optBoolean("bot_not_started")) {
                            page.addView(button("🤖 بوټ پرانیزه او /start وکړه") {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(referralLink)))
                            }, params(8))
                        }
                    }
                }
            } catch (_: Exception) {
                runOnUiThread { status.text = "له سرور سره اړیکه ناکامه شوه؛ انټرنېټ وګوره." }
            }
        }
    }

    private fun showCodePage() {
        root("۲ ـ شپږ عددي تایید کوډ", "کوډ ستا ټېلېګرام ته استول شوی. شپږ شمېرې په جلا خانو کې ولیکه؛ د پخواني کوډ د اعتبار تر ختمېدو مخکې نوی کوډ نه شې غوښتلای.")
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER
        }
        digits.clear()
        repeat(6) { index ->
            val field = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                setSingleLine(true)
                textSize = 22f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setHintTextColor(0xFF8996B2.toInt())
                hint = "•"
                setBackgroundColor(0xFF26324A.toInt())
                filters = arrayOf(android.text.InputFilter.LengthFilter(1))
                addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        if (s?.length == 1 && index < 5) digits[index + 1].requestFocus()
                        if (s.isNullOrEmpty() && before == 1 && index > 0) digits[index - 1].requestFocus()
                    }
                    override fun afterTextChanged(s: android.text.Editable?) {}
                })
            }
            digits.add(field)
            val lp = LinearLayout.LayoutParams(0, dp(54), 1f).apply {
                marginStart = dp(3); marginEnd = dp(3)
            }
            row.addView(field, lp)
        }
        page.addView(row, params(12))
        val countdown = label("", 14, 0xFFFFD28A.toInt(), true)
        page.addView(countdown)
        status = label("کوډ ۵ دقیقې اعتبار لري او یوازې یو ځل کارېږي.", 13, 0xFFD7DCEF.toInt(), false)
        page.addView(status)
        page.addView(button("📋 کوډ پيسټ کړه") {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val raw = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
            val code = raw.filter { it.isDigit() }.take(6)
            if (code.isEmpty()) status.text = "په کلېپ‌بورډ کې شپږ عددي کوډ نشته."
            else {
                digits.forEachIndexed { i, f -> f.setText(code.getOrNull(i)?.toString() ?: "") }
                digits.getOrNull((code.length - 1).coerceAtLeast(0))?.requestFocus()
            }
        }, params(10))
        page.addView(button("✅ کوډ تایید او حساب ونښلوه") { verifyCode() }, params(8))
        val resend = button("🔄 نوی کوډ وغواړه") { requestCode() }
        resend.isEnabled = false
        page.addView(resend, params(8))
        page.addView(button("⬅️ بېرته د پېژندشمېرې پاڼې ته") { showIdPage() }, params(8))
        timer?.cancel()
        timer = object : CountDownTimer(secondsLeft * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                secondsLeft = (millisUntilFinished / 1000L).toInt()
                countdown.text = "د کوډ پاتې وخت: ${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2,'0')}"
            }
            override fun onFinish() {
                secondsLeft = 0
                countdown.text = "د کوډ وخت پای ته ورسېد."
                resend.isEnabled = true
                resend.alpha = 1f
                status.text = "اوس نوی کوډ غوښتلای شې."
            }
        }.start()
        resend.alpha = 0.45f
    }

    private fun verifyCode() {
        val code = digits.joinToString("") { it.text.toString() }
        if (!code.matches(Regex("[0-9]{6}"))) {
            status.text = "ټولې شپږ خانې ډکې کړه."
            return
        }
        status.text = "کوډ تاییدېږي..."
        worker.execute {
            try {
                val result = ApiClient.verifyCode(telegramId, code)
                runOnUiThread {
                    if (result.optBoolean("ok")) {
                        val token = result.optString("session_token")
                        if (token.isBlank()) {
                            status.text = "سرور د حساب نښه ونه لېږله."
                            return@runOnUiThread
                        }
                        prefs.edit().putString("session_token", token)
                            .remove("pending_telegram_id").apply()
                        Toast.makeText(this, "حساب په بریالیتوب ونښلول شو ✅", Toast.LENGTH_LONG).show()
                        setResult(RESULT_OK)
                        finish()
                    } else status.text = result.optString("error", "کوډ ناسم دی.")
                }
            } catch (_: Exception) {
                runOnUiThread { status.text = "تایید ناکام شو؛ انټرنېټ وګوره او بیا هڅه وکړه." }
            }
        }
    }

    private fun params(top: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    override fun onDestroy() {
        timer?.cancel()
        worker.shutdown()
        super.onDestroy()
    }
}
