package com.aistudio.xide.core.editor.ui

import androidx.compose.ui.text.AnnotatedString

/**
 * Abstraction for syntax highlighting.
 * Responsibilities:
 * - Detect language
 * - Produce tokens
 * - Provide styling information
 */
interface SyntaxHighlightProvider {
    fun detectLanguage(filePath: String, content: String): String
    fun highlight(content: String, language: String): AnnotatedString
}

/**
 * Default implementation: Plain text renderer.
 */
class PlainTextHighlightProvider : SyntaxHighlightProvider {
    override fun detectLanguage(filePath: String, content: String): String {
        return "text/plain"
    }

    override fun highlight(content: String, language: String): AnnotatedString {
        return AnnotatedString(content)
    }
}
