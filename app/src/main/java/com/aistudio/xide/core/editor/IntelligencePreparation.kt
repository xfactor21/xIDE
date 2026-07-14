package com.aistudio.xide.core.editor

/**
 * Extension points for future code indexing, symbol extraction, dependency awareness,
 * AI context generation, and error intelligence.
 */
interface EditorIntelligenceHook {
    fun onDocumentOpened(document: DocumentModel)
    fun onContentChanged(document: DocumentModel, change: EditorChange)
    fun requestContext(documentId: String, range: SelectionRange): String
}
