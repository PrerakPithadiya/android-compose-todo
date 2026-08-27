package com.example.todo_list.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.*
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
fun CreateCategoryBottomSheet(
    onDismiss: () -> Unit,
    onCreateCategory: (TaskListCategory) -> Unit
) {
    val context = LocalContext.current
    var listName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#007AFF") }
    var selectedIconName by remember { mutableStateOf("List") }
    var nameError by remember { mutableStateOf(false) }

    val colors = listOf(
        "#007AFF", // System Blue
        "#AF52DE", // System Purple
        "#34C759", // System Green
        "#FF9500", // System Orange
        "#FF3B30", // System Red
        "#FF2D55", // System Pink
        "#30B0C7", // System Teal
        "#5856D6"  // System Indigo
    )

    val icons = listOf(
        "List" to Icons.Outlined.Category,
        "Work" to Icons.Outlined.WorkOutline,
        "Personal" to Icons.Outlined.Person,
        "Health" to Icons.Outlined.FavoriteBorder,
        "Study" to Icons.Outlined.School,
        "Shopping" to Icons.Outlined.ShoppingCart,
        "Star" to Icons.Outlined.StarBorder,
        "Flag" to Icons.Outlined.Flag
    )

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
                .imePadding()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "New Category",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SystemLabelPrimary
                    )
                    Text(
                        text = "Create a custom category with color & icon",
                        fontSize = 13.sp,
                        color = SystemLabelSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SystemGray5)
                        .size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Close", tint = SystemLabelSecondary, modifier = Modifier.size(20.dp))
                }
            }

            // Preview Badge Header
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(selectedColorHex)))
                ) {
                    Icon(
                        imageVector = getCategoryIcon(selectedIconName, listName),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Category Title Input Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "CATEGORY NAME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                OutlinedTextField(
                    value = listName,
                    onValueChange = {
                        listName = it
                        if (it.isNotBlank()) nameError = false
                    },
                    placeholder = { Text("e.g. Design, Finance, Travel, Groceries") },
                    singleLine = true,
                    isError = nameError,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = SystemSurface,
                        focusedContainerColor = SystemSurface,
                        unfocusedBorderColor = SystemDivider,
                        focusedBorderColor = SystemBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (nameError) {
                    Text(
                        text = "Please enter a category name",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }

            // Color Selector Row
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "COLOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { hex ->
                        val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .then(
                                    if (isSelected) Modifier.border(3.dp, SystemLabelPrimary, CircleShape) else Modifier
                                )
                                .clickable {
                                    HapticManager.performClick(context)
                                    selectedColorHex = hex
                                }
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Icon Selector Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ICON",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 0.5.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(icons) { (iconLabel, iconVector) ->
                        val isSelected = iconLabel.equals(selectedIconName, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SystemBlue else SystemSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isSelected) SystemBlue else SystemDivider
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    HapticManager.performClick(context)
                                    selectedIconName = iconLabel
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = iconLabel,
                                    tint = if (isSelected) Color.White else SystemLabelPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = iconLabel,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else SystemLabelPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Create Button
            Button(
                onClick = {
                    if (listName.isBlank()) {
                        nameError = true
                    } else {
                        HapticManager.performSuccess(context)
                        val newCategory = TaskListCategory(
                            id = "cat_${System.currentTimeMillis()}",
                            name = listName.trim(),
                            colorHex = selectedColorHex,
                            iconName = selectedIconName,
                            isSystemDefault = false
                        )
                        onCreateCategory(newCategory)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Create Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
