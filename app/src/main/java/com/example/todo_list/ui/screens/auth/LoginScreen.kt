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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.todo_list.data.local.entity.UserEntity
import com.example.todo_list.model.AvatarPreset
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
    autofillCode: String? = null,
    onAutofillConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var savedAccounts by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var selectedAccount by remember { mutableStateOf<UserEntity?>(null) }
    var isManualEntryMode by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<UserEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var identifier by remember { mutableStateOf(AuthManager.registeredUsername.ifEmpty { "" }) }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    var showForgotPasswordSheet by remember { mutableStateOf(false) }

    // Load saved accounts from local SQLite database
    LaunchedEffect(Unit) {
        val accounts = AuthManager.getSavedAccounts()
        savedAccounts = accounts
        if (accounts.isNotEmpty()) {
            val defaultUser = accounts.find { it.username.equals(AuthManager.registeredUsername, ignoreCase = true) }
                ?: accounts.first()
            selectedAccount = defaultUser
            identifier = defaultUser.username
            isManualEntryMode = false
        } else {
            isManualEntryMode = true
        }
    }

    val isFormValid = remember(identifier, password) {
        identifier.trim().isNotEmpty() && password.length >= 4
    }

    // Biometric Login for a specific selected user account
    val triggerAccountBiometricLogin: (UserEntity) -> Unit = { targetUser ->
        if (activity != null) {
            val status = BiometricAuthHelper.checkBiometricAvailability(context)
            if (status == BiometricAvailability.AVAILABLE) {
                val bioName = BiometricAuthHelper.getBiometricDisplayName(context)
                BiometricAuthHelper.promptBiometric(
                    activity = activity,
                    title = "Sign In to TaskFlow",
                    subtitle = "Confirm biometric identity for ${targetUser.name}",
                    negativeButtonText = "Use Password",
                    onSuccess = {
                        coroutineScope.launch {
                            val success = AuthManager.loginWithBiometrics(targetUser)
                            if (success) {
                                HapticManager.performSuccess(context)
                                Toast.makeText(context, "Welcome back, ${targetUser.name}! 👋", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                HapticManager.performError(context)
                                Toast.makeText(context, "Failed to restore session. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onError = { _ -> }
                )
            }
        }
    }

    // Password login for a specific selected user account
    val triggerAccountPasswordLogin: (UserEntity) -> Unit = { targetUser ->
        if (password.length >= 4 && !isLoggingIn) {
            isLoggingIn = true
            focusManager.clearFocus()

            coroutineScope.launch {
                val success = AuthManager.loginUserWithPassword(targetUser, password)
                if (success) {
                    HapticManager.performSuccess(context)
                    isError = false
                    isLoggingIn = false
                    Toast.makeText(context, "Welcome back, ${targetUser.name}! 👋", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    HapticManager.performError(context)
                    isError = true
                    errorMessage = "Incorrect password for ${targetUser.name}. Please try again."
                    isLoggingIn = false
                }
            }
        }
    }

    // Perform login with given credentials (manual or demo accounts)
    val performLoginWithCredentials: (String, String) -> Unit = { id, pw ->
        if (!isLoggingIn) {
            isLoggingIn = true
            focusManager.clearFocus()

            coroutineScope.launch {
                val success = AuthManager.login(id, pw)
                if (success) {
                    HapticManager.performSuccess(context)
                    isError = false
                    isLoggingIn = false
                    Toast.makeText(context, "Welcome back, ${AuthManager.registeredName.ifEmpty { "User" }}! 👋", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    HapticManager.performError(context)
                    isError = true
                    val exists = AuthManager.userExists(id)
                    errorMessage = if (!exists) {
                        "No registered account found with \"${id.trim()}\". Please check or create an account."
                    } else {
                        "Incorrect password. Please verify your credentials and try again."
                    }
                    isLoggingIn = false
                }
            }
        }
    }

    // Manual login (username/phone + password)
    val triggerManualLogin = {
        if (isFormValid && !isLoggingIn) {
            performLoginWithCredentials(identifier, password)
        }
    }

    // Handle system back button for sheets and dialogs
    BackHandler(enabled = true) {
        when {
            showDeleteConfirmDialog -> showDeleteConfirmDialog = false
            showForgotPasswordSheet -> showForgotPasswordSheet = false
            else -> {
                // Allow exiting app on login screen
                (context as? androidx.activity.ComponentActivity)?.onBackPressed()
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
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SystemBlue)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "TaskFlow",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome Back",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = SystemLabelPrimary,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (savedAccounts.isNotEmpty() && !isManualEntryMode) {
                    "Select an account or verify your biometric identity to continue."
                } else {
                    "Sign in with your username or phone number to access your workspace."
                },
                fontSize = 14.sp,
                color = SystemLabelSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Main Body: Account Switcher vs Manual Entry Form
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (savedAccounts.isNotEmpty() && !isManualEntryMode) {
                // Multi-Account Switcher Cards
                Text(
                    text = if (savedAccounts.size > 1) "SAVED ACCOUNTS" else "ACTIVE ACCOUNT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SystemSurface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        savedAccounts.forEachIndexed { index, account ->
                            val isSelected = selectedAccount?.id == account.id
                            val avatarPreset = remember(account.avatarPresetId) {
                                AvatarPreset.getById(account.avatarPresetId)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        HapticManager.performClick(context)
                                        selectedAccount = account
                                        identifier = account.username
                                        password = ""
                                        isError = false
                                    }
                                    .background(
                                        if (isSelected) SystemBlue.copy(alpha = 0.08f) else Color.Transparent
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar Circle
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(avatarPreset.gradientStartHex),
                                                    Color(avatarPreset.gradientEndHex)
                                                )
                                            )
                                        )
                                ) {
                                    Text(
                                        text = avatarPreset.emoji,
                                        fontSize = 20.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = account.name,
                                            fontSize = 16.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = SystemLabelPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (account.isBiometricEnabled) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = BiometricAuthHelper.getBiometricIcon(context),
                                                contentDescription = "Biometrics Enabled",
                                                tint = AppleHealth,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = account.username,
                                        fontSize = 13.sp,
                                        color = SystemLabelSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Selection checkmark or delete action
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = SystemBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        accountToDelete = account
                                        showDeleteConfirmDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Remove Account",
                                        tint = SystemGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (index < savedAccounts.size - 1) {
                                HorizontalDivider(
                                    color = SystemDivider,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 70.dp)
                                )
                            }
                        }
                    }
                }

                // Selected Account Actions (Biometrics or Password)
                selectedAccount?.let { account ->
                    val isBioAvailable = BiometricAuthHelper.checkBiometricAvailability(context) == BiometricAvailability.AVAILABLE
                    val showBiometricButton = account.isBiometricEnabled && isBioAvailable

                    if (showBiometricButton) {
                        val bioName = BiometricAuthHelper.getBiometricDisplayName(context)
                        val bioIcon = BiometricAuthHelper.getBiometricIcon(context)

                        Button(
                            onClick = { triggerAccountBiometricLogin(account) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = bioIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign In as ${account.name} with $bioName",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SystemDivider, thickness = 0.5.dp)
                            Text(
                                text = "OR ENTER PASSWORD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SystemLabelSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SystemDivider, thickness = 0.5.dp)
                        }
                    }

                    // Password Card for Selected Account
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
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    isError = false
                                },
                                label = { Text("Password for ${account.name}") },
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
                                    onDone = { triggerAccountPasswordLogin(account) }
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

                    // Password Sign In Button
                    Button(
                        onClick = { triggerAccountPasswordLogin(account) },
                        enabled = password.length >= 4 && !isLoggingIn,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showBiometricButton) SystemSurface else SystemBlue,
                            contentColor = if (showBiometricButton) SystemBlue else Color.White
                        ),
                        border = if (showBiometricButton) androidx.compose.foundation.BorderStroke(1.dp, SystemBlue) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = if (isLoggingIn) "Signing In..." else "Sign In with Password",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Switch to Manual Login
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Use Another Account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SystemBlue,
                        modifier = Modifier.clickable {
                            HapticManager.performClick(context)
                            isManualEntryMode = true
                            password = ""
                            isError = false
                        }
                    )

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
            } else {
                // Manual Entry Mode (Username/Phone + Password)
                if (savedAccounts.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                HapticManager.performClick(context)
                                isManualEntryMode = false
                                isError = false
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = SystemBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Back to Saved Accounts",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SystemBlue
                        )
                    }
                }

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
                                onDone = { triggerManualLogin() }
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

                // Sign In Button
                Button(
                    onClick = triggerManualLogin,
                    enabled = isFormValid && !isLoggingIn,
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
                        text = if (isLoggingIn) "Signing In..." else "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Cloud Demo Quick Login
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "CLOUD DEMO ACCOUNTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SystemLabelSecondary,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("Alex", "@alexcarter", "password123"),
                            Triple("Sarah", "@sarahc", "password123"),
                            Triple("David", "@davidm", "password123")
                        ).forEach { (name, user, pw) ->
                            OutlinedButton(
                                onClick = {
                                    identifier = user
                                    password = pw
                                    performLoginWithCredentials(user, pw)
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SystemBlue.copy(alpha = 0.35f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SystemSurface,
                                    contentColor = SystemBlue
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Text(
                                    text = "👤 $name",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Error prompt for selected account
            if (savedAccounts.isNotEmpty() && !isManualEntryMode) {
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

    // Account Deletion Confirmation Dialog
    if (showDeleteConfirmDialog && accountToDelete != null) {
        val target = accountToDelete!!
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                accountToDelete = null
            },
            title = {
                Text(
                    text = "Remove Account?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SystemLabelPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${target.name} (${target.username}) from this device? All local tasks and settings for this account will be permanently removed.",
                    fontSize = 14.sp,
                    color = SystemLabelSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            AuthManager.removeAccountLocally(target.id)
                            val remaining = AuthManager.getSavedAccounts()
                            savedAccounts = remaining
                            if (remaining.isNotEmpty()) {
                                selectedAccount = remaining.first()
                                identifier = remaining.first().username
                            } else {
                                selectedAccount = null
                                identifier = ""
                                isManualEntryMode = true
                            }
                            showDeleteConfirmDialog = false
                            accountToDelete = null
                            Toast.makeText(context, "Account removed from device", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Remove", color = SystemRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        accountToDelete = null
                    }
                ) {
                    Text("Cancel", color = SystemBlue)
                }
            },
            containerColor = SystemSurface,
            shape = RoundedCornerShape(16.dp)
        )
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
            },
            autofillCode = autofillCode,
            onAutofillConsumed = onAutofillConsumed
        )
    }
}
