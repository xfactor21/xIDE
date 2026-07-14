package com.aistudio.xide.core.editor.ui

import com.aistudio.xide.core.editor.DocumentModel
import com.aistudio.xide.core.editor.EditorState

data class EditorUiState(
    val isReady: Boolean = false,
    val openDocuments: List<DocumentModel> = emptyList(),
    val activeDocumentId: String? = null,
    val activeContent: String = "",
    val activeLanguage: String = "text/plain"
) {
    val activeDocument: DocumentModel?
        get() = openDocuments.find { it.id == activeDocumentId }
}
