package com.aistudio.xide.core.editor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aistudio.xide.core.editor.DocumentModel

@Composable
fun EditorViewport(
    document: DocumentModel?,
    content: String,
    onContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (document == null) {
        // Empty state
        return
    }

    BasicTextField(
        value = content,
        onValueChange = onContentChanged,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = MaterialTheme.typography.bodyMedium.fontSize
        )
    )
}
