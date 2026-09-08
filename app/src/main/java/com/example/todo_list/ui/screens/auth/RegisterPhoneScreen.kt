package com.example.todo_list.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.security.AuthManager
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager
import kotlinx.coroutines.launch

/**
 * Step 1 of Registration: Phone Number Input & OTP Request (Apple HIG).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPhoneScreen(
    onCodeSent: (phone: String, generatedOtp: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedCountryCode by remember { mutableStateOf("+91") }
    var phoneNumber by remember { mutableStateOf("") }
    var showCountryPicker by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var smsErrorDialogMessage by remember { mutableStateOf<String?>(null) }
    var pendingOtpToContinueWith by remember { mutableStateOf<String?>(null) }

    val isPhoneValid = remember(phoneNumber) {
        val digits = phoneNumber.filter { it.isDigit() }
        digits.length in 7..15
    }

    val fullPhoneNumber = remember(selectedCountryCode, phoneNumber) {
        "$selectedCountryCode ${phoneNumber.trim()}"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SystemGroupedBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: App Branding & Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        ) {
            // TaskFlow iOS App Icon Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SystemBlue)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "TaskFlow",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Create Account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter your phone number to receive a secure 6-digit verification code.",
                fontSize = 15.sp,
                color = SystemLabelSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Center Input Section: Inset Grouped Phone Field
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SystemSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Country Code Selector Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SystemGroupedBackground,
                        modifier = Modifier
                            .clickable {
                                HapticManager.performClick(context)
                                showCountryPicker = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = selectedCountryCode,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SystemLabelPrimary
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Select Country",
                                tint = SystemLabelSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Phone Number Input
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() || it == ' ' || it == '-' }
                            phoneNumber = clean
                        },
                        placeholder = {
                            Text(
                                text = "(555) 019-2834",
                                color = SystemLabelTertiary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Security Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = "End-to-end Encrypted",
                    tint = SystemBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Phone number is protected with end-to-end encryption.",
                    fontSize = 12.sp,
                    color = SystemLabelSecondary
                )
            }

            // Send Verification Code CTA Button
            Button(
                onClick = {
                    if (!isPhoneValid) {
                        HapticManager.performError(context)
                        Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    focusManager.clearFocus()
                    coroutineScope.launch {
                        isSending = true
                        val (generatedOtp, deliveryResult) = AuthManager.sendRealSmsOtp(fullPhoneNumber)
                        isSending = false
                        when (deliveryResult) {
                            is com.example.todo_list.security.SmsDeliveryResult.Success -> {
                                HapticManager.performSuccess(context)
                                Toast.makeText(context, "Real SMS sent to $fullPhoneNumber", Toast.LENGTH_LONG).show()
                                onCodeSent(fullPhoneNumber, generatedOtp)
                            }
                            is com.example.todo_list.security.SmsDeliveryResult.Failure -> {
                                HapticManager.performError(context)
                                pendingOtpToContinueWith = generatedOtp
                                smsErrorDialogMessage = deliveryResult.errorMessage
                            }
                        }
                    }
                },
                enabled = isPhoneValid && !isSending,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemBlue,
                    disabledContainerColor = SystemBlue.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSending) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Sending Real SMS...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = "Send Verification Code",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Bottom Section: Navigation to Login
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                fontSize = 14.sp,
                color = SystemLabelSecondary
            )
            Text(
                text = "Log In",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SystemBlue,
                modifier = Modifier.clickable {
                    HapticManager.performClick(context)
                    onNavigateToLogin()
                }
            )
        }
    }

    // Country Code Picker Modal Bottom Sheet
    if (showCountryPicker) {
        val countryCodes = listOf(
            "+91" to "India 🇮🇳",
            "+1" to "United States / Canada 🇺🇸",
            "+44" to "United Kingdom 🇬🇧",
            "+49" to "Germany 🇩🇪",
            "+33" to "France 🇫🇷",
            "+81" to "Japan 🇯🇵",
            "+61" to "Australia 🇦🇺",
            "+86" to "China 🇨🇳",
            "+55" to "Brazil 🇧🇷",
            "+971" to "United Arab Emirates 🇦🇪",
            "+65" to "Singapore 🇸🇬"
        )

        ModalBottomSheet(
            onDismissRequest = { showCountryPicker = false },
            containerColor = SystemSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Country Code",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )

                countryCodes.forEach { (code, label) ->
                    val isSelected = selectedCountryCode == code
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) SystemBlueLight else SystemGroupedBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticManager.performClick(context)
                                selectedCountryCode = code
                                showCountryPicker = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) SystemBlue else SystemLabelPrimary
                            )
                            Text(
                                text = code,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SystemBlue else SystemLabelSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Fast2SMS Gateway Status / Notice Dialog
    smsErrorDialogMessage?.let { errMsg ->
        val isRechargeNotice = errMsg.contains("100 INR", ignoreCase = true) || errMsg.contains("transaction", ignoreCase = true)
        AlertDialog(
            onDismissRequest = { smsErrorDialogMessage = null },
            title = {
                Text(
                    text = if (isRechargeNotice) "Fast2SMS Wallet Setup Required" else "Fast2SMS Gateway Notice",
                    fontWeight = FontWeight.Bold,
                    color = SystemRed,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = errMsg,
                        fontSize = 14.sp,
                        color = SystemLabelPrimary
                    )
                    if (isRechargeNotice) {
                        Text(
                            text = "Note: Fast2SMS API requires an initial transaction/recharge of ₹100 INR on fast2sms.com before real carrier SMS dispatch is unlocked on their Dev API route.",
                            fontSize = 13.sp,
                            color = SystemLabelSecondary
                        )
                    }
                }
            },
            confirmButton = {
                if (pendingOtpToContinueWith != null) {
                    TextButton(
                        onClick = {
                            val code = pendingOtpToContinueWith!!
                            smsErrorDialogMessage = null
                            pendingOtpToContinueWith = null
                            onCodeSent(fullPhoneNumber, code)
                        }
                    ) {
                        Text(
                            text = "Proceed to Verify",
                            color = SystemBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    TextButton(onClick = { smsErrorDialogMessage = null }) {
                        Text("OK", color = SystemBlue)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { smsErrorDialogMessage = null }) {
                    Text("Dismiss", color = SystemGray)
                }
            },
            containerColor = SystemSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
