package com.example.todo_list.ui.screens.lock

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.example.todo_list.security.AppLockManager
import com.example.todo_list.security.LockType
import com.example.todo_list.ui.components.lock.IosKeypad
import com.example.todo_list.ui.components.lock.PatternLockView
import com.example.todo_list.ui.components.lock.PinDotsView
import com.example.todo_list.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SetupStep {
    SELECT_LOCK_TYPE,
    ENTER_SECRET,
    CONFIRM_SECRET,
    VERIFY_CURRENT_FOR_CHANGE,
    VERIFY_CURRENT_FOR_DISABLE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSetupSheet(
    initialStep: SetupStep = SetupStep.SELECT_LOCK_TYPE,
    onDismiss: () -> Unit,
    onLockSetupComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var currentStep by remember { mutableStateOf(initialStep) }
    var selectedType by remember { mutableStateOf(LockType.PIN_4) }

    // Passcode states
    var firstSecret by remember { mutableStateOf("") }
    var secondSecret by remember { mutableStateOf("") }
    var verifySecret by remember { mutableStateOf("") }

    // Password visibility state
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Error states
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val handleSecretEntered = { entered: String ->
        when (currentStep) {
            SetupStep.ENTER_SECRET -> {
                firstSecret = entered
                currentStep = SetupStep.CONFIRM_SECRET
                secondSecret = ""
                isError = false
            }
            SetupStep.CONFIRM_SECRET -> {
                secondSecret = entered
                if (firstSecret == secondSecret) {
                    // Match! Save and enable lock automatically
                    AppLockManager.setLock(selectedType, firstSecret)
                    Toast.makeText(context, "${selectedType.displayName} enabled successfully", Toast.LENGTH_SHORT).show()
                    onLockSetupComplete()
                    onDismiss()
                } else {
                    isError = true
                    errorMessage = "${selectedType.displayName}s did not match. Try again."
                    coroutineScope.launch {
                        delay(600)
                        secondSecret = ""
                        isError = false
                    }
                }
            }
            SetupStep.VERIFY_CURRENT_FOR_CHANGE -> {
                if (AppLockManager.verifySecret(entered)) {
                    isError = false
                    verifySecret = ""
                    currentStep = SetupStep.SELECT_LOCK_TYPE
                } else {
                    isError = true
                    errorMessage = "Incorrect current passcode."
                    coroutineScope.launch {
                        delay(600)
                        verifySecret = ""
                        isError = false
                    }
                }
            }
            SetupStep.VERIFY_CURRENT_FOR_DISABLE -> {
                if (AppLockManager.verifySecret(entered)) {
                    AppLockManager.disableLock()
                    Toast.makeText(context, "App Lock turned off", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    isError = true
                    errorMessage = "Incorrect current passcode."
                    coroutineScope.launch {
                        delay(600)
                        verifySecret = ""
                        isError = false
                    }
                }
            }
            else -> Unit
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (currentStep) {
                        SetupStep.SELECT_LOCK_TYPE -> "Set Up App Lock"
                        SetupStep.ENTER_SECRET -> "Set ${selectedType.displayName}"
                        SetupStep.CONFIRM_SECRET -> "Confirm ${selectedType.displayName}"
                        SetupStep.VERIFY_CURRENT_FOR_CHANGE,
                        SetupStep.VERIFY_CURRENT_FOR_DISABLE -> "Verify Identity"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SystemGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "SetupStepAnimation"
            ) { step ->
                when (step) {
                    SetupStep.SELECT_LOCK_TYPE -> {
                        // Step 1: Select Lock Type
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Choose the type of security you want for TaskFlow:",
                                fontSize = 14.sp,
                                color = SystemLabelSecondary
                            )

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SystemGroupedBackground,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    LockType.entries.forEachIndexed { index, type ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    com.example.todo_list.utils.HapticManager.performClick(context)
                                                    selectedType = type
                                                    firstSecret = ""
                                                    secondSecret = ""
                                                    currentStep = SetupStep.ENTER_SECRET
                                                }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(SystemBlue.copy(alpha = 0.12f))
                                            ) {
                                                Icon(
                                                    imageVector = type.icon,
                                                    contentDescription = null,
                                                    tint = SystemBlue,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = type.displayName,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SystemLabelPrimary
                                                )
                                                Text(
                                                    text = type.subtitle,
                                                    fontSize = 12.sp,
                                                    color = SystemLabelSecondary
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = SystemGray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        if (index < LockType.entries.size - 1) {
                                            HorizontalDivider(
                                                color = SystemDivider,
                                                thickness = 0.5.dp,
                                                modifier = Modifier.padding(start = 70.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    SetupStep.ENTER_SECRET -> {
                        // Step 2: Enter Secret
                        SecretInputStepView(
                            lockType = selectedType,
                            instruction = when (selectedType) {
                                LockType.PIN_4 -> "Enter a 4-digit passcode"
                                LockType.PIN_6 -> "Enter a 6-digit passcode"
                                LockType.PATTERN -> "Draw an unlock pattern (connect at least 4 dots)"
                                LockType.PASSWORD -> "Enter an alphanumeric password"
                            },
                            currentSecret = firstSecret,
                            onSecretChange = { firstSecret = it },
                            onComplete = { handleSecretEntered(it) },
                            isError = isError,
                            errorMessage = errorMessage,
                            isPasswordVisible = isPasswordVisible,
                            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                            onCancel = { currentStep = SetupStep.SELECT_LOCK_TYPE }
                        )
                    }

                    SetupStep.CONFIRM_SECRET -> {
                        // Step 3: Confirm Secret
                        SecretInputStepView(
                            lockType = selectedType,
                            instruction = when (selectedType) {
                                LockType.PIN_4 -> "Re-enter your 4-digit passcode to confirm"
                                LockType.PIN_6 -> "Re-enter your 6-digit passcode to confirm"
                                LockType.PATTERN -> "Draw your pattern again to confirm"
                                LockType.PASSWORD -> "Re-enter your password to confirm"
                            },
                            currentSecret = secondSecret,
                            onSecretChange = { secondSecret = it },
                            onComplete = { handleSecretEntered(it) },
                            isError = isError,
                            errorMessage = errorMessage,
                            isPasswordVisible = isPasswordVisible,
                            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                            onCancel = {
                                firstSecret = ""
                                secondSecret = ""
                                currentStep = SetupStep.ENTER_SECRET
                            }
                        )
                    }

                    SetupStep.VERIFY_CURRENT_FOR_CHANGE,
                    SetupStep.VERIFY_CURRENT_FOR_DISABLE -> {
                        // Verify current lock
                        val activeType = AppLockManager.currentLockType
                        SecretInputStepView(
                            lockType = activeType,
                            instruction = "Enter your current ${activeType.displayName} to continue",
                            currentSecret = verifySecret,
                            onSecretChange = { verifySecret = it },
                            onComplete = { handleSecretEntered(it) },
                            isError = isError,
                            errorMessage = errorMessage,
                            isPasswordVisible = isPasswordVisible,
                            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                            onCancel = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecretInputStepView(
    lockType: LockType,
    instruction: String,
    currentSecret: String,
    onSecretChange: (String) -> Unit,
    onComplete: (String) -> Unit,
    isError: Boolean,
    errorMessage: String,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onCancel: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = instruction,
            fontSize = 14.sp,
            color = SystemLabelSecondary,
            modifier = Modifier.padding(bottom = 14.dp)
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
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        when (lockType) {
            LockType.PIN_4, LockType.PIN_6 -> {
                val maxDigits = if (lockType == LockType.PIN_4) 4 else 6

                PinDotsView(
                    totalDigits = maxDigits,
                    enteredCount = currentSecret.length,
                    isError = isError,
                    accentColor = SystemBlue
                )

                Spacer(modifier = Modifier.height(16.dp))

                IosKeypad(
                    onDigitClick = { digit ->
                        if (currentSecret.length < maxDigits && !isError) {
                            val next = currentSecret + digit
                            onSecretChange(next)
                            if (next.length == maxDigits) {
                                onComplete(next)
                            }
                        }
                    },
                    onDeleteClick = {
                        if (currentSecret.isNotEmpty() && !isError) {
                            onSecretChange(currentSecret.dropLast(1))
                        }
                    },
                    onCancelClick = onCancel
                )
            }

            LockType.PATTERN -> {
                PatternLockView(
                    onPatternComplete = { patternStr ->
                        onSecretChange(patternStr)
                        onComplete(patternStr)
                    },
                    isError = isError,
                    accentColor = SystemBlue
                )
            }

            LockType.PASSWORD -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = currentSecret,
                        onValueChange = onSecretChange,
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
                                if (currentSecret.isNotEmpty()) {
                                    onComplete(currentSecret)
                                }
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = onTogglePasswordVisibility) {
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
                            errorBorderColor = SystemRed
                        ),
                        isError = isError,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Back", color = SystemLabelPrimary)
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (currentSecret.isNotEmpty()) {
                                    onComplete(currentSecret)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = currentSecret.length >= 4
                        ) {
                            Text("Continue", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
