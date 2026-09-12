package com.example.todo_list.ai

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages configuration and credentials for TaskFlow Intelligence (Small LLM).
 * Persists API key, model selection, and heuristic fallback preferences securely in private app storage.
 */
object AiConfigurationManager {

    private const val PREFS_NAME = "taskflow_ai_preferences"
    private const val KEY_API_KEY = "gemini_api_key"
    private const val KEY_MODEL = "selected_model"
    private const val KEY_FALLBACK_ENABLED = "heuristic_fallback_enabled"

    const val MODEL_GEMINI_1_5_FLASH = "gemini-1.5-flash"
    const val MODEL_GEMINI_2_0_FLASH = "gemini-2.0-flash"
    const val MODEL_LOCAL_HEURISTIC = "local-heuristic"

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    var apiKey: String
        get() = sharedPreferences?.getString(KEY_API_KEY, "") ?: ""
        set(value) {
            sharedPreferences?.edit()?.putString(KEY_API_KEY, value.trim())?.apply()
        }

    var selectedModel: String
        get() = sharedPreferences?.getString(KEY_MODEL, MODEL_GEMINI_1_5_FLASH) ?: MODEL_GEMINI_1_5_FLASH
        set(value) {
            sharedPreferences?.edit()?.putString(KEY_MODEL, value)?.apply()
        }

    var isHeuristicFallbackEnabled: Boolean
        get() = sharedPreferences?.getBoolean(KEY_FALLBACK_ENABLED, true) ?: true
        set(value) {
            sharedPreferences?.edit()?.putBoolean(KEY_FALLBACK_ENABLED, value)?.apply()
        }

    val isConfigured: Boolean
        get() = apiKey.isNotBlank() || selectedModel == MODEL_LOCAL_HEURISTIC

    /**
     * Tests the connectivity and authentication for the configured Gemini API key.
     * Uses a lightweight minimal prompt to verify credentials.
     */
    suspend fun testConnection(testKey: String = apiKey): Result<String> = withContext(Dispatchers.IO) {
        val keyToTest = testKey.trim()
        if (keyToTest.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be empty. Please enter your Gemini API key."))
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_GEMINI_1_5_FLASH:generateContent?key=$keyToTest"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val requestBody = JSONObject().apply {
                put("contents", org.json.JSONArray().put(
                    JSONObject().apply {
                        put("parts", org.json.JSONArray().put(
                            JSONObject().apply {
                                put("text", "Respond with 'OK' if you can read this.")
                            }
                        ))
                    }
                ))
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Result.success("Connection verified successfully! Gemini 1.5 Flash is active and ready.")
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                val errorMessage = parseErrorMessage(errorStream, responseCode)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.localizedMessage ?: "Unknown network error"}"))
        }
    }

    private fun parseErrorMessage(errorBody: String, statusCode: Int): String {
        return try {
            val json = JSONObject(errorBody)
            val errorObj = json.optJSONObject("error")
            val message = errorObj?.optString("message") ?: "HTTP error $statusCode"
            "Gemini API Error ($statusCode): $message"
        } catch (_: Exception) {
            "HTTP $statusCode: Failed to authenticate API Key."
        }
    }
}
