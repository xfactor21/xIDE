package com.aistudio.xide.core.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.xide.core.designsystem.components.XideButton
import com.aistudio.xide.core.designsystem.components.XidePanel
import com.aistudio.xide.core.editor.DocumentModel

@Composable
fun EditorToolbar(
    activeDocument: DocumentModel?,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    XidePanel(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            XideButton(
                text = "Save",
                onClick = onSave,
                enabled = activeDocument?.isModified == true
            )
            XideButton(
                text = "Undo",
                onClick = onUndo,
                enabled = true 
            )
            XideButton(
                text = "Redo",
                onClick = onRedo,
                enabled = true 
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (activeDocument != null) {
                Text(text = activeDocument.languageType, modifier = Modifier.padding(horizontal = 8.dp))
                Text(text = activeDocument.encoding, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}
