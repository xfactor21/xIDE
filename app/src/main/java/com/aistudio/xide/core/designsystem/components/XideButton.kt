package com.aistudio.xide.core.designsystem.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aistudio.xide.core.designsystem.tokens.ShapeTokens
import com.aistudio.xide.core.designsystem.tokens.SpacingTokens
import com.aistudio.xide.core.designsystem.tokens.TypographyTokens

@Composable
fun XideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ShapeTokens.Medium,
        contentPadding = PaddingValues(
            horizontal = SpacingTokens.Large,
            vertical = SpacingTokens.Medium
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = text,
            style = TypographyTokens.LabelSmall
        )
    }
}
