package com.example.todo_list.ui.components.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.todo_list.ui.theme.SystemBlue
import com.example.todo_list.ui.theme.SystemGray2
import com.example.todo_list.ui.theme.SystemRed
import kotlin.math.hypot

@Composable
fun PatternLockView(
    onPatternComplete: (String) -> Unit,
    isError: Boolean = false,
    enabled: Boolean = true,
    accentColor: Color = SystemBlue,
    modifier: Modifier = Modifier
) {
    var selectedNodes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }

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
            // Automatically clear error pattern after brief moment
            selectedNodes = emptyList()
            currentTouchPoint = null
        }
    }

    val activeColor = if (isError) SystemRed else accentColor
    val inactiveColor = SystemGray2.copy(alpha = 0.6f)

    val nodeCentersCache = remember { Array(9) { Offset.Zero } }
    var cachedWidth by remember { mutableFloatStateOf(0f) }
    var cachedHeight by remember { mutableFloatStateOf(0f) }

    fun updateCenters(width: Float, height: Float) {
        if (width <= 0f || height <= 0f || (width == cachedWidth && height == cachedHeight)) return
        cachedWidth = width
        cachedHeight = height
        val stepX = width / 3f
        val stepY = height / 3f
        var idx = 0
        for (row in 0..2) {
            for (col in 0..2) {
                val cx = (col + 0.5f) * stepX
                val cy = (row + 0.5f) * stepY
                nodeCentersCache[idx++] = Offset(cx, cy)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .aspectRatio(1f)
            .graphicsLayer {
                translationX = shakeOffset.value
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    val canvasWidth = size.width.toFloat()
                    val canvasHeight = size.height.toFloat()
                    updateCenters(canvasWidth, canvasHeight)

                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedNodes = emptyList()
                            val nodeRadius = canvasWidth / 6f
                            val hitRadius = nodeRadius * 0.85f

                            var hitIndex = -1
                            for (i in 0 until 9) {
                                val center = nodeCentersCache[i]
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                if ((dx * dx + dy * dy) <= (hitRadius * hitRadius)) {
                                    hitIndex = i
                                    break
                                }
                            }

                            if (hitIndex != -1) {
                                selectedNodes = listOf(hitIndex)
                                currentTouchPoint = offset
                            } else {
                                currentTouchPoint = null
                            }
                        },
                        onDrag = { change, _ ->
                            val offset = change.position
                            currentTouchPoint = offset

                            val nodeRadius = canvasWidth / 6f
                            val hitRadius = nodeRadius * 0.85f

                            var hitIndex = -1
                            for (i in 0 until 9) {
                                val center = nodeCentersCache[i]
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                if ((dx * dx + dy * dy) <= (hitRadius * hitRadius)) {
                                    hitIndex = i
                                    break
                                }
                            }

                            if (hitIndex != -1 && !selectedNodes.contains(hitIndex)) {
                                selectedNodes = selectedNodes + hitIndex
                            }
                        },
                        onDragEnd = {
                            currentTouchPoint = null
                            if (selectedNodes.isNotEmpty()) {
                                val patternStr = selectedNodes.joinToString("-")
                                onPatternComplete(patternStr)
                            }
                        },
                        onDragCancel = {
                            currentTouchPoint = null
                            selectedNodes = emptyList()
                        }
                    )
                }
        ) {
            updateCenters(size.width, size.height)

            val strokeWidthPx = 5.dp.toPx()
            val outerRadiusPx = 28.dp.toPx()
            val dotRadiusPx = 7.dp.toPx()

            // 1. Draw Connecting Lines between Selected Nodes
            if (selectedNodes.isNotEmpty()) {
                val linePath = Path().apply {
                    val firstCenter = nodeCentersCache[selectedNodes.first()]
                    moveTo(firstCenter.x, firstCenter.y)
                    for (i in 1 until selectedNodes.size) {
                        val nextCenter = nodeCentersCache[selectedNodes[i]]
                        lineTo(nextCenter.x, nextCenter.y)
                    }
                }

                drawPath(
                    path = linePath,
                    color = activeColor,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 2. Draw line from last selected node to active finger drag position
                currentTouchPoint?.let { touchPos ->
                    val lastCenter = nodeCentersCache[selectedNodes.last()]
                    drawLine(
                        color = activeColor.copy(alpha = 0.7f),
                        start = lastCenter,
                        end = touchPos,
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Draw 3x3 Nodes
            for (index in 0 until 9) {
                val center = nodeCentersCache[index]
                val isSelected = selectedNodes.contains(index)

                if (isSelected) {
                    // Outer Translucent Glow Ring
                    drawCircle(
                        color = activeColor.copy(alpha = 0.2f),
                        radius = outerRadiusPx,
                        center = center
                    )
                    // Outer Ring Stroke
                    drawCircle(
                        color = activeColor,
                        radius = outerRadiusPx,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Inner Solid Center Dot
                    drawCircle(
                        color = activeColor,
                        radius = dotRadiusPx + 2.dp.toPx(),
                        center = center
                    )
                } else {
                    // Normal Inactive Dot
                    drawCircle(
                        color = inactiveColor,
                        radius = dotRadiusPx,
                        center = center
                    )
                }
            }
        }
    }
}
