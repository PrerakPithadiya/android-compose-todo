package com.example.todo_list.ui.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Authentic Apple iOS Face ID vector icon conforming to Apple Human Interface Guidelines.
 * Features 4 corner boundary brackets, facial eyes, nose bracket, and friendly curve.
 */
val IosFaceIdIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "IosFaceId",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 4 Corner Brackets
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Top-Left
            moveTo(3f, 9f)
            lineTo(3f, 6.5f)
            curveTo(3f, 4.5f, 4.5f, 3f, 6.5f, 3f)
            lineTo(9f, 3f)

            // Top-Right
            moveTo(15f, 3f)
            lineTo(17.5f, 3f)
            curveTo(19.5f, 3f, 21f, 4.5f, 21f, 6.5f)
            lineTo(21f, 9f)

            // Bottom-Left
            moveTo(3f, 15f)
            lineTo(3f, 17.5f)
            curveTo(3f, 19.5f, 4.5f, 21f, 6.5f, 21f)
            lineTo(9f, 21f)

            // Bottom-Right
            moveTo(15f, 21f)
            lineTo(17.5f, 21f)
            curveTo(19.5f, 21f, 21f, 19.5f, 21f, 17.5f)
            lineTo(21f, 15f)
        }

        // Facial landmarks: Eyes, Nose, Smile
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Left Eye
            moveTo(8.5f, 8.5f)
            lineTo(8.5f, 11f)

            // Right Eye
            moveTo(15.5f, 8.5f)
            lineTo(15.5f, 11f)

            // Nose
            moveTo(12f, 10.5f)
            lineTo(12f, 14f)
            lineTo(13.5f, 14f)

            // Smile
            moveTo(8.5f, 16.5f)
            quadTo(12f, 19f, 15.5f, 16.5f)
        }
    }.build()
}
