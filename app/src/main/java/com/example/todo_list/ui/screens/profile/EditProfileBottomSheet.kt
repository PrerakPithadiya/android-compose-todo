package com.example.todo_list.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.todo_list.manager.UserProfileManager
import com.example.todo_list.model.AvatarPreset
import com.example.todo_list.model.UserProfile
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

    var name by remember { mutableStateOf(profile.name) }
    var username by remember { mutableStateOf(profile.username) }
    var email by remember { mutableStateOf(profile.email) }
    var phone by remember { mutableStateOf(profile.phone) }
    var bio by remember { mutableStateOf(profile.bio) }
    var selectedPresetId by remember { mutableIntStateOf(profile.avatarPresetId) }
    var customUri by remember { mutableStateOf<String?>(profile.customAvatarUri) }
    var showImageCropDialog by remember { mutableStateOf(false) }
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

    val activeAvatarUrl = customUri ?: AvatarPreset.getById(selectedPresetId).avatarUrl

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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Profile",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelPrimary
                )

                TextButton(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Display name cannot be empty", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        UserProfileManager.updateProfile(
                            name = name,
                            username = username,
                            email = email,
                            phone = phone,
                            bio = bio
                        )
                        if (customUri != null) {
                            UserProfileManager.setCustomAvatarUri(customUri)
                        } else {
                            UserProfileManager.setAvatarPreset(selectedPresetId)
                        }
                        HapticManager.performSuccess(context)
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        onProfileSaved()
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "Done",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemBlue
                    )
                }
            }

            // Avatar Preview & Gallery Upload Trigger
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(90.dp)
                ) {
                    AsyncImage(
                        model = activeAvatarUrl,
                        contentDescription = "Active Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(2.dp, SystemBlue, CircleShape)
                    )

                    Surface(
                        shape = CircleShape,
                        color = SystemBlue,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                HapticManager.performClick(context)
                                photoPickerLauncher.launch("image/*")
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = "Change Photo",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            HapticManager.performClick(context)
                            photoPickerLauncher.launch("image/*")
                        }
                    ) {
                        Text(
                            text = if (customUri != null) "Choose Another Photo" else "Choose from Gallery",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SystemBlue
                        )
                    }

                    if (customUri != null) {
                        TextButton(
                            onClick = {
                                HapticManager.performClick(context)
                                pendingRawUri = customUri
                                showImageCropDialog = true
                            }
                        ) {
                            Text(
                                text = "Edit Crop & Filters",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SystemBlue
                            )
                        }
                    }
                }
            }

            // Preset Avatars Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CHOOSE AVATAR PRESET",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(AvatarPreset.PRESETS, key = { it.id }) { preset ->
                        val isSelected = (customUri == null && selectedPresetId == preset.id)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable {
                                HapticManager.performClick(context)
                                customUri = null
                                selectedPresetId = preset.id
                                UserProfileManager.setCustomAvatarUri(null)
                                UserProfileManager.setAvatarPreset(preset.id)
                            }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) SystemBlue else SystemDivider,
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
                                            .background(SystemBlue.copy(alpha = 0.35f))
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

                            Text(
                                text = preset.emoji,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = SystemDivider, thickness = 0.5.dp)

            // Text Inputs
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Person, contentDescription = "Name", tint = SystemBlue)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemBlue,
                        unfocusedBorderColor = SystemDivider
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username Handle") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.AlternateEmail, contentDescription = "Username", tint = SystemBlue)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemBlue,
                        unfocusedBorderColor = SystemDivider
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.MailOutline, contentDescription = "Email", tint = SystemBlue)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemBlue,
                        unfocusedBorderColor = SystemDivider
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Phone, contentDescription = "Phone", tint = SystemBlue)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemBlue,
                        unfocusedBorderColor = SystemDivider
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio & Status Note") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.EditNote, contentDescription = "Bio", tint = SystemBlue)
                    },
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemBlue,
                        unfocusedBorderColor = SystemDivider
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Save Changes Button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Display name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    UserProfileManager.updateProfile(
                        name = name,
                        username = username,
                        email = email,
                        phone = phone,
                        bio = bio
                    )
                    if (customUri != null) {
                        UserProfileManager.setCustomAvatarUri(customUri)
                    } else {
                        UserProfileManager.setAvatarPreset(selectedPresetId)
                    }
                    HapticManager.performSuccess(context)
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    onProfileSaved()
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Save Profile",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
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
                selectedPresetId = -1
                showImageCropDialog = false
                HapticManager.performSuccess(context)
                Toast.makeText(context, "Photo crop and adjustments applied", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
