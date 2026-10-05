package com.example.todo_list.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
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
import com.example.todo_list.manager.UserProfileManager
import com.example.todo_list.security.AuthManager
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager
import kotlinx.coroutines.launch

/**
 * Step 3 of Registration: Setting up Full Name, Username & Password (Apple HIG).
 */
@Composable
fun AccountSetupScreen(
    verifiedPhone: String,
    onRegistrationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val cleanUsername = remember(username) {
        val trimmed = username.trim()
        if (trimmed.startsWith("@")) trimmed else if (trimmed.isNotEmpty()) "@$trimmed" else ""
    }

    val hasMinLength = password.length >= 8
    val hasNumber = password.any { it.isDigit() }
    val hasSpecialOrUpper = password.any { !it.isLetterOrDigit() || it.isUpperCase() }
    val passwordsMatch = password.isNotEmpty() && password == confirmPassword
    val isPasswordValid = hasMinLength && hasNumber && hasSpecialOrUpper && passwordsMatch

    val isFormValid = remember(name, username, isPasswordValid) {
        name.trim().length >= 2 && username.trim().length >= 3 && isPasswordValid
    }

    // Password strength score (0 to 4)
    val strengthScore = remember(password) {
        var score = 0
        if (hasMinLength) score++
        if (hasNumber) score++
        if (hasSpecialOrUpper) score++
        if (password.length >= 12) score++
        score
    }

    // Handle system back button - prevent exiting during account setup
    BackHandler(enabled = true) {
        // Don't allow back button during account setup
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
        // Header Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SystemBlue.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Badge,
                    contentDescription = "Profile Setup",
                    tint = SystemBlue,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Set Up Profile",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Choose your username and password for future logins.",
                fontSize = 15.sp,
                color = SystemLabelSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Center Inset Grouped Inputs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Inset Group 1: Identity (Name & Username)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SystemSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("e.g. Alex Johnson") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Person, contentDescription = "Name", tint = SystemBlue)
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

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '.' || ch == '@' } },
                        label = { Text("Username") },
                        placeholder = { Text("@alexj") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.AlternateEmail, contentDescription = "Username", tint = SystemBlue)
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
                }
            }

            // Inset Group 2: Security (Password & Confirm)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SystemSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (min 8 characters)") },
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

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Key, contentDescription = "Confirm", tint = SystemBlue)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isConfirmVisible = !isConfirmVisible }) {
                                Icon(
                                    imageVector = if (isConfirmVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = SystemGray
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (isConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Live Password Strength Bar
            if (password.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Password Strength",
                            fontSize = 12.sp,
                            color = SystemLabelSecondary
                        )
                        Text(
                            text = when (strengthScore) {
                                0, 1 -> "Weak"
                                2 -> "Fair"
                                3 -> "Good"
                                else -> "Strong"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (strengthScore) {
                                0, 1 -> SystemRed
                                2 -> SystemOrange
                                3 -> SystemBlue
                                else -> SystemGreen
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 1..4) {
                            val isFilled = i <= strengthScore
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (isFilled) {
                                            when (strengthScore) {
                                                1 -> SystemRed
                                                2 -> SystemOrange
                                                3 -> SystemBlue
                                                else -> SystemGreen
                                            }
                                        } else {
                                            SystemDivider
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            // 4 Strict Password Rules Checklist
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AccountSetupRequirementItem(label = "At least 8 characters", isMet = hasMinLength)
                AccountSetupRequirementItem(label = "Contains at least 1 number", isMet = hasNumber)
                AccountSetupRequirementItem(label = "Contains uppercase letter or symbol", isMet = hasSpecialOrUpper)
                AccountSetupRequirementItem(label = "Passwords match", isMet = passwordsMatch)
            }
        }

        // Bottom Actions Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Complete Registration CTA Button
            Button(
                onClick = {
                    if (!isFormValid) {
                        HapticManager.performError(context)
                        Toast.makeText(context, "Please fill all fields accurately", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSubmitting = true
                    focusManager.clearFocus()

                    coroutineScope.launch {
                        val success = AuthManager.registerAccount(
                            name = name,
                            username = cleanUsername,
                            phone = verifiedPhone,
                            password = password
                        )

                        if (success) {
                            HapticManager.performSuccess(context)
                            Toast.makeText(context, "Account created successfully! Welcome to TaskFlow 🎉", Toast.LENGTH_LONG).show()
                            onRegistrationComplete()
                        } else {
                            HapticManager.performError(context)
                            Toast.makeText(context, "Username or phone number already in use. Please try again.", Toast.LENGTH_SHORT).show()
                            isSubmitting = false
                        }
                    }
                },
                enabled = isFormValid && !isSubmitting,
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
                    text = if (isSubmitting) "Creating Account..." else "Complete Registration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Bottom Security Note
            Text(
                text = "By continuing, you agree to TaskFlow's Terms of Service and Privacy Policy.",
                fontSize = 12.sp,
                color = SystemLabelTertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun AccountSetupRequirementItem(label: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Filled.Check else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (isMet) SystemGreen else SystemLabelSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isMet) SystemGreen else SystemLabelSecondary
        )
    }
}
