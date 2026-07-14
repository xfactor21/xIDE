package com.aistudio.xide.core.editor

interface EditorRepository {
    suspend fun loadDocument(filePath: String): DocumentModel
    suspend fun saveDocument(document: DocumentModel): Boolean
    suspend fun closeDocument(documentId: String)
}
