package com.example.todo_list.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todo_list.security.AuthManager
import com.example.todo_list.ui.components.auth.IosNotificationBanner
import com.example.todo_list.ui.theme.SystemGroupedBackground

enum class AuthFlowStep {
    LOGIN,
    REGISTER_PHONE,
    OTP_VERIFICATION,
    ACCOUNT_SETUP
}

/**
 * Master Authentication Container Composable.
 * Orchestrates Login, Registration, OTP Verification, and Account Setup flows.
 */
@Composable
fun AuthScreen(
    onAuthComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Initial auto-detection: If account not created -> RegisterPhone; otherwise -> Login
    var currentStep by remember {
        mutableStateOf(
            if (AuthManager.isAccountCreated) AuthFlowStep.LOGIN else AuthFlowStep.REGISTER_PHONE
        )
    }

    var pendingPhoneNumber by remember { mutableStateOf("") }
    var activeOtpCode by remember { mutableStateOf("") }
    var autofillCode by remember { mutableStateOf<String?>(null) }
    var showOtpBanner by remember { mutableStateOf(false) }

    LaunchedEffect(AuthManager.isAccountCreated) {
        if (AuthManager.isAccountCreated && currentStep == AuthFlowStep.REGISTER_PHONE && pendingPhoneNumber.isEmpty()) {
            currentStep = AuthFlowStep.LOGIN
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemGroupedBackground)
    ) {
        // Active Sub-Screen Animated Container
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "AuthFlowTransition"
        ) { step ->
            when (step) {
                AuthFlowStep.LOGIN -> {
                    LoginScreen(
                        onLoginSuccess = onAuthComplete,
                        onNavigateToRegister = {
                            currentStep = AuthFlowStep.REGISTER_PHONE
                        },
                        onForgotPasswordOtpDispatched = { code ->
                            activeOtpCode = code
                            autofillCode = null
                            showOtpBanner = true
                        },
                        autofillCode = autofillCode,
                        onAutofillConsumed = {
                            autofillCode = null
                        }
                    )
                }

                AuthFlowStep.REGISTER_PHONE -> {
                    RegisterPhoneScreen(
                        onCodeSent = { phone, otp ->
                            pendingPhoneNumber = phone
                            activeOtpCode = otp
                            autofillCode = null
                            showOtpBanner = true
                            currentStep = AuthFlowStep.OTP_VERIFICATION
                        },
                        onNavigateToLogin = {
                            currentStep = AuthFlowStep.LOGIN
                        }
                    )
                }

                AuthFlowStep.OTP_VERIFICATION -> {
                    OtpVerificationScreen(
                        phoneNumber = pendingPhoneNumber,
                        autofillCode = autofillCode,
                        onAutofillConsumed = {
                            autofillCode = null
                        },
                        onOtpVerified = {
                            showOtpBanner = false
                            currentStep = AuthFlowStep.ACCOUNT_SETUP
                        },
                        onBackToPhone = {
                            showOtpBanner = false
                            currentStep = AuthFlowStep.REGISTER_PHONE
                        },
                        onResendRequested = { newOtp ->
                            activeOtpCode = newOtp
                            autofillCode = null
                            showOtpBanner = true
                        }
                    )
                }

                AuthFlowStep.ACCOUNT_SETUP -> {
                    AccountSetupScreen(
                        verifiedPhone = pendingPhoneNumber,
                        onRegistrationComplete = {
                            showOtpBanner = false
                            onAuthComplete()
                        }
                    )
                }
            }
        }

        // Top Simulated iOS Notification Banner for Incoming OTP
        IosNotificationBanner(
            visible = showOtpBanner,
            otpCode = activeOtpCode,
            onAutofillClick = { code ->
                autofillCode = code
            },
            onDismiss = {
                showOtpBanner = false
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        )
    }
}
