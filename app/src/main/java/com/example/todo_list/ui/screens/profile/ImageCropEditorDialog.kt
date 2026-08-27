package com.example.todo_list.ui.screens.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Filter definitions for Apple iOS Photo aesthetic.
 */
enum class ApplePhotoFilter(val displayName: String, val colorMatrix: ColorMatrix) {
    ORIGINAL(
        "Original",
        ColorMatrix()
    ),
    VIVID(
        "Vivid",
        ColorMatrix(
            floatArrayOf(
                1.30f, -0.05f, -0.05f, 0f, 5f,
                -0.05f, 1.30f, -0.05f, 0f, 5f,
                -0.05f, -0.05f, 1.30f, 0f, 5f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    ),
    WARM(
        "Warm",
        ColorMatrix(
            floatArrayOf(
                1.15f, 0f, 0f, 0f, 18f,
                0f, 1.05f, 0f, 0f, 8f,
                0f, 0f, 0.88f, 0f, -14f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    ),
    COOL(
        "Cool",
        ColorMatrix(
            floatArrayOf(
                0.90f, 0f, 0f, 0f, -10f,
                0f, 1.02f, 0f, 0f, 5f,
                0f, 0f, 1.25f, 0f, 22f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    ),
    NOIR(
        "Noir",
        ColorMatrix(
            floatArrayOf(
                0.33f, 0.59f, 0.11f, 0f, -12f,
                0.33f, 0.59f, 0.11f, 0f, -12f,
                0.33f, 0.59f, 0.11f, 0f, -12f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    ),
    SILVERTONE(
        "Silvertone",
        ColorMatrix(
            floatArrayOf(
                0.30f, 0.55f, 0.15f, 0f, 18f,
                0.30f, 0.55f, 0.15f, 0f, 18f,
                0.30f, 0.55f, 0.15f, 0f, 18f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    ),
    DRAMATIC(
        "Dramatic",
        ColorMatrix(
            floatArrayOf(
                1.40f, -0.08f, -0.08f, 0f, -18f,
                -0.08f, 1.40f, -0.08f, 0f, -18f,
                -0.08f, -0.08f, 1.40f, 0f, -18f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
}

@Composable
fun ImageCropEditorDialog(
    rawImageUri: String,
    onDismiss: () -> Unit,
    onPhotoFinalized: (croppedUriString: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessingSave by remember { mutableStateOf(false) }

    // Transform State
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var isFlippedHorizontal by remember { mutableStateOf(false) }
    var isInteracting by remember { mutableStateOf(false) }

    // Filter & Adjustments State
    var selectedFilter by remember { mutableStateOf(ApplePhotoFilter.ORIGINAL) }
    var brightness by remember { mutableFloatStateOf(0f) } // -40f..40f
    var contrast by remember { mutableFloatStateOf(1f) }   // 0.6f..1.4f
    var activeToolTab by remember { mutableIntStateOf(0) } // 0: Transform, 1: Filters, 2: Adjustments

    // Load original bitmap asynchronously
    LaunchedEffect(rawImageUri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(rawImageUri)
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                loadedBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // 1. Apple-style Top Bar ("Cancel" | "Move and Scale" | "Choose")
                Surface(
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                HapticManager.performClick(context)
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 17.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Text(
                            text = "Move and Scale",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        TextButton(
                            enabled = !isLoading && !isProcessingSave && loadedBitmap != null,
                            onClick = {
                                val bmp = loadedBitmap ?: return@TextButton
                                isProcessingSave = true
                                HapticManager.performSuccess(context)
                                coroutineScope.launch {
                                    try {
                                        val savedUri = withContext(Dispatchers.IO) {
                                            renderAndSaveCroppedAvatar(
                                                context = context,
                                                sourceBitmap = bmp,
                                                scale = scale,
                                                offset = offset,
                                                rotationDegrees = rotationAngle,
                                                isFlipped = isFlippedHorizontal,
                                                filter = selectedFilter,
                                                brightness = brightness,
                                                contrast = contrast
                                            )
                                        }
                                        onPhotoFinalized(savedUri)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Error saving photo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isProcessingSave = false
                                    }
                                }
                            }
                        ) {
                            if (isProcessingSave) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = SystemBlue,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Choose",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SystemBlue
                                )
                            }
                        }
                    }
                }

                // 2. Interactive Direct Canvas Viewport (Image + Scrim + Ring + Grid) with Exact Bounding Constraints
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = SystemBlue)
                    } else if (loadedBitmap != null) {
                        val bmp = loadedBitmap!!
                        val circleRadiusDp = 140.dp // 280dp viewport diameter
                        val density = LocalDensity.current
                        val circleRadiusPx = with(density) { circleRadiusDp.toPx() }

                        // Rotated source dimensions
                        val isRotated90or270 = (rotationAngle % 180f != 0f)
                        val rotatedSrcWidth = if (isRotated90or270) bmp.height.toFloat() else bmp.width.toFloat()
                        val rotatedSrcHeight = if (isRotated90or270) bmp.width.toFloat() else bmp.height.toFloat()

                        // Base scale ensures the smallest dimension completely covers the 280dp circle
                        val minRotatedDim = minOf(rotatedSrcWidth, rotatedSrcHeight)
                        val baseScale = (circleRadiusPx * 2f) / minRotatedDim
                        val effectiveScale = baseScale * scale

                        // Rendered dimensions on screen at the current effectiveScale
                        val currentRenderWidth = rotatedSrcWidth * effectiveScale
                        val currentRenderHeight = rotatedSrcHeight * effectiveScale

                        // Maximum allowable pan offsets to keep circle 100% inside the photo (zero black margins)
                        val maxPanX = maxOf(0f, (currentRenderWidth / 2f) - circleRadiusPx)
                        val maxPanY = maxOf(0f, (currentRenderHeight / 2f) - circleRadiusPx)

                        // Clamped pan offset
                        val clampedOffset = Offset(
                            x = offset.x.coerceIn(-maxPanX, maxPanX),
                            y = offset.y.coerceIn(-maxPanY, maxPanY)
                        )

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(rotationAngle, scale, isFlippedHorizontal) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        isInteracting = true
                                        val newScale = (scale * zoom).coerceIn(1.0f, 4.5f)
                                        val newEffectiveScale = baseScale * newScale
                                        val newRenderW = rotatedSrcWidth * newEffectiveScale
                                        val newRenderH = rotatedSrcHeight * newEffectiveScale
                                        val curMaxX = maxOf(0f, (newRenderW / 2f) - circleRadiusPx)
                                        val curMaxY = maxOf(0f, (newRenderH / 2f) - circleRadiusPx)

                                        scale = newScale
                                        val candidateOffset = offset + pan
                                        offset = Offset(
                                            x = candidateOffset.x.coerceIn(-curMaxX, curMaxX),
                                            y = candidateOffset.y.coerceIn(-curMaxY, curMaxY)
                                        )
                                    }
                                }
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val canvasCenter = Offset(canvasWidth / 2f, canvasHeight / 2f)

                            // 1. Draw Transformed Photo with Filters & Matrix
                            drawIntoCanvas { composeCanvas ->
                                val nativeCanvas = composeCanvas.nativeCanvas
                                val paint = android.graphics.Paint(
                                    android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG
                                )

                                // Apply live ColorMatrix filter to paint
                                val cm = createAdjustedColorMatrix(selectedFilter, brightness, contrast)
                                paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm.values)

                                val matrix = android.graphics.Matrix()
                                val srcCenterX = bmp.width / 2f
                                val srcCenterY = bmp.height / 2f

                                // Center bitmap on origin (0, 0)
                                matrix.postTranslate(-srcCenterX, -srcCenterY)

                                // Flip horizontally around origin
                                if (isFlippedHorizontal) {
                                    matrix.postScale(-1f, 1f)
                                }

                                // Rotate around origin
                                matrix.postRotate(rotationAngle)

                                // Scale around origin
                                matrix.postScale(effectiveScale, effectiveScale)

                                // Translate to center + clamped user pan offset
                                matrix.postTranslate(canvasCenter.x + clampedOffset.x, canvasCenter.y + clampedOffset.y)

                                nativeCanvas.drawBitmap(bmp, matrix, paint)
                            }

                            // 2. Draw Dark Scrim Outside the Circle (Using PathFillType.EvenOdd)
                            val scrimPath = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
                                addOval(
                                    Rect(
                                        canvasCenter.x - circleRadiusPx,
                                        canvasCenter.y - circleRadiusPx,
                                        canvasCenter.x + circleRadiusPx,
                                        canvasCenter.y + circleRadiusPx
                                    )
                                )
                            }
                            drawPath(
                                path = scrimPath,
                                color = Color(0xD9000000) // 85% pure black backdrop outside circle
                            )

                            // 3. Draw Rule-of-Thirds Grid inside the circle
                            val step = (circleRadiusPx * 2) / 3f
                            val left = canvasCenter.x - circleRadiusPx
                            val top = canvasCenter.y - circleRadiusPx

                            // Vertical grid lines
                            drawLine(
                                color = Color.White.copy(alpha = 0.35f),
                                start = Offset(left + step, top + 15.dp.toPx()),
                                end = Offset(left + step, top + circleRadiusPx * 2 - 15.dp.toPx()),
                                strokeWidth = 0.75.dp.toPx()
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.35f),
                                start = Offset(left + step * 2, top + 15.dp.toPx()),
                                end = Offset(left + step * 2, top + circleRadiusPx * 2 - 15.dp.toPx()),
                                strokeWidth = 0.75.dp.toPx()
                            )

                            // Horizontal grid lines
                            drawLine(
                                color = Color.White.copy(alpha = 0.35f),
                                start = Offset(left + 15.dp.toPx(), top + step),
                                end = Offset(left + circleRadiusPx * 2 - 15.dp.toPx(), top + step),
                                strokeWidth = 0.75.dp.toPx()
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.35f),
                                start = Offset(left + 15.dp.toPx(), top + step * 2),
                                end = Offset(left + circleRadiusPx * 2 - 15.dp.toPx(), top + step * 2),
                                strokeWidth = 0.75.dp.toPx()
                            )

                            // 4. Crisp White Circular Viewport Border
                            drawCircle(
                                color = Color.White,
                                radius = circleRadiusPx,
                                center = canvasCenter,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }

                // 3. Bottom Tool Tabs & Controls (Elevated well above system gesture bar)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161618))
                        .navigationBarsPadding()
                        .padding(bottom = 44.dp)
                ) {
                    // Active Tool Panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (activeToolTab) {
                            0 -> TransformToolPanel(
                                scale = scale,
                                onScaleChange = { newScale ->
                                    scale = newScale
                                },
                                onRotate90 = {
                                    HapticManager.performClick(context)
                                    rotationAngle = (rotationAngle + 90f) % 360f
                                    offset = Offset.Zero
                                },
                                onFlip = {
                                    HapticManager.performClick(context)
                                    isFlippedHorizontal = !isFlippedHorizontal
                                },
                                onReset = {
                                    HapticManager.performClick(context)
                                    scale = 1f
                                    offset = Offset.Zero
                                    rotationAngle = 0f
                                    isFlippedHorizontal = false
                                    selectedFilter = ApplePhotoFilter.ORIGINAL
                                    brightness = 0f
                                    contrast = 1f
                                    Toast.makeText(context, "Reset to original", Toast.LENGTH_SHORT).show()
                                }
                            )
                            1 -> FiltersToolPanel(
                                selectedFilter = selectedFilter,
                                onFilterSelect = { filter ->
                                    HapticManager.performClick(context)
                                    selectedFilter = filter
                                }
                            )
                            2 -> AdjustmentsToolPanel(
                                brightness = brightness,
                                onBrightnessChange = { brightness = it },
                                contrast = contrast,
                                onContrastChange = { contrast = it },
                                onResetAdjustments = {
                                    HapticManager.performClick(context)
                                    brightness = 0f
                                    contrast = 1f
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0x2EFFFFFF), thickness = 0.5.dp)

                    // Tab Selector Bar (Crop & Scale | Filters | Adjust) - comfortable elevated touch targets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EditorTabButton(
                            icon = Icons.Outlined.Crop,
                            label = "Crop & Scale",
                            isSelected = activeToolTab == 0,
                            onClick = {
                                HapticManager.performClick(context)
                                activeToolTab = 0
                            }
                        )
                        EditorTabButton(
                            icon = Icons.Outlined.AutoAwesome,
                            label = "Filters",
                            isSelected = activeToolTab == 1,
                            onClick = {
                                HapticManager.performClick(context)
                                activeToolTab = 1
                            }
                        )
                        EditorTabButton(
                            icon = Icons.Outlined.Tune,
                            label = "Adjust",
                            isSelected = activeToolTab == 2,
                            onClick = {
                                HapticManager.performClick(context)
                                activeToolTab = 2
                            }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Sub-Components
// -------------------------------------------------------------------------------------------------

@Composable
fun TransformToolPanel(
    scale: Float,
    onScaleChange: (Float) -> Unit,
    onRotate90: () -> Unit,
    onFlip: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransformActionButton(
                icon = Icons.AutoMirrored.Outlined.RotateRight,
                label = "Rotate 90°",
                onClick = onRotate90
            )

            TransformActionButton(
                icon = Icons.Outlined.Flip,
                label = "Flip",
                onClick = onFlip
            )

            TransformActionButton(
                icon = Icons.Outlined.RestartAlt,
                label = "Reset",
                onClick = onReset
            )
        }

        // Zoom Slider Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ZoomOut,
                contentDescription = "Zoom Out",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )

            Slider(
                value = scale,
                onValueChange = onScaleChange,
                valueRange = 1.0f..4.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = SystemBlue,
                    inactiveTrackColor = Color(0xFF333336)
                ),
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Outlined.ZoomIn,
                contentDescription = "Zoom In",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun TransformActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF242428),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    }
}

@Composable
fun FiltersToolPanel(
    selectedFilter: ApplePhotoFilter,
    onFilterSelect: (ApplePhotoFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(ApplePhotoFilter.entries) { filter ->
            val isSelected = selectedFilter == filter
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onFilterSelect(filter) }
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) SystemBlueLight else Color(0xFF28282D),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) SystemBlue else Color(0xFF44444A)
                    ),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (filter) {
                                ApplePhotoFilter.ORIGINAL -> "✨"
                                ApplePhotoFilter.VIVID -> "🌈"
                                ApplePhotoFilter.WARM -> "🌅"
                                ApplePhotoFilter.COOL -> "❄️"
                                ApplePhotoFilter.NOIR -> "🌑"
                                ApplePhotoFilter.SILVERTONE -> "🎞️"
                                ApplePhotoFilter.DRAMATIC -> "⚡"
                            },
                            fontSize = 18.sp
                        )
                    }
                }

                Text(
                    text = filter.displayName,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) SystemBlue else Color.White
                )
            }
        }
    }
}

