package com.example.todo_list.ui.components.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.security.BiometricAuthHelper
import com.example.todo_list.ui.theme.SystemBlue
import com.example.todo_list.ui.theme.SystemGray
import com.example.todo_list.ui.theme.SystemGray5
import com.example.todo_list.ui.theme.SystemLabelPrimary
import com.example.todo_list.utils.HapticManager

private val keypadDigits = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9")
)

@Composable
fun IosKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onCancelClick: (() -> Unit)? = null,
    onBiometricClick: (() -> Unit)? = null,
    biometricIcon: ImageVector? = null,
    biometricContentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolvedBiometricIcon = biometricIcon ?: BiometricAuthHelper.getBiometricIcon(context)
    val resolvedBiometricDesc = biometricContentDescription ?: "Unlock with ${BiometricAuthHelper.getBiometricDisplayName(context)}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Rows 1 to 3: Pure Digits (1-9) with no subtext, perfectly aligned and uniform
        keypadDigits.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { digit ->
                    IosKeypadButton(
                        digit = digit,
                        onClick = {
                            HapticManager.performClick(context)
                            onDigitClick(digit)
                        }
                    )
                }
            }
        }

        // Bottom Row: [Cancel / Biometric / Empty] | [ 0 ] | [ Backspace ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Button: Biometric Icon, Cancel Text, or Spacer
            when {
                onBiometricClick != null -> {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(SystemBlue.copy(alpha = 0.12f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = SystemBlue.copy(alpha = 0.3f)),
                                onClick = {
                                    HapticManager.performClick(context)
                                    onBiometricClick()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = resolvedBiometricIcon,
                            contentDescription = resolvedBiometricDesc,
                            tint = SystemBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                onCancelClick != null -> {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    HapticManager.performClick(context)
                                    onCancelClick()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = SystemLabelPrimary
                        )
                    }
                }
                else -> {
                    Spacer(modifier = Modifier.size(76.dp))
                }
            }

            // Center Button: 0
            IosKeypadButton(
                digit = "0",
                onClick = {
                    HapticManager.performClick(context)
                    onDigitClick("0")
                }
            )

            // Right Button: Delete / Backspace
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = SystemGray.copy(alpha = 0.4f)),
                        onClick = {
                            HapticManager.performClick(context)
                            onDeleteClick()
                        }
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
        Text(
            text = digit,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            color = SystemLabelPrimary
        )
    }
}
