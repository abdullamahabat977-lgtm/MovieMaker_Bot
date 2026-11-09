package com.moviestudio.filmmaker

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small HTTPS client for the Movie Studio account API.
 * Never place Telegram bot secrets in the Android app.
 */
object ApiClient {
    private const val BASE_URL = "https://movie-studio-api.abdullamahabat977.workers.dev"

    @Throws(Exception::class)
    private fun request(path: String, method: String, body: JSONObject? = null, token: String? = null): JSONObject {
        val connection = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return if (text.isBlank()) JSONObject().put("ok", false).put("error", "سرور تش ځواب راکړ")
            else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    fun requestCode(telegramId: String) =
        request("/api/auth/request-code", "POST", JSONObject().put("telegram_id", telegramId))

    fun verifyCode(telegramId: String, code: String) =
        request("/api/auth/verify-code", "POST",
            JSONObject().put("telegram_id", telegramId).put("code", code))

    fun me(token: String) = request("/api/me", "GET", token = token)
}
