package com.arnavpaul.smartcalc

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class Message(val role: String, val content: String)

object GroqClient {

    fun chat(history: List<Message>, onResult: (String) -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                val conn = URL("https://api.groq.com/openai/v1/chat/completions")
                    .openConnection() as HttpsURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 30000
                conn.readTimeout = 60000
                conn.doOutput = true

                val msgs = JSONArray()
                msgs.put(JSONObject().put("role", "system")
                    .put("content", "You are a math and general assistant inside a calculator app. Keep answers short and clear."))
                history.forEach { msgs.put(JSONObject().put("role", it.role).put("content", it.content)) }
                val body = JSONObject()
                    .put("model", "llama-3.3-70b-versatile")
                    .put("messages", msgs)
                    .put("temperature", 0.4)

                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    .bufferedReader().use { it.readText() }

                if (code in 200..299) {
                    val content = JSONObject(text)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    onResult(content.trim())
                } else {
                    onError("API error " + code + ": " + text.take(300))
                }
            } catch (e: Exception) {
                onError("Network error: " + (e.message ?: "unknown"))
            }
        }.start()
    }
}
