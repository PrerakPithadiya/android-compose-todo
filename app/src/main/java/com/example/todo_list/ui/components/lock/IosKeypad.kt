package com.example.todo_list.ui.components.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.ui.theme.SystemGray
import com.example.todo_list.ui.theme.SystemGray5
import com.example.todo_list.ui.theme.SystemLabelPrimary
import com.example.todo_list.ui.theme.SystemLabelSecondary

private data class KeypadItem(
    val digit: String,
    val letters: String = ""
)

private val keypadRows = listOf(
    listOf(KeypadItem("1", ""), KeypadItem("2", "A B C"), KeypadItem("3", "D E F")),
    listOf(KeypadItem("4", "G H I"), KeypadItem("5", "J K L"), KeypadItem("6", "M N O")),
    listOf(KeypadItem("7", "P Q R S"), KeypadItem("8", "T U V"), KeypadItem("9", "W X Y Z")),
)

@Composable
fun IosKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onCancelClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Rows 1 to 3
        keypadRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { item ->
                    IosKeypadButton(
                        digit = item.digit,
                        subtext = item.letters,
                        onClick = { onDigitClick(item.digit) }
                    )
                }
            }
        }

        // Bottom Row: Cancel / 0 / Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Cancel or Empty Box
            if (onCancelClick != null) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCancelClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = SystemLabelPrimary
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(76.dp))
            }

            // Center: 0
            IosKeypadButton(
                digit = "0",
                subtext = "+",
                onClick = { onDigitClick("0") }
            )

            // Right: Delete / Backspace
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = SystemGray.copy(alpha = 0.4f)),
                        onClick = onDeleteClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Delete",
                    tint = SystemLabelPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun IosKeypadButton(
    digit: String,
    subtext: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(SystemGray5.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = SystemGray.copy(alpha = 0.4f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                color = SystemLabelPrimary,
                lineHeight = 30.sp
            )
            if (subtext.isNotEmpty()) {
                Text(
                    text = subtext,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemLabelSecondary,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
