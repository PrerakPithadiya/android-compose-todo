package com.example.todo_list.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Result of an SMS delivery attempt via Fast2SMS.
 */
sealed class SmsDeliveryResult {
    data class Success(
        val requestId: String,
        val message: String
    ) : SmsDeliveryResult()

    data class Failure(
        val errorMessage: String,
        val statusCode: Int? = null
    ) : SmsDeliveryResult()
}

/**
 * Service to dispatch real SMS OTP messages using the Fast2SMS Gateway API.
 */
object Fast2SmsService {
    // Fast2SMS API Key (set via Fast2SmsService.apiKey or build configuration)
    var apiKey: String = ""

    // Unique OTP Template ID from Fast2SMS Dashboard (OTP SMS section)
    var otpId: String = ""


    private const val FAST2SMS_OTP_SEND_URL = "https://www.fast2sms.com/dev/otp/send"
    private const val FAST2SMS_BULK_URL = "https://www.fast2sms.com/dev/bulkV2"

    /**
     * Cleans phone number to standard 10-digit format required by Fast2SMS.
     * Extracts the last 10 digits for Indian numbers (strips +91, 0, spaces, dashes).
     */
    fun cleanPhoneNumber(rawPhone: String): String {
        val digitsOnly = rawPhone.filter { it.isDigit() }
        return if (digitsOnly.length > 10) {
            digitsOnly.takeLast(10)
        } else {
            digitsOnly
        }
    }

    /**
     * Dispatches a real OTP SMS to the target phone number using Fast2SMS API.
     * Uses the dedicated /dev/otp/send endpoint if otpId is provided, or bulkV2 route.
     * Runs asynchronously on [Dispatchers.IO].
     */
    suspend fun sendOtpSms(
        phoneNumber: String,
        otpCode: String
    ): SmsDeliveryResult = withContext(Dispatchers.IO) {
        val cleanNumber = cleanPhoneNumber(phoneNumber)

        if (cleanNumber.length != 10) {
            return@withContext SmsDeliveryResult.Failure(
                errorMessage = "Fast2SMS requires a valid 10-digit mobile number. Given: '$phoneNumber'"
            )
        }

        try {
            val useDedicatedOtpEndpoint = otpId.isNotBlank()
            val targetUrl = if (useDedicatedOtpEndpoint) FAST2SMS_OTP_SEND_URL else FAST2SMS_BULK_URL

            val url = URL(targetUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 12000
                doInput = true
                doOutput = true
                setRequestProperty("authorization", apiKey)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val payload = if (useDedicatedOtpEndpoint) {
                JSONObject().apply {
                    put("mobile", cleanNumber)
                    put("otp_id", otpId)
                    put("otp_length", otpCode.length)
                    put("otp", otpCode)
                }
            } else {
                JSONObject().apply {
                    put("route", "otp")
                    put("variables_values", otpCode)
                    put("numbers", cleanNumber)
                }
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseText = try {
                val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                if (inputStream != null) {
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
                } else {
                    ""
                }
            } catch (_: Exception) {
                ""
            }

            if (responseText.isNotBlank()) {
                val json = try {
                    JSONObject(responseText)
                } catch (_: Exception) {
                    null
                }

                if (json != null) {
                    val isReturnTrue = json.optBoolean("return", false)
                    val statusCode = json.optInt("status_code", responseCode)

                    if (isReturnTrue || statusCode == 200) {
                        val requestId = json.optString("request_id", "")
                        val message = json.optJSONArray("message")?.optString(0) ?: "SMS sent successfully."
                        return@withContext SmsDeliveryResult.Success(requestId = requestId, message = message)
                    } else {
                        // Error returned by Fast2SMS (e.g. 999 wallet recharge required, invalid template, etc.)
                        val msg = if (json.has("message")) {
                            val msgObj = json.get("message")
                            if (msgObj is org.json.JSONArray) {
                                msgObj.optString(0, "Fast2SMS Error")
                            } else {
                                msgObj.toString()
                            }
                        } else {
                            "Fast2SMS error code $statusCode"
                        }

                        return@withContext SmsDeliveryResult.Failure(
                            errorMessage = msg,
                            statusCode = statusCode
                        )
                    }
                }
            }

            if (responseCode in 200..299) {
                SmsDeliveryResult.Success(requestId = "", message = "SMS sent successfully.")
            } else {
                SmsDeliveryResult.Failure(
                    errorMessage = "HTTP $responseCode from Fast2SMS gateway: $responseText",
                    statusCode = responseCode
                )
            }
        } catch (e: Exception) {
            SmsDeliveryResult.Failure(
                errorMessage = "Network error connecting to Fast2SMS: ${e.localizedMessage ?: e.message}"
            )
        }
    }
}
