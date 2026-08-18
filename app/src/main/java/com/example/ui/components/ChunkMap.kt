package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Red500
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChunkMap(
    totalChunks: Int,
    receivedIndices: Set<Int>,
    currentChunkIndex: Int? = null,
    missingChunks: List<Int> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (totalChunks <= 0) return

    val receivedCount = receivedIndices.size
    val percent = if (totalChunks > 0) (receivedCount * 100) / totalChunks else 0

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Slate900.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Matrix",
                        tint = Cyan400,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Chunk Matrix ($receivedCount / $totalChunks)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Text(
                    text = "$percent%",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (percent == 100) Emerald400 else Cyan400
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chunk grid (limit render to first 120 blocks for layout performance if huge)
            val displayCount = minOf(totalChunks, 150)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (i in 0 until displayCount) {
                    val isReceived = receivedIndices.contains(i)
                    val isCurrent = currentChunkIndex == i
                    val isMissing = missingChunks.contains(i)

                    val targetColor = when {
                        isCurrent -> Cyan400
                        isReceived -> Emerald400
                        isMissing -> Red500.copy(alpha = 0.7f)
                        else -> Slate800
                    }

                    val color by animateColorAsState(targetValue = targetColor, label = "chunk_color")

                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                            .then(
                                if (isCurrent) Modifier.border(1.dp, Color.White, RoundedCornerShape(2.dp))
                                else Modifier.border(0.5.dp, Slate700.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                            )
                    )
                }
                if (totalChunks > displayCount) {
                    Text(
                        text = "+${totalChunks - displayCount} more",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            color = Slate400
                        ),
                        modifier = Modifier.align(Alignment.CenterVertically).padding(start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendItem(color = Emerald400, label = "Received")
                LegendItem(color = Cyan400, label = "Active")
                LegendItem(color = Slate800, label = "Pending")
                if (missingChunks.isNotEmpty()) {
                    LegendItem(color = Red500, label = "Missing")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Slate400)
        )
    }
}
