package com.aistudio.xide.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aistudio.xide.core.designsystem.tokens.ElevationTokens
import com.aistudio.xide.core.designsystem.tokens.ShapeTokens
import com.aistudio.xide.core.designsystem.tokens.SpacingTokens

@Composable
fun XideCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = ShapeTokens.Large,
        elevation = CardDefaults.cardElevation(defaultElevation = ElevationTokens.Level1),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(SpacingTokens.Medium),
            content = content
        )
    }
}