@Composable
fun AdjustmentsToolPanel(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    onResetAdjustments: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Brightness Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = Icons.Outlined.Brightness6, contentDescription = "Brightness", tint = Color.White, modifier = Modifier.size(16.dp))
            Text(text = "Brightness", fontSize = 12.sp, color = Color.White, modifier = Modifier.width(70.dp))
            Slider(
                value = brightness,
                onValueChange = onBrightnessChange,
                valueRange = -40f..40f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = SystemBlue, inactiveTrackColor = Color(0xFF333336)),
                modifier = Modifier.weight(1f)
            )
        }

        // Contrast Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = Icons.Outlined.Contrast, contentDescription = "Contrast", tint = Color.White, modifier = Modifier.size(16.dp))
            Text(text = "Contrast", fontSize = 12.sp, color = Color.White, modifier = Modifier.width(70.dp))
            Slider(
                value = contrast,
                onValueChange = onContrastChange,
                valueRange = 0.6f..1.4f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = SystemBlue, inactiveTrackColor = Color(0xFF333336)),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun EditorTabButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) SystemBlue else Color(0xFF8E8E93),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) SystemBlue else Color(0xFF8E8E93)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Off-Screen Render & Output Engine
// -------------------------------------------------------------------------------------------------

fun createAdjustedColorMatrix(
    filter: ApplePhotoFilter,
    brightness: Float,
    contrast: Float
): ColorMatrix {
    val matrix = ColorMatrix()
    matrix.set(filter.colorMatrix)

    if (brightness != 0f || contrast != 1f) {
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        matrix.timesAssign(contrastMatrix)
    }

    return matrix
}

