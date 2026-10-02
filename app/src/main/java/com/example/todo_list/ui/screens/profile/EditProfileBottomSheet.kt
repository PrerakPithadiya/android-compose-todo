package com.example.todo_list.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.todo_list.manager.UserProfileManager
import com.example.todo_list.model.AvatarPreset
import com.example.todo_list.model.UserProfile
import com.example.todo_list.ui.components.primitives.TFButton
import com.example.todo_list.ui.components.primitives.TFButtonType
import com.example.todo_list.ui.components.primitives.TFCardGroup
import com.example.todo_list.ui.components.primitives.TFGroupDivider
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileBottomSheet(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onProfileSaved: () -> Unit
) {
    val context = LocalContext.current
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles
    val typography = TFTheme.typography

    var name by remember { mutableStateOf(profile.name) }
    var username by remember { mutableStateOf(profile.username) }
    var email by remember { mutableStateOf(profile.email) }
    var phone by remember { mutableStateOf(profile.phone) }
    var bio by remember { mutableStateOf(profile.bio) }
    var selectedPresetId by remember { mutableIntStateOf(profile.avatarPresetId) }
    var customUri by remember { mutableStateOf<String?>(profile.customAvatarUri) }
    var showImageCropDialog by remember { mutableStateOf(false) }
    var showChangePasswordSheet by remember { mutableStateOf(false) }
    var pendingRawUri by remember { mutableStateOf<String?>(null) }

    // Image Picker Launcher - Stages photo for editing instead of direct commit
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRawUri = uri.toString()
            showImageCropDialog = true
        }
    }

    // Dynamic avatar url: shows custom image if present, or the selected avatar preset
    val currentDisplayAvatarUrl = if (!customUri.isNullOrBlank()) {
        customUri
    } else {
        val preset = AvatarPreset.PRESETS.find { it.id == selectedPresetId } ?: AvatarPreset.PRESETS[0]
        preset.avatarUrl
    }

    // Single source of truth for saving profile changes atomically
    val onSaveProfile: () -> Unit = {
        if (name.isBlank()) {
            Toast.makeText(context, "Display name cannot be empty", Toast.LENGTH_SHORT).show()
        } else {
            UserProfileManager.saveProfile(
                name = name.trim(),
                username = username.trim(),
                email = email.trim(),
                phone = phone.trim(),
                bio = bio.trim(),
                avatarPresetId = selectedPresetId,
                customAvatarUri = customUri
            )
            HapticManager.performSuccess(context)
            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            onProfileSaved()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.cardRaised,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.separator) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Apple HIG Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        HapticManager.performClick(context)
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "Cancel",
                        style = typography.body,
                        color = colors.labelSecondary
                    )
                }

                Text(
                    text = "Edit Profile",
                    style = typography.headline,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.labelPrimary
                )

                TextButton(
                    onClick = {
                        HapticManager.performClick(context)
                        onSaveProfile()
                    }
                ) {
                    Text(
                        text = "Done",
                        style = typography.body,
                        fontWeight = FontWeight.Bold,
                        color = accentRoles.accentText
                    )
                }
            }

            // Avatar Preview & Capsule Action Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Interactive 96dp Avatar with Accent Border & Docked Camera Badge
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(96.dp)
                ) {
                    AsyncImage(
                        model = currentDisplayAvatarUrl,
                        contentDescription = "Active Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(colors.cardSecondary)
                            .border(
                                width = 2.dp,
                                color = if (customUri != null) accentRoles.accent else colors.cardStroke,
                                shape = CircleShape
                            )
                            .clickable {
                                HapticManager.performClick(context)
                                photoPickerLauncher.launch("image/*")
                            }
                    )

                    Surface(
                        shape = CircleShape,
                        color = accentRoles.accentFill,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable {
                                HapticManager.performClick(context)
                                photoPickerLauncher.launch("image/*")
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = "Change Photo",
                                tint = accentRoles.onAccent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Apple HIG Capsule Action Controls
                if (customUri != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Change Photo Pill
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.fillControl,
                            border = if (colors.isDark) BorderStroke(hairline(), colors.cardStroke) else null,
                            modifier = Modifier
                                .height(36.dp)
                                .clickable {
                                    HapticManager.performClick(context)
                                    photoPickerLauncher.launch("image/*")
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = null,
                                    tint = accentRoles.accentText,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Change",
                                    style = typography.caption,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentRoles.accentText
                                )
                            }
                        }

                        // Edit Crop & Filters Pill
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.fillControl,
                            border = if (colors.isDark) BorderStroke(hairline(), colors.cardStroke) else null,
                            modifier = Modifier
                                .height(36.dp)
                                .clickable {
                                    HapticManager.performClick(context)
                                    pendingRawUri = customUri
                                    showImageCropDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Crop,
                                    contentDescription = null,
                                    tint = accentRoles.accentText,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Crop & Filter",
                                    style = typography.caption,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentRoles.accentText
                                )
                            }
                        }

                        // Remove Photo Pill (Destructive)
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.red.copy(alpha = 0.12f),
                            border = if (colors.isDark) BorderStroke(hairline(), colors.red.copy(alpha = 0.3f)) else null,
                            modifier = Modifier
                                .height(36.dp)
                                .clickable {
                                    HapticManager.performClick(context)
                                    customUri = null
                                    if (selectedPresetId < 0) {
                                        selectedPresetId = 0
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = colors.red,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Remove",
                                    style = typography.caption,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.red
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.fillControl,
                        border = if (colors.isDark) BorderStroke(hairline(), colors.cardStroke) else null,
                        modifier = Modifier
                            .height(36.dp)
                            .clickable {
                                HapticManager.performClick(context)
                                photoPickerLauncher.launch("image/*")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddPhotoAlternate,
                                contentDescription = null,
                                tint = accentRoles.accentText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Choose from Gallery",
                                style = typography.subheadline,
                                fontWeight = FontWeight.SemiBold,
                                color = accentRoles.accentText
                            )
                        }
                    }
                }
            }

            // Preset Avatars Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHOOSE AVATAR PRESET",
                        style = typography.footnote,
                        fontWeight = FontWeight.Bold,
                        color = colors.labelSecondary,
                        letterSpacing = 0.5.sp
                    )

                    if (customUri == null) {
                        Text(
                            text = AvatarPreset.getById(selectedPresetId).name,
                            style = typography.caption,
                            fontWeight = FontWeight.Medium,
                            color = accentRoles.accentText
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    items(AvatarPreset.PRESETS, key = { it.id }) { preset ->
                        val isSelected = (customUri == null && selectedPresetId == preset.id)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable {
                                HapticManager.performClick(context)
                                customUri = null
                                selectedPresetId = preset.id
                            }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(colors.cardSecondary)
                                    .border(
                                        width = if (isSelected) 2.5.dp else hairline(),
                                        color = if (isSelected) accentRoles.accent else colors.cardStroke,
                                        shape = CircleShape
                                    )
                            ) {
                                AsyncImage(
                                    model = preset.avatarUrl,
                                    contentDescription = preset.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                )

                                if (isSelected) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(accentRoles.accent.copy(alpha = 0.35f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) accentRoles.accentContainer else colors.fillControl.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = preset.emoji,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Inset Grouped Form: Profile Information
            TFCardGroup(headerTitle = "PROFILE INFORMATION") {
                ProfileInputField(
                    label = "Display Name",
                    value = name,
                    onValueChange = { name = it },
                    icon = Icons.Outlined.Person,
                    placeholder = "Your full name",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                TFGroupDivider(startIndent = 56.dp)

                ProfileInputField(
                    label = "Username Handle",
                    value = username,
                    onValueChange = { username = it },
                    icon = Icons.Outlined.AlternateEmail,
                    placeholder = "@username"
                )

                TFGroupDivider(startIndent = 56.dp)

                ProfileInputField(
                    label = "Email Address",
                    value = email,
                    onValueChange = { email = it },
                    icon = Icons.Outlined.MailOutline,
                    placeholder = "name@example.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                TFGroupDivider(startIndent = 56.dp)

                ProfileInputField(
                    label = "Phone Number",
                    value = phone,
                    onValueChange = { phone = it },
                    icon = Icons.Outlined.Phone,
                    placeholder = "+1 (555) 000-0000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                TFGroupDivider(startIndent = 56.dp)

                ProfileInputField(
                    label = "Bio & Status Note",
                    value = bio,
                    onValueChange = { bio = it },
                    icon = Icons.Outlined.EditNote,
                    placeholder = "Add a bio...",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }

            // Inset Grouped Card: Account Security
            TFCardGroup(headerTitle = "ACCOUNT SECURITY") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            HapticManager.performClick(context)
                            showChangePasswordSheet = true
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LockReset,
                                contentDescription = "Password",
                                tint = accentRoles.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Change Account Password",
                                style = typography.subheadline,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.labelPrimary
                            )
                            Text(
                                text = "Protected with Multi-Factor Security Gate",
                                style = typography.caption,
                                color = colors.labelSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = colors.labelSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Save Profile Primary Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                TFButton(
                    text = "Save Profile",
                    onClick = {
                        HapticManager.performClick(context)
                        onSaveProfile()
                    },
                    type = TFButtonType.FILLED,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Modal Sheet: Multi-Stage Secure Change Password Sheet
    if (showChangePasswordSheet) {
        ChangePasswordSecuritySheet(
            onDismiss = { showChangePasswordSheet = false },
            onPasswordChanged = {
                showChangePasswordSheet = false
                Toast.makeText(context, "Account password updated securely! 🛡️", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Apple iOS Move & Scale Photo Crop / Filter Dialog
    if (showImageCropDialog && pendingRawUri != null) {
        ImageCropEditorDialog(
            rawImageUri = pendingRawUri!!,
            onDismiss = {
                showImageCropDialog = false
            },
            onPhotoFinalized = { croppedUri ->
                customUri = croppedUri
                showImageCropDialog = false
                HapticManager.performSuccess(context)
                Toast.makeText(context, "Photo crop and adjustments applied", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Apple iOS Inset Grouped Profile Input Field Row.
 */
@Composable
private fun ProfileInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val colors = TFTheme.colors
    val accentRoles = TFTheme.accentRoles
    val typography = TFTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .padding(top = if (singleLine) 0.dp else 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentRoles.accent,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = typography.caption,
                color = colors.labelSecondary,
                fontWeight = FontWeight.Medium
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                minLines = minLines,
                maxLines = maxLines,
                keyboardOptions = keyboardOptions,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.labelPrimary
                ),
                cursorBrush = SolidColor(accentRoles.accentText),
                decorationBox = { innerTextField ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = typography.body,
                            color = colors.labelSecondary.copy(alpha = 0.45f)
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )
        }
    }
}
