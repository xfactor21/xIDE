package com.aistudio.xide.core.editor

data class EditorSession(
    val sessionId: String,
    val openDocuments: List<DocumentModel>,
    val activeDocumentId: String?
)
