package com.example.todo_list.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.security.AuthManager
import com.example.todo_list.ui.components.auth.IosOtpInputView
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager
import kotlinx.coroutines.launch

/**
 * Password Recovery Bottom Sheet via Phone OTP (Apple HIG).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordSheet(
    onDismiss: () -> Unit,
    onPasswordResetComplete: () -> Unit,
    onOtpDispatched: (code: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Step 0: Phone, Step 1: OTP, Step 2: New Password
    var step by remember { mutableIntStateOf(0) }
    var phoneInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isOtpError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = SystemDivider) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (step) {
                        0 -> "Reset Password"
                        1 -> "Verify Security Code"
                        else -> "New Password"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = SystemBlue, fontWeight = FontWeight.SemiBold)
                }
            }

            when (step) {
                0 -> {
                    Text(
                        text = "Enter your registered phone number to receive a 6-digit verification code.",
                        fontSize = 14.sp,
                        color = SystemLabelSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SystemGroupedBackground,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            placeholder = { Text("Registered Phone (e.g. +1 555 382 9012)") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Outlined.Phone, contentDescription = "Phone", tint = SystemBlue)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            if (phoneInput.trim().length < 7) {
                                HapticManager.performError(context)
                                Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val isRegistered = AuthManager.isRegisteredPhone(phoneInput)
                            if (!isRegistered) {
                                HapticManager.performError(context)
                                Toast.makeText(context, "This phone number is not linked to any account", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            focusManager.clearFocus()
                            coroutineScope.launch {
                                isSending = true
                                val (otp, deliveryResult) = AuthManager.sendRealSmsOtp(phoneInput)
                                isSending = false
                                onOtpDispatched(otp)
                                HapticManager.performSuccess(context)
                                step = 1
                                when (deliveryResult) {
                                    is com.example.todo_list.security.SmsDeliveryResult.Success -> {
                                        Toast.makeText(context, "Real SMS sent to $phoneInput", Toast.LENGTH_SHORT).show()
                                    }
                                    is com.example.todo_list.security.SmsDeliveryResult.Failure -> {
                                        Toast.makeText(context, "Fast2SMS: ${deliveryResult.errorMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isSending,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Send Reset Code", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                1 -> {
                    Text(
                        text = "Enter the 6-digit verification code sent to your phone.",
                        fontSize = 14.sp,
                        color = SystemLabelSecondary
                    )

                    IosOtpInputView(
                        otpCode = otpInput,
                        onOtpChange = {
                            otpInput = it
                            isOtpError = false
                        },
                        length = 6,
                        isError = isOtpError,
                        onComplete = { code ->
                            if (AuthManager.verifyOtp(code)) {
                                HapticManager.performSuccess(context)
                                step = 2
                            } else {
                                HapticManager.performError(context)
                                isOtpError = true
                                Toast.makeText(context, "Invalid code. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Button(
                        onClick = {
                            if (AuthManager.verifyOtp(otpInput)) {
                                HapticManager.performSuccess(context)
                                step = 2
                            } else {
                                HapticManager.performError(context)
                                isOtpError = true
                                Toast.makeText(context, "Invalid code. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = otpInput.length == 6,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Verify Code", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                2 -> {
                    Text(
                        text = "Create a new secure password for your TaskFlow account.",
                        fontSize = 14.sp,
                        color = SystemLabelSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SystemGroupedBackground,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("New Password") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Outlined.Lock, contentDescription = "Password", tint = SystemBlue)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isNewPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = "Toggle",
                                            tint = SystemGray
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(color = SystemDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 40.dp))

                            OutlinedTextField(
                                value = confirmNewPassword,
                                onValueChange = { confirmNewPassword = it },
                                label = { Text("Confirm New Password") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Outlined.Key, contentDescription = "Confirm", tint = SystemBlue)
                                },
                                singleLine = true,
                                visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (newPassword.length < 6) {
                                HapticManager.performError(context)
                                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (newPassword != confirmNewPassword) {
                                HapticManager.performError(context)
                                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            AuthManager.resetPassword(newPassword)
                            HapticManager.performSuccess(context)
                            Toast.makeText(context, "Password reset successfully! You can now log in.", Toast.LENGTH_LONG).show()
                            onPasswordResetComplete()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Update Password", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
