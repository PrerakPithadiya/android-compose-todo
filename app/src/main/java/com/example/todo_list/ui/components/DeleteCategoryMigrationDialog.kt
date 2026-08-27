package com.example.todo_list.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.model.TaskListCategory
import com.example.todo_list.ui.screens.getCategoryIcon
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteCategoryMigrationDialog(
    categoryToDelete: TaskListCategory,
    affectedTasksCount: Int,
    availableCategories: List<TaskListCategory>,
    onDismiss: () -> Unit,
    onConfirmDeleteAndMigrate: (targetCategoryName: String) -> Unit,
    onRequestCreateNewCategory: () -> Unit
) {
    val context = LocalContext.current

    // Case 1: Category contains 0 tasks - Simple confirmation dialog
    if (affectedTasksCount == 0) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SystemRed.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = SystemRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Delete \"${categoryToDelete.name}\"?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = SystemLabelPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this category? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = SystemLabelSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        HapticManager.performWarning(context)
                        // Fallback destination is empty since 0 tasks exist
                        val fallback = availableCategories.firstOrNull()?.name ?: "Personal"
                        onConfirmDeleteAndMigrate(fallback)
                    }
                ) {
                    Text("Delete", color = SystemRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = SystemBlue)
                }
            },
            containerColor = SystemSurface,
            shape = RoundedCornerShape(20.dp)
        )
        return
    }

    // Case 2: Category has existing tasks - Migration Modal Sheet
    val candidateCategories = remember(availableCategories, categoryToDelete) {
        availableCategories.filter { it.id != categoryToDelete.id }
    }

    var selectedTargetCategory by remember {
        mutableStateOf(candidateCategories.firstOrNull()?.name ?: "Personal")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SystemGroupedBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with warning icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SystemRed.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = SystemRed,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Migrate Existing Tasks",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemLabelPrimary
                    )
                    Text(
                        text = "Category \"${categoryToDelete.name}\" has $affectedTasksCount task${if (affectedTasksCount > 1) "s" else ""}",
                        fontSize = 13.sp,
                        color = SystemLabelSecondary
                    )
                }
            }

            Text(
                text = "Choose which category to transfer these $affectedTasksCount task${if (affectedTasksCount > 1) "s" else ""} to before deleting \"${categoryToDelete.name}\":",
                fontSize = 14.sp,
                color = SystemLabelPrimary,
                lineHeight = 20.sp
            )

            // Selectable Destination Category List
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SystemSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, SystemDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(candidateCategories, key = { it.id }) { cat ->
                        val isSelected = selectedTargetCategory.equals(cat.name, ignoreCase = true)
                        val catColor = cat.getColor()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticManager.performClick(context)
                                    selectedTargetCategory = cat.name
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat.iconName, cat.name),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = cat.name,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = SystemLabelPrimary
                                )
                            }

                            if (isSelected) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(SystemBlue)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(1.5.dp, SystemGray2, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // Option to create a new category directly
            OutlinedButton(
                onClick = {
                    HapticManager.performClick(context)
                    onRequestCreateNewCategory()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SystemBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create New Category & Migrate Here", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons: Migrate & Delete / Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Cancel", color = SystemLabelSecondary, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        HapticManager.performSuccess(context)
                        onConfirmDeleteAndMigrate(selectedTargetCategory)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemRed),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Migrate & Delete", color = Color.White, fontWeight = FontWeight.Bold)
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
