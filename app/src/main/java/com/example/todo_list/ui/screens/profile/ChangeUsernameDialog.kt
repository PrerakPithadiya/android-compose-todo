package com.example.todo_list.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.todo_list.manager.UserProfileManager
import com.example.todo_list.security.AuthManager
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager

/**
 * Apple iOS HIG Change Username Dialog with real-time validation and format checking.
 */
@Composable
fun ChangeUsernameDialog(
    currentUsername: String,
    onDismiss: () -> Unit,
    onUsernameChanged: (newUsername: String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val initialClean = currentUsername.removePrefix("@").trim()
    var usernameInput by remember { mutableStateOf(initialClean) }

    val cleanCandidate = usernameInput.trim().removePrefix("@")
    val isValidFormat = cleanCandidate.length >= 3 &&
            cleanCandidate.length <= 25 &&
            cleanCandidate.all { it.isLetterOrDigit() || it == '_' || it == '.' }
    val isChanged = cleanCandidate.isNotEmpty() && cleanCandidate != initialClean

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SystemSurface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Change Username",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SystemLabelPrimary
                        )
                        Text(
                            text = "Choose your unique TaskFlow handle",
                            fontSize = 13.sp,
                            color = SystemLabelSecondary
                        )
                    }

                    IconButton(
                        onClick = {
                            HapticManager.performClick(context)
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = SystemLabelSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = SystemDivider, thickness = 0.5.dp)

                // Input Field
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { input ->
                        usernameInput = input.filter { !it.isWhitespace() }
                    },
                    label = { Text("Username Handle") },
                    placeholder = { Text("e.g. johndoe") },
                    prefix = {
                        Text(
                            text = "@",
                            fontWeight = FontWeight.Bold,
                            color = SystemBlue,
                            fontSize = 16.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AlternateEmail,
                            contentDescription = "Username",
                            tint = SystemBlue
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemBlue,
                        unfocusedBorderColor = SystemDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Live Validation Hint
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusText = when {
                        cleanCandidate.isEmpty() -> "Enter at least 3 characters"
                        cleanCandidate.length < 3 -> "Username must be at least 3 characters long"
                        !cleanCandidate.all { it.isLetterOrDigit() || it == '_' || it == '.' } -> "Only letters, numbers, dots, and underscores allowed"
                        !isChanged -> "This is your current active username"
                        else -> "✓ Valid username format (@$cleanCandidate)"
                    }
                    val statusColor = when {
                        isValidFormat && isChanged -> SystemGreen
                        cleanCandidate.isEmpty() || !isChanged -> SystemLabelSecondary
                        else -> SystemOrange
                    }

                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = if (isValidFormat && isChanged) FontWeight.SemiBold else FontWeight.Normal,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            HapticManager.performClick(context)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = SystemLabelPrimary
                        )
                    }

                    Button(
                        enabled = isValidFormat && isChanged,
                        onClick = {
                            val formattedUsername = "@$cleanCandidate"
                            HapticManager.performSuccess(context)
                            UserProfileManager.updateProfile(
                                name = UserProfileManager.profile.name,
                                username = formattedUsername,
                                email = UserProfileManager.profile.email,
                                phone = UserProfileManager.profile.phone,
                                bio = UserProfileManager.profile.bio
                            )
                            AuthManager.updateUsername(formattedUsername)
                            Toast.makeText(context, "Username updated to $formattedUsername", Toast.LENGTH_SHORT).show()
                            onUsernameChanged(formattedUsername)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Save",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
