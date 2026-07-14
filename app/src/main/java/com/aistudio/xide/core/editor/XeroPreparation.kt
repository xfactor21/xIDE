package com.aistudio.xide.core.editor

/**
 * Integration points for future Xero abilities: explain code, generate code,
 * refactor code, find errors, understand project context.
 */
interface EditorXeroHook {
    suspend fun explainCode(documentId: String, range: SelectionRange): String
    suspend fun generateCode(documentId: String, prompt: String, position: CursorPosition): EditorCommand
    suspend fun refactorCode(documentId: String, range: SelectionRange, instruction: String): EditorCommand
    suspend fun findErrors(documentId: String): List<String>
}