suspend fun renderAndSaveCroppedAvatar(
    context: Context,
    sourceBitmap: Bitmap,
    scale: Float,
    offset: Offset,
    rotationDegrees: Float,
    isFlipped: Boolean,
    filter: ApplePhotoFilter,
    brightness: Float,
    contrast: Float,
    outputSize: Int = 512
): String = withContext(Dispatchers.IO) {
    val outputBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(outputBitmap)

    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)

    // Apply color filter to paint
    val cm = createAdjustedColorMatrix(filter, brightness, contrast)
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm.values)

    val matrix = android.graphics.Matrix()

    // 1. Center source bitmap at (0,0)
    val srcCenterX = sourceBitmap.width / 2f
    val srcCenterY = sourceBitmap.height / 2f
    matrix.postTranslate(-srcCenterX, -srcCenterY)

    // 2. Apply horizontal flip around origin
    if (isFlipped) {
        matrix.postScale(-1f, 1f)
    }

    // 3. Apply rotation around origin
    matrix.postRotate(rotationDegrees)

    // 4. Calculate rotated dimensions and scale
    val isRotated90or270 = (rotationDegrees % 180f != 0f)
    val rotatedSrcWidth = if (isRotated90or270) sourceBitmap.height.toFloat() else sourceBitmap.width.toFloat()
    val rotatedSrcHeight = if (isRotated90or270) sourceBitmap.width.toFloat() else sourceBitmap.height.toFloat()

    val minRotatedDim = minOf(rotatedSrcWidth, rotatedSrcHeight)
    val baseScale = outputSize.toFloat() / minRotatedDim
    val effectiveScale = baseScale * scale
    matrix.postScale(effectiveScale, effectiveScale)

    // 5. Apply clamped user translation pan (mapped from 280dp circle viewport to 512px output canvas)
    val currentRenderWidth = rotatedSrcWidth * effectiveScale
    val currentRenderHeight = rotatedSrcHeight * effectiveScale
    val outRadius = outputSize / 2f
    val maxPanX = maxOf(0f, (currentRenderWidth / 2f) - outRadius)
    val maxPanY = maxOf(0f, (currentRenderHeight / 2f) - outRadius)

    val screenCircleDiameterPx = context.resources.displayMetrics.density * 280f
    val panScaleFactor = (outputSize.toFloat() / screenCircleDiameterPx)
    val clampedOffsetX = (offset.x * panScaleFactor).coerceIn(-maxPanX, maxPanX)
    val clampedOffsetY = (offset.y * panScaleFactor).coerceIn(-maxPanY, maxPanY)

    matrix.postTranslate(
        (outputSize / 2f) + clampedOffsetX,
        (outputSize / 2f) + clampedOffsetY
    )

    canvas.drawBitmap(sourceBitmap, matrix, paint)

    // Save to private internal avatars directory
    val avatarsDir = File(context.filesDir, "avatars").apply { if (!exists()) mkdirs() }
    val avatarFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}.jpg")

    FileOutputStream(avatarFile).use { out ->
        outputBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }

    outputBitmap.recycle()

    Uri.fromFile(avatarFile).toString()
}
