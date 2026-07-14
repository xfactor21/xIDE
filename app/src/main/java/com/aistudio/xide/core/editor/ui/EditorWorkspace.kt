package com.aistudio.xide.core.editor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aistudio.xide.core.designsystem.layout.WindowSizeClass

@Composable
fun EditorWorkspace(
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
    Box(modifier = modifier.fillMaxSize()) {
        EditorScreen(
            uiState = uiState,
            windowSizeClass = windowSizeClass,
            onTabSelected = onTabSelected,
            onTabClosed = onTabClosed,
            onContentChanged = onContentChanged,
            onSave = onSave,
            onUndo = onUndo,
            onRedo = onRedo
        )
        
        // Extension points for future panels:
        // - Terminal
        // - Problems panel
        // - Xero assistant
        // - Project explorer
    }
}
