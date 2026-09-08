package com.example.todo_list.ui.components.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.ui.theme.*

/**
 * Authentic Apple iOS 6-Digit Verification Code (OTP) Input Component.
 * Features auto-focusing cells, fluid highlight animations, and error shake.
 */
@Composable
fun IosOtpInputView(
    otpCode: String,
    onOtpChange: (String) -> Unit,
    length: Int = 6,
    isError: Boolean = false,
    onComplete: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val shakeOffset = remember { Animatable(0f) }

    // Trigger error shake animation when isError turns true
    LaunchedEffect(isError) {
        if (isError) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    (-14f) at 50
                    14f at 100
                    (-10f) at 150
                    10f at 200
                    (-6f) at 250
                    6f at 300
                    (-2f) at 350
                    0f at 400
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        // Request keyboard focus immediately upon landing on OTP screen
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer { translationX = shakeOffset.value }
            .fillMaxWidth()
    ) {
        // Invisible input capture field
        BasicTextField(
            value = otpCode,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }.take(length)
                onOtpChange(filtered)
                if (filtered.length == length) {
                    focusManager.clearFocus()
                    onComplete(filtered)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (otpCode.length == length) {
                        onComplete(otpCode)
                    }
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .focusRequester(focusRequester)
                .size(1.dp) // Hidden from visual layout
        )

        // 6 Apple HIG Digit Cells
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusRequester.requestFocus()
                }
        ) {
            for (i in 0 until length) {
                val digit = otpCode.getOrNull(i)?.toString() ?: ""
                val isFocused = otpCode.length == i || (otpCode.length == length && i == length - 1)

                val borderColor by animateColorAsState(
                    targetValue = when {
                        isError -> SystemRed
                        isFocused -> SystemBlue
                        digit.isNotEmpty() -> SystemBlue.copy(alpha = 0.5f)
                        else -> SystemDivider
                    },
                    animationSpec = tween(durationMillis = 180),
                    label = "OtpBorderColor"
                )

                val cellBg by animateColorAsState(
                    targetValue = when {
                        isError -> SystemRed.copy(alpha = 0.08f)
                        isFocused -> SystemBlueLight.copy(alpha = 0.35f)
                        else -> SystemSurface
                    },
                    animationSpec = tween(durationMillis = 180),
                    label = "OtpCellBg"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 46.dp, height = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cellBg)
                        .border(
                            width = if (isFocused || isError) 2.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    if (digit.isNotEmpty()) {
                        Text(
                            text = digit,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isError) SystemRed else SystemLabelPrimary,
                            textAlign = TextAlign.Center
                        )
                    } else if (isFocused && !isError) {
                        // Subtle Apple pulse dot for active unfilled box
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SystemBlue.copy(alpha = 0.6f))
                        )
                    }
                }
            }
        }
    }
}
