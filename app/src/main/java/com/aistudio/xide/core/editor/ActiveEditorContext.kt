package com.aistudio.xide.core.editor

data class ActiveEditorContext(
    val filePath: String?,
    val cursorLocation: CursorPosition = CursorPosition(1, 1),
    val selectedText: String? = null,
    val currentFileSymbol: String? = null,
    val currentFunction: String? = null,
    val currentClass: String? = null,
    val selectedSymbol: String? = null,
    val surroundingCodeContext: String? = null,
    val relatedSymbols: List<String> = emptyList(),
    val diagnosticsAtCursor: List<com.aistudio.xide.core.diagnostics.BuildDiagnostic> = emptyList()
)
