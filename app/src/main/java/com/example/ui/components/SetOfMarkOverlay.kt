package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SomBadgeColor

data class SomElement(
    val id: Int,
    val label: String,
    val relX: Float, // 0.0 .. 1.0
    val relY: Float, // 0.0 .. 1.0
    val widthDp: Int = 40,
    val heightDp: Int = 40
)

@Composable
fun SetOfMarkOverlay(
    modifier: Modifier = Modifier,
    elements: List<SomElement> = defaultMockSomElements
) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("set_of_mark_overlay")
    ) {
        val totalWidthPx = constraints.maxWidth
        val totalHeightPx = constraints.maxHeight

        elements.forEach { elem ->
            val posX = (elem.relX * totalWidthPx).toInt()
            val posY = (elem.relY * totalHeightPx).toInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(posX, posY) }
                    .border(1.5.dp, SomBadgeColor.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(2.dp)
            ) {
                // شارة الرقم SoM الدائرية
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(SomBadgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = elem.id.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

val defaultMockSomElements = listOf(
    SomElement(1, "Menu Bar", 0.08f, 0.07f),
    SomElement(2, "Search Input", 0.35f, 0.07f),
    SomElement(3, "Action Button", 0.85f, 0.07f),
    SomElement(4, "Card Header", 0.20f, 0.25f),
    SomElement(5, "Switch Toggle", 0.80f, 0.25f),
    SomElement(6, "Item Select", 0.45f, 0.45f),
    SomElement(7, "Send Icon", 0.88f, 0.90f)
)
