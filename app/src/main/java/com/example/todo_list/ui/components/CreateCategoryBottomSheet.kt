package com.example.todo_list.ui.components

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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.model.TaskListCategory
import com.example.todo_list.ui.components.primitives.TFButton
import com.example.todo_list.ui.components.primitives.TFButtonType
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
    val colors = TFTheme.colors
    val typography = TFTheme.typography
    val accentRoles = LocalAccentRoles.current

    var listName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#007AFF") }
    var selectedIconName by remember { mutableStateOf("List") }
    var nameError by remember { mutableStateOf(false) }

    val paletteColors = listOf(
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
        containerColor = colors.canvas,
        shape = TFShape.sheet,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(colors.labelTertiary)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TFSpace.lg)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(TFSpace.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "New Category",
                        style = typography.title2,
                        color = colors.labelPrimary
                    )
                    Text(
                        text = "Create a custom category with color & icon",
                        style = typography.subheadline,
                        color = colors.labelSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.fillControl)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Close",
                        tint = colors.labelSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Preview Badge Header (80dp live preview)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TFSpace.sm)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(selectedColorHex)))
                ) {
                    Icon(
                        imageVector = getCategoryIcon(selectedIconName, listName),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Category Title Input Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "CATEGORY NAME",
                    style = typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = colors.labelSecondary,
                    letterSpacing = 0.5.sp
                )

                OutlinedTextField(
                    value = listName,
                    onValueChange = {
                        listName = it
                        if (it.isNotBlank()) nameError = false
                    },
                    placeholder = {
                        Text(
                            "e.g. Design, Finance, Travel, Groceries",
                            style = typography.body,
                            color = colors.labelSecondary.copy(alpha = 0.5f)
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.labelPrimary
                    ),
                    singleLine = true,
                    isError = nameError,
                    shape = TFShape.card,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = colors.card,
                        focusedContainerColor = colors.card,
                        unfocusedBorderColor = if (colors.isDark) colors.cardStroke else colors.separator,
                        focusedBorderColor = accentRoles.accentText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (nameError) {
                    Text(
                        text = "Please enter a category name",
                        color = colors.red,
                        style = typography.caption
                    )
                }
            }

            // Color Selector Row
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "COLOR",
                    style = typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = colors.labelSecondary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    paletteColors.forEach { hex ->
                        val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .then(
                                    if (isSelected) Modifier.border(3.dp, colors.labelPrimary, CircleShape) else Modifier
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
                    style = typography.caption,
                    fontWeight = FontWeight.Bold,
                    color = colors.labelSecondary,
                    letterSpacing = 0.5.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(icons) { (iconLabel, iconVector) ->
                        val isSelected = iconLabel.equals(selectedIconName, ignoreCase = true)
                        Surface(
                            shape = TFShape.card,
                            color = if (isSelected) accentRoles.accentFill else colors.card,
                            border = BorderStroke(
                                hairline(),
                                if (isSelected) accentRoles.accent else (if (colors.isDark) colors.cardStroke else colors.separator)
                            ),
                            modifier = Modifier
                                .clip(TFShape.card)
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
                                    tint = if (isSelected) accentRoles.onAccent else colors.labelPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = iconLabel,
                                    style = typography.subheadline,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) accentRoles.onAccent else colors.labelPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Create Button
            TFButton(
                text = "Create Category",
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
                type = TFButtonType.FILLED,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
