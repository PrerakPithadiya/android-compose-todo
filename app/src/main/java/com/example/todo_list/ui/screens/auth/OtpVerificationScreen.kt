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
import androidx.compose.material.icons.outlined.Sms
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

    // 30-Second Countdown Timer for Resend OTP
    var resendCountdown by remember { mutableIntStateOf(30) }
    var canResend by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (resendCountdown > 0) {
            delay(1000)
            resendCountdown--
        }
        canResend = true
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
            if (AuthManager.verifyOtp(codeToVerify)) {
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
                    delay(1200)
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

        // Center Input Section: 6 Digit Boxes + Error
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
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

                if (canResend) {
                    Text(
                        text = "Resend Code",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemBlue,
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                HapticManager.performSuccess(context)
                                val (newOtp, deliveryResult) = AuthManager.sendRealSmsOtp(phoneNumber)
                                onResendRequested(newOtp)
                                resendCountdown = 30
                                canResend = false
                                when (deliveryResult) {
                                    is com.example.todo_list.security.SmsDeliveryResult.Success -> {
                                        Toast.makeText(context, "New SMS dispatched to $phoneNumber", Toast.LENGTH_SHORT).show()
                                    }
                                    is com.example.todo_list.security.SmsDeliveryResult.Failure -> {
                                        Toast.makeText(context, "Fast2SMS: ${deliveryResult.errorMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                                while (resendCountdown > 0) {
                                    delay(1000)
                                    resendCountdown--
                                }
                                canResend = true
                            }
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
