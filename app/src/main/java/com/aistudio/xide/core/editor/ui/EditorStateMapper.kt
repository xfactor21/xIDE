package com.aistudio.xide.core.editor.ui

import com.aistudio.xide.core.editor.EditorState

object EditorStateMapper {
    fun mapToUiState(editorState: EditorState, activeContent: String): EditorUiState {
        val session = editorState.activeSession
        return EditorUiState(
            isReady = editorState.isReady,
            openDocuments = session?.openDocuments ?: emptyList(),
            activeDocumentId = session?.activeDocumentId,
            activeContent = activeContent,
            activeLanguage = session?.openDocuments?.find { it.id == session.activeDocumentId }?.languageType ?: "text/plain"
        )
    }
}
