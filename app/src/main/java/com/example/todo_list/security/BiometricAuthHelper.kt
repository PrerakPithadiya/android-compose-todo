package com.example.todo_list.security

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.todo_list.ui.components.icons.IosFaceIdIcon

enum class BiometricAvailability {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    HW_UNAVAILABLE
}

enum class BiometricModality {
    NONE,
    FINGERPRINT_ONLY,
    FACE_ONLY,
    FACE_AND_FINGERPRINT,
    GENERIC_BIOMETRIC
}

object BiometricAuthHelper {

    fun checkBiometricAvailability(context: Context): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HW_UNAVAILABLE
            else -> BiometricAvailability.NO_HARDWARE
        }
    }

    fun isBiometricSupported(context: Context): Boolean {
        val status = checkBiometricAvailability(context)
        return status == BiometricAvailability.AVAILABLE || status == BiometricAvailability.NOT_ENROLLED
    }

    fun getBiometricModality(context: Context): BiometricModality {
        val pm = context.packageManager
        val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_FACE)
        } else {
            pm.hasSystemFeature("android.hardware.biometrics.face")
        }

        return when {
            hasFace && hasFingerprint -> BiometricModality.FACE_AND_FINGERPRINT
            hasFace -> BiometricModality.FACE_ONLY
            hasFingerprint -> BiometricModality.FINGERPRINT_ONLY
            isBiometricSupported(context) -> BiometricModality.GENERIC_BIOMETRIC
            else -> BiometricModality.NONE
        }
    }

    fun getBiometricDisplayName(context: Context): String {
        return when (getBiometricModality(context)) {
            BiometricModality.FACE_ONLY -> "Face ID"
            BiometricModality.FINGERPRINT_ONLY -> "Fingerprint"
            BiometricModality.FACE_AND_FINGERPRINT -> "Face ID & Fingerprint"
            BiometricModality.GENERIC_BIOMETRIC -> "Face ID & Biometrics"
            BiometricModality.NONE -> "Biometrics"
        }
    }

    fun getBiometricPromptSubtitle(context: Context): String {
        return when (getBiometricModality(context)) {
            BiometricModality.FACE_ONLY -> "Look at your device to continue"
            BiometricModality.FINGERPRINT_ONLY -> "Touch fingerprint sensor to continue"
            BiometricModality.FACE_AND_FINGERPRINT -> "Look at device or touch fingerprint sensor"
            BiometricModality.GENERIC_BIOMETRIC -> "Verify your face or fingerprint to continue"
            BiometricModality.NONE -> "Verify biometric identity to continue"
        }
    }

    fun getBiometricSettingsSubtitle(context: Context, isEnabled: Boolean): String {
        if (!isEnabled) {
            return when (getBiometricModality(context)) {
                BiometricModality.FACE_ONLY -> "Use Face ID instead of passcode"
                BiometricModality.FINGERPRINT_ONLY -> "Use fingerprint instead of passcode"
                BiometricModality.FACE_AND_FINGERPRINT -> "Use Face ID or fingerprint instead of passcode"
                else -> "Use biometrics instead of passcode"
            }
        }
        return when (getBiometricModality(context)) {
            BiometricModality.FACE_ONLY -> "Face ID enabled for instant app unlock"
            BiometricModality.FINGERPRINT_ONLY -> "Fingerprint enabled for instant app unlock"
            BiometricModality.FACE_AND_FINGERPRINT -> "Face ID & Fingerprint enabled for instant unlock"
            else -> "Biometric unlock enabled"
        }
    }

    fun getBiometricSetupSubtitle(context: Context): String {
        return when (getBiometricModality(context)) {
            BiometricModality.FACE_ONLY -> "Scan your face to enable Face ID unlock"
            BiometricModality.FINGERPRINT_ONLY -> "Scan your fingerprint to enable biometric unlock"
            BiometricModality.FACE_AND_FINGERPRINT -> "Verify your face or fingerprint to enable unlock"
            else -> "Confirm biometric identity to enable unlock"
        }
    }

    fun getBiometricEnrollmentTitle(context: Context): String {
        return when (getBiometricModality(context)) {
            BiometricModality.FACE_ONLY -> "Enroll Face ID"
            BiometricModality.FINGERPRINT_ONLY -> "Enroll Fingerprint"
            else -> "Enroll Face ID or Fingerprint"
        }
    }

    fun getBiometricEnrollmentMessage(context: Context): String {
        return when (getBiometricModality(context)) {
            BiometricModality.FACE_ONLY -> "No Face ID profile is currently registered on this device. Would you like to open device settings to register your face?"
            BiometricModality.FINGERPRINT_ONLY -> "No fingerprint is currently registered on this device. Would you like to open device settings to add a fingerprint?"
            else -> "No Face ID or fingerprint is currently registered on this device. Would you like to open device settings to configure biometric unlock?"
        }
    }

    fun getBiometricIcon(context: Context): ImageVector {
        return when (getBiometricModality(context)) {
            BiometricModality.FACE_ONLY,
            BiometricModality.FACE_AND_FINGERPRINT,
            BiometricModality.GENERIC_BIOMETRIC -> IosFaceIdIcon
            BiometricModality.FINGERPRINT_ONLY -> Icons.Outlined.Fingerprint
            BiometricModality.NONE -> IosFaceIdIcon
        }
    }

    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "Unlock TaskFlow",
        subtitle: String = getBiometricPromptSubtitle(activity),
        negativeButtonText: String = "Use Passcode",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If user clicked negative button or canceled, report error without crashing
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    val modality = getBiometricModality(activity)
                    val failureMsg = when (modality) {
                        BiometricModality.FACE_ONLY -> "Face not recognized. Please look directly at the screen."
                        BiometricModality.FINGERPRINT_ONLY -> "Fingerprint not recognized. Please try again."
                        else -> "Face or fingerprint not recognized. Please try again."
                    }
                    onError(failureMsg)
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun openEnrollmentSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(
                        Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BIOMETRIC_STRONG or BIOMETRIC_WEAK
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(enrollIntent)
            } else {
                val securityIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(securityIntent)
            }
        } catch (_: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }
}
