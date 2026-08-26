package com.example.todo_list.ui.components.lock

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.todo_list.ui.theme.SystemBlue
import com.example.todo_list.ui.theme.SystemGray2
import com.example.todo_list.ui.theme.SystemRed

@Composable
fun PinDotsView(
    totalDigits: Int,
    enteredCount: Int,
    isError: Boolean = false,
    accentColor: Color = SystemBlue,
    modifier: Modifier = Modifier
) {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -10f at 250
                    10f at 300
                    -5f at 350
                    0f at 400
                }
            )
        }
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                translationX = shakeOffset.value
            }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDigits) { index ->
            val isFilled = index < enteredCount
            val dotColor by animateColorAsState(
                targetValue = when {
                    isError -> SystemRed
                    isFilled -> accentColor
                    else -> Color.Transparent
                },
                animationSpec = tween(durationMillis = 150),
                label = "PinDotColor"
            )

            val borderColor by animateColorAsState(
                targetValue = when {
                    isError -> SystemRed
                    isFilled -> accentColor
                    else -> SystemGray2
                },
                animationSpec = tween(durationMillis = 150),
                label = "PinBorderColor"
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(
                        width = if (isFilled || isError) 0.dp else 1.5.dp,
                        color = borderColor,
                        shape = CircleShape
                    )
            )
        }
    }
}
