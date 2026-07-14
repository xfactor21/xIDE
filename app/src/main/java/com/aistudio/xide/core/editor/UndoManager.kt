package com.aistudio.xide.core.editor

interface UndoManager {
    fun recordChange(change: EditorChange)
    fun undo(): EditorChange?
    fun redo(): EditorChange?
    fun clear()
    
    val canUndo: Boolean
    val canRedo: Boolean
}

data class EditorChange(
    val range: SelectionRange,
    val oldText: String,
    val newText: String
)
