package com.aistudio.xide.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aistudio.xide.core.designsystem.tokens.ShapeTokens
import com.aistudio.xide.core.designsystem.tokens.SpacingTokens
import com.aistudio.xide.core.designsystem.tokens.TypographyTokens

enum class IndicatorStatus {
    IDLE, BUSY, SUCCESS, ERROR, WARNING
}

@Composable
fun XideStatusIndicator(
    status: IndicatorStatus,
    message: String,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        IndicatorStatus.IDLE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        IndicatorStatus.BUSY -> MaterialTheme.colorScheme.primary
        IndicatorStatus.SUCCESS -> Color(0xFF4CAF50)
        IndicatorStatus.ERROR -> MaterialTheme.colorScheme.error
        IndicatorStatus.WARNING -> Color(0xFFFFC107)
    }

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, ShapeTokens.ExtraSmall)
            .padding(horizontal = SpacingTokens.Medium, vertical = SpacingTokens.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (status == IndicatorStatus.BUSY) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = color,
                strokeWidth = 2.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, ShapeTokens.Full)
            )
        }

        Spacer(modifier = Modifier.width(SpacingTokens.Small))

        Text(
            text = message,
            style = TypographyTokens.LabelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
