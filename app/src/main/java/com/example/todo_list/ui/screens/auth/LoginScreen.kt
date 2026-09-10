package com.example.todo_list.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.material3.ripple
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
import androidx.fragment.app.FragmentActivity
import com.example.todo_list.security.AppLockManager
import com.example.todo_list.security.AuthManager
import com.example.todo_list.security.BiometricAuthHelper
import com.example.todo_list.security.BiometricAvailability
import com.example.todo_list.ui.theme.*
import kotlinx.coroutines.launch
import com.example.todo_list.utils.HapticManager

/**
 * Authentic Apple iOS Login Screen (Username/Phone & Password).
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onForgotPasswordOtpDispatched: (code: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var identifier by remember { mutableStateOf(AuthManager.registeredUsername.ifEmpty { "" }) }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    var showForgotPasswordSheet by remember { mutableStateOf(false) }

    val isFormValid = remember(identifier, password) {
        identifier.trim().isNotEmpty() && password.length >= 4
    }

    val triggerLogin = {
        if (isFormValid && !isLoggingIn) {
            isLoggingIn = true
            focusManager.clearFocus()

            coroutineScope.launch {
                val success = AuthManager.login(identifier, password)
                if (success) {
                    HapticManager.performSuccess(context)
                    isError = false
                    isLoggingIn = false
                    Toast.makeText(context, "Welcome back, ${AuthManager.registeredName.ifEmpty { "User" }}! 👋", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    HapticManager.performError(context)
                    isError = true
                    errorMessage = "Incorrect username, phone, or password. Please try again."
                    isLoggingIn = false
                }
            }
        }
    }

    val triggerBiometricLogin: () -> Unit = {
        if (activity != null) {
            val status = BiometricAuthHelper.checkBiometricAvailability(context)
            if (status == BiometricAvailability.AVAILABLE) {
                val bioName = BiometricAuthHelper.getBiometricDisplayName(context)
                BiometricAuthHelper.promptBiometric(
                    activity = activity,
                    title = "Sign In to TaskFlow",
                    subtitle = BiometricAuthHelper.getBiometricPromptSubtitle(context),
                    negativeButtonText = "Use Password",
                    onSuccess = {
                        HapticManager.performSuccess(context)
                        Toast.makeText(context, "$bioName sign-in successful! Welcome back 🎉", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    onError = { _ -> }
                )
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
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header & Logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp)
        ) {
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
                text = "Welcome Back",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Sign in with your username or phone number to access your productivity workspace.",
                fontSize = 15.sp,
                color = SystemLabelSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Center Input Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Inset Grouped Credentials Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SystemSurface,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isError) 1.dp else 0.5.dp,
                    color = if (isError) SystemRed else SystemDivider
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                    // Identifier Field
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = {
                            identifier = it
                            isError = false
                        },
                        label = { Text("Username or Phone") },
                        placeholder = { Text("@username or +1...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Person, contentDescription = "User", tint = SystemBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = SystemDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 40.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            isError = false
                        },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Lock, contentDescription = "Password", tint = SystemBlue)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = SystemGray
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                triggerLogin()
                            }
                        ),
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

            // Error Prompt
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
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }

            // Forgot Password Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Forgot Password?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SystemBlue,
                    modifier = Modifier.clickable {
                        HapticManager.performClick(context)
                        showForgotPasswordSheet = true
                    }
                )
            }

            // Action Buttons (Sign In + Optional Biometrics)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (AppLockManager.isBiometricEnabled && AuthManager.isAccountCreated) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SystemSurface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = SystemBlue.copy(alpha = 0.2f)),
                                onClick = triggerBiometricLogin
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = BiometricAuthHelper.getBiometricIcon(context),
                                contentDescription = "Sign In with ${BiometricAuthHelper.getBiometricDisplayName(context)}",
                                tint = SystemBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = triggerLogin,
                    enabled = isFormValid && !isLoggingIn,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SystemBlue,
                        disabledContainerColor = SystemBlue.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = if (isLoggingIn) "Signing In..." else "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Bottom Section: Navigation to Registration
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                fontSize = 14.sp,
                color = SystemLabelSecondary
            )
            Text(
                text = "Sign Up",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SystemBlue,
                modifier = Modifier.clickable {
                    HapticManager.performClick(context)
                    onNavigateToRegister()
                }
            )
        }
    }

    // Forgot Password Bottom Sheet
    if (showForgotPasswordSheet) {
        ForgotPasswordSheet(
            onDismiss = { showForgotPasswordSheet = false },
            onPasswordResetComplete = {
                showForgotPasswordSheet = false
            },
            onOtpDispatched = { otp ->
                onForgotPasswordOtpDispatched(otp)
            }
        )
    }
}
