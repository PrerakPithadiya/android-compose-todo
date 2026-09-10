package com.example.todo_list.ui.screens.lock

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Fingerprint
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.todo_list.security.AppLockManager
import com.example.todo_list.security.BiometricAuthHelper
import com.example.todo_list.security.LockType
import com.example.todo_list.ui.components.lock.IosKeypad
import com.example.todo_list.ui.components.lock.PatternLockView
import com.example.todo_list.ui.components.lock.PinDotsView
import com.example.todo_list.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppLockAuthScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    // Intercept hardware/gesture back press to prevent bypassing lock screen
    BackHandler(enabled = true) {
        // App remains locked
    }

    val lockType = AppLockManager.currentLockType
    var enteredPin by remember { mutableStateOf("") }
    var enteredPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var attemptCount by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val biometricSubtitle = remember(context) {
        BiometricAuthHelper.getBiometricPromptSubtitle(context)
    }
    val biometricIcon = remember(context) {
        BiometricAuthHelper.getBiometricIcon(context)
    }
    val biometricDisplayName = remember(context) {
        BiometricAuthHelper.getBiometricDisplayName(context)
    }

    val triggerBiometricPrompt: () -> Unit = remember(activity, biometricSubtitle) {
        {
            if (activity != null && AppLockManager.isBiometricEnabled) {
                BiometricAuthHelper.promptBiometric(
                    activity = activity,
                    title = "Unlock TaskFlow",
                    subtitle = biometricSubtitle,
                    negativeButtonText = "Use Passcode",
                    onSuccess = {
                        AppLockManager.unlock()
                    },
                    onError = { _ ->
                        // Fallback to manual passcode entry
                    }
                )
            }
        }
    }

    // Direct launch: Trigger fingerprint authentication immediately upon opening / resuming
    DisposableEffect(lifecycleOwner, AppLockManager.isBiometricEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && AppLockManager.isBiometricEnabled) {
                triggerBiometricPrompt()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(AppLockManager.isBiometricEnabled) {
        if (AppLockManager.isBiometricEnabled) {
            delay(100)
            triggerBiometricPrompt()
        }
    }

    val onValidateSecret = { secretToVerify: String ->
        if (AppLockManager.verifySecret(secretToVerify)) {
            com.example.todo_list.utils.HapticManager.performSuccess(context)
            isError = false
            enteredPin = ""
            enteredPassword = ""
            AppLockManager.unlock()
        } else {
            com.example.todo_list.utils.HapticManager.performError(context)
            isError = true
            attemptCount++
            errorMessage = "Incorrect ${lockType.displayName}. Try again."
            coroutineScope.launch {
                delay(600)
                enteredPin = ""
                isError = false
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SystemGroupedBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SystemBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = SystemBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "TaskFlow Locked",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (lockType) {
                        LockType.PIN_4 -> "Enter 4-Digit Passcode"
                        LockType.PIN_6 -> "Enter 6-Digit Passcode"
                        LockType.PATTERN -> "Draw your Pattern to Unlock"
                        LockType.PASSWORD -> "Enter your Password to Unlock"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = SystemLabelSecondary
                )

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
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }

            // Authentication Input Section according to LockType
            when (lockType) {
                LockType.PIN_4, LockType.PIN_6 -> {
                    val maxDigits = if (lockType == LockType.PIN_4) 4 else 6

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PinDotsView(
                            totalDigits = maxDigits,
                            enteredCount = enteredPin.length,
                            isError = isError,
                            accentColor = SystemBlue
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        IosKeypad(
                            onDigitClick = { digit ->
                                if (enteredPin.length < maxDigits && !isError) {
                                    val next = enteredPin + digit
                                    enteredPin = next
                                    if (next.length == maxDigits) {
                                        onValidateSecret(next)
                                    }
                                }
                            },
                            onDeleteClick = {
                                if (enteredPin.isNotEmpty() && !isError) {
                                    enteredPin = enteredPin.dropLast(1)
                                }
                            },
                            onBiometricClick = if (AppLockManager.isBiometricEnabled) triggerBiometricPrompt else null,
                            biometricIcon = biometricIcon,
                            biometricContentDescription = "Unlock with $biometricDisplayName",
                            onCancelClick = null
                        )
                    }
                }

                LockType.PATTERN -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PatternLockView(
                            onPatternComplete = { patternStr ->
                                onValidateSecret(patternStr)
                            },
                            isError = isError,
                            accentColor = SystemBlue
                        )

                        if (AppLockManager.isBiometricEnabled) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(SystemBlue.copy(alpha = 0.12f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = SystemBlue.copy(alpha = 0.3f)),
                                        onClick = triggerBiometricPrompt
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = biometricIcon,
                                    contentDescription = "Unlock with $biometricDisplayName",
                                    tint = SystemBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                LockType.PASSWORD -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = enteredPassword,
                            onValueChange = {
                                enteredPassword = it
                                isError = false
                            },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (enteredPassword.isNotEmpty()) {
                                        onValidateSecret(enteredPassword)
                                    }
                                }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        tint = SystemGray
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SystemBlue,
                                unfocusedBorderColor = SystemDivider,
                                errorBorderColor = SystemRed,
                                focusedContainerColor = SystemSurface,
                                unfocusedContainerColor = SystemSurface
                            ),
                            isError = isError,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (AppLockManager.isBiometricEnabled) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SystemBlue.copy(alpha = 0.12f))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true, color = SystemBlue.copy(alpha = 0.3f)),
                                            onClick = triggerBiometricPrompt
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = biometricIcon,
                                        contentDescription = "Unlock with $biometricDisplayName",
                                        tint = SystemBlue,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (enteredPassword.isNotEmpty()) {
                                        onValidateSecret(enteredPassword)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = enteredPassword.isNotEmpty()
                            ) {
                                Text(
                                    text = "Unlock App",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Safe Inset Spacer
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
