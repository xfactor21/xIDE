package com.aistudio.xide.core.editor

/**
 * Document abstraction supporting identity, file path, language type, content state,
 * modified state, version tracking, save state, and encoding information.
 */
data class DocumentModel(
    val id: String,
    val filePath: String,
    val languageType: String,
    val isModified: Boolean = false,
    val version: Long = 0L,
    val encoding: String = "UTF-8",
    val buffer: TextBuffer = SimpleTextBuffer()
)
