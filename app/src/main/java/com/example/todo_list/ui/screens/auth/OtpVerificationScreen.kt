package com.example.todo_list.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.security.AuthManager
import com.example.todo_list.ui.components.auth.IosOtpInputView
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Step 2 of Registration: 6-Digit Phone OTP Verification Screen (Apple HIG).
 */
@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    onOtpVerified: () -> Unit,
    onBackToPhone: () -> Unit,
    onResendRequested: (newOtp: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var enteredOtp by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }

    // Visible Countdown Timer for OTP Validity (60 Seconds)
    var otpTimerKey by remember { mutableIntStateOf(0) }
    var otpRemainingSeconds by remember { mutableIntStateOf(AuthManager.OTP_VALIDITY_SECONDS) }
    var isOtpExpired by remember { mutableStateOf(false) }

    // 30-Second Cooldown Timer for Resend OTP
    var resendCountdown by remember { mutableIntStateOf(30) }
    var canResend by remember { mutableStateOf(false) }

    LaunchedEffect(otpTimerKey) {
        otpRemainingSeconds = AuthManager.OTP_VALIDITY_SECONDS
        isOtpExpired = false
        resendCountdown = 30
        canResend = false
        while (otpRemainingSeconds > 0) {
            delay(1000)
            otpRemainingSeconds--
            if (resendCountdown > 0) {
                resendCountdown--
            } else {
                canResend = true
            }
        }
        isOtpExpired = true
        canResend = true
        AuthManager.expireCurrentOtp()
        HapticManager.performWarning(context)
    }

    val formattedCountdown = remember(otpRemainingSeconds) {
        val minutes = otpRemainingSeconds / 60
        val seconds = otpRemainingSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    val maskedPhone = remember(phoneNumber) {
        if (phoneNumber.length > 7) {
            val start = phoneNumber.take(6)
            val end = phoneNumber.takeLast(4)
            "$start ••• $end"
        } else {
            phoneNumber
        }
    }

    val verifyCodeAction = { codeToVerify: String ->
        if (codeToVerify.length == 6 && !isVerifying) {
            isVerifying = true
            if (isOtpExpired || AuthManager.isOtpExpired()) {
                HapticManager.performError(context)
                isError = true
                errorMessage = "Verification code has expired. Please tap 'Resend Code'."
                isVerifying = false
            } else if (AuthManager.verifyOtp(codeToVerify)) {
                HapticManager.performSuccess(context)
                isError = false
                isVerifying = false
                onOtpVerified()
            } else {
                HapticManager.performError(context)
                isError = true
                errorMessage = "Invalid verification code. Please try again."
                isVerifying = false
                coroutineScope.launch {
                    delay(1500)
                    isError = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SystemGroupedBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Navigation & Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Back Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = {
                        HapticManager.performClick(context)
                        onBackToPhone()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SystemBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SMS Verification Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SystemBlue.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sms,
                    contentDescription = "SMS Verification",
                    tint = SystemBlue,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Verify Phone",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "We sent a 6-digit verification code to\n$maskedPhone",
                fontSize = 15.sp,
                color = SystemLabelSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Edit Phone Number",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SystemBlue,
                modifier = Modifier.clickable {
                    HapticManager.performClick(context)
                    onBackToPhone()
                }
            )
        }

        // Center Input Section: Countdown Badge + 6 Digit Boxes + Error
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visible Countdown Timer Badge (Apple HIG)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when {
                    isOtpExpired -> SystemRed.copy(alpha = 0.10f)
                    otpRemainingSeconds <= 10 -> SystemOrange.copy(alpha = 0.12f)
                    else -> SystemBlue.copy(alpha = 0.08f)
                },
                border = androidx.compose.foundation.BorderStroke(
                    width = 0.8.dp,
                    color = when {
                        isOtpExpired -> SystemRed.copy(alpha = 0.3f)
                        otpRemainingSeconds <= 10 -> SystemOrange.copy(alpha = 0.35f)
                        else -> SystemBlue.copy(alpha = 0.2f)
                    }
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = when {
                            isOtpExpired -> Icons.Outlined.ErrorOutline
                            otpRemainingSeconds <= 10 -> Icons.Outlined.HourglassBottom
                            else -> Icons.Outlined.Timer
                        },
                        contentDescription = "Timer",
                        tint = when {
                            isOtpExpired -> SystemRed
                            otpRemainingSeconds <= 10 -> SystemOrange
                            else -> SystemBlue
                        },
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = when {
                            isOtpExpired -> "Code expired. Tap 'Resend Code' below"
                            otpRemainingSeconds <= 10 -> "Expiring soon: $formattedCountdown"
                            else -> "Code expires in $formattedCountdown"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isOtpExpired -> SystemRed
                            otpRemainingSeconds <= 10 -> SystemOrange
                            else -> SystemBlue
                        }
                    )
                }
            }

            IosOtpInputView(
                otpCode = enteredOtp,
                onOtpChange = {
                    enteredOtp = it
                    if (isError) isError = false
                },
                length = 6,
                isError = isError,
                onComplete = { completedCode ->
                    verifyCodeAction(completedCode)
                }
            )

            // Animated Error Message
            AnimatedVisibility(
                visible = isError,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SystemRed,
                    textAlign = TextAlign.Center
                )
            }

            // Resend Code Countdown / Trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Didn't receive code?",
                    fontSize = 14.sp,
                    color = SystemLabelSecondary
                )

                val canResendNow = canResend || isOtpExpired
                if (canResendNow) {
                    Text(
                        text = "Resend Code",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemBlue,
                        modifier = Modifier.clickable {
                            HapticManager.performSuccess(context)
                            val newOtp = AuthManager.sendOtp(phoneNumber)
                            onResendRequested(newOtp)
                            enteredOtp = ""
                            isError = false
                            otpTimerKey++ // Resets countdown timer back to 60s
                            Toast.makeText(context, "New verification code dispatched", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Text(
                        text = "Resend in ${resendCountdown}s",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SystemLabelTertiary
                    )
                }
            }

            // Verify CTA Button
            Button(
                onClick = {
                    verifyCodeAction(enteredOtp)
                },
                enabled = enteredOtp.length == 6 && !isVerifying,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemBlue,
                    disabledContainerColor = SystemBlue.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isVerifying) "Verifying..." else "Verify & Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Bottom Inset Spacer
        Spacer(modifier = Modifier.height(16.dp))
    }
}
