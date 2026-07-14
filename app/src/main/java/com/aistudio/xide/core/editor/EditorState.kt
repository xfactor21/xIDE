package com.aistudio.xide.core.editor

data class EditorState(
    val activeSession: EditorSession?,
    val isReady: Boolean = false
)
