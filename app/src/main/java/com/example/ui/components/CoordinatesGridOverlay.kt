package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag

@Composable
fun CoordinatesGridOverlay(
    modifier: Modifier = Modifier,
    divisions: Int = 10,
    lineColor: Color = Color(0x600284C7),
    textColor: Color = Color(0xFF0284C7)
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("coordinates_grid_overlay")
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / divisions
        val stepY = height / divisions

        // رسم خطوط X
        for (i in 0..divisions) {
            val x = i * stepX
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = if (i % 5 == 0) 2.5f else 1f
            )
        }

        // رسم خطوط Y
        for (j in 0..divisions) {
            val y = j * stepY
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = if (j % 5 == 0) 2.5f else 1f
            )
        }

        // تسمية التقاطعات بالأرقام والنسب المئوية
        val paint = android.graphics.Paint().apply {
            color = textColor.hashCode()
            textSize = 24f
            isAntiAlias = true
        }

        for (i in 1 until divisions) {
            val x = i * stepX
            drawContext.canvas.nativeCanvas.drawText(
                "${(i * (100 / divisions))}%",
                x + 4f,
                30f,
                paint
            )
        }

        for (j in 1 until divisions) {
            val y = j * stepY
            drawContext.canvas.nativeCanvas.drawText(
                "${(j * (100 / divisions))}%",
                6f,
                y - 4f,
                paint
            )
        }
    }
}
