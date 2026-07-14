package com.aistudio.xide.core.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.xide.core.designsystem.tokens.SpacingTokens
import com.aistudio.xide.core.editor.DocumentModel

@Composable
fun EditorTabs(
    openDocuments: List<DocumentModel>,
    activeDocumentId: String?,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(rememberScrollState())
    ) {
        openDocuments.forEach { document ->
            val isActive = document.id == activeDocumentId
            val backgroundColor = if (isActive) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }

            Row(
                modifier = Modifier
                    .background(backgroundColor)
                    .clickable { onTabSelected(document.id) }
                    .padding(horizontal = SpacingTokens.Medium, vertical = SpacingTokens.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = document.filePath.substringAfterLast("/") + (if (document.isModified) " *" else ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = " x",
                    modifier = Modifier
                        .clickable { onTabClosed(document.id) }
                        .padding(start = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
