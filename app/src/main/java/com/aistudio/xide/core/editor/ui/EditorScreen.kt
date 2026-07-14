package com.aistudio.xide.core.editor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aistudio.xide.core.designsystem.layout.WindowSizeClass

@Composable
fun EditorScreen(
    uiState: EditorUiState,
    windowSizeClass: WindowSizeClass,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        EditorTabs(
            openDocuments = uiState.openDocuments,
            activeDocumentId = uiState.activeDocumentId,
            onTabSelected = onTabSelected,
            onTabClosed = onTabClosed
        )

        EditorToolbar(
            activeDocument = uiState.activeDocument,
            onSave = onSave,
            onUndo = onUndo,
            onRedo = onRedo
        )

        EditorViewport(
            document = uiState.activeDocument,
            content = uiState.activeContent,
            onContentChanged = onContentChanged,
            modifier = Modifier.weight(1f)
        )
    }
}
