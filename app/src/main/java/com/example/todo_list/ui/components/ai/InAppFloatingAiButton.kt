package com.example.todo_list.ui.components.ai

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.todo_list.ai.FloatingAiButtonManager
import com.example.todo_list.ui.theme.TFMotion
import com.example.todo_list.utils.HapticManager
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * In-App Draggable Floating AI Assistant Shortcut Button (Apple iOS HIG).
 * Provides an edge-docked shortcut button within TaskFlow screens when system overlay
 * permission is not active or as an in-app overlay.
 *
 * Guaranteed Behavior:
 * - Always snaps to the left or right screen border upon release.
 * - Never remains in the center of the screen.
 * - Direct shortcut to open the AI chatbot on tap.
 */
@Composable
fun InAppFloatingAiButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val buttonSizeDp = 52.dp
    val buttonSizePx = with(density) { buttonSizeDp.toPx() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        val topMarginPx = with(density) { 70.dp.toPx() }
        val bottomMarginPx = with(density) { 90.dp.toPx() }

        val (savedIsRight, savedYRatio) = remember { FloatingAiButtonManager.getDockedPosition() }

        val initialX = if (savedIsRight) (screenWidthPx - buttonSizePx).coerceAtLeast(0f) else 0f
        val initialY = (screenHeightPx * savedYRatio).coerceIn(topMarginPx, (screenHeightPx - buttonSizePx - bottomMarginPx).coerceAtLeast(topMarginPx))

        val offsetX = remember { Animatable(initialX) }
        val offsetY = remember { Animatable(initialY) }
        val scale = remember { Animatable(1.0f) }

        var isDragging by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        offsetX.value.roundToInt(),
                        offsetY.value.roundToInt()
                    )
                }
                .size(buttonSizeDp)
                .scale(scale.value)
                .shadow(elevation = 10.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF7C3AED), // Deep vibrant Apple purple
                            Color(0xFFA855F7)  // Electric purple
                        )
                    )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                .pointerInput(screenWidthPx, screenHeightPx) {
                    var totalDragDistance = 0f

                    detectDragGestures(
                        onDragStart = {
                            totalDragDistance = 0f
                            isDragging = false
                            HapticManager.performClick(context)
                            coroutineScope.launch {
                                scale.animateTo(0.92f, TFMotion.press())
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragDistance += abs(dragAmount.x) + abs(dragAmount.y)
                            if (totalDragDistance > 15f && !isDragging) {
                                isDragging = true
                                coroutineScope.launch {
                                    scale.animateTo(1.06f, TFMotion.snappy())
                                }
                            }

                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                val newY = (offsetY.value + dragAmount.y).coerceIn(
                                    topMarginPx,
                                    (screenHeightPx - buttonSizePx - bottomMarginPx).coerceAtLeast(topMarginPx)
                                )
                                offsetY.snapTo(newY)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                scale.animateTo(1.0f, TFMotion.snappy())
                            }

                            if (!isDragging || totalDragDistance < 20f) {
                                // Tap event -> Open chatbot directly
                                onClick()
                            } else {
                                // Released from drag -> Snap strictly to nearest border (left or right)
                                val currentX = offsetX.value.toInt()
                                val targetX = FloatingAiButtonManager.calculateDockedTargetX(
                                    currentX = currentX,
                                    buttonWidth = buttonSizePx.toInt(),
                                    screenWidth = screenWidthPx.toInt()
                                ).toFloat()

                                val currentY = offsetY.value.toInt()
                                val targetY = FloatingAiButtonManager.clampYPosition(
                                    currentY = currentY,
                                    buttonHeight = buttonSizePx.toInt(),
                                    screenHeight = screenHeightPx.toInt(),
                                    topMargin = topMarginPx.toInt(),
                                    bottomMargin = bottomMarginPx.toInt()
                                ).toFloat()

                                coroutineScope.launch {
                                    offsetX.animateTo(targetX, TFMotion.snappy())
                                    offsetY.animateTo(targetY, TFMotion.snappy())

                                    val isRight = (targetX > 0)
                                    val yRatio = targetY / screenHeightPx
                                    FloatingAiButtonManager.savePosition(isRight, yRatio)
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                scale.animateTo(1.0f, TFMotion.snappy())
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Assistant Shortcut",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
