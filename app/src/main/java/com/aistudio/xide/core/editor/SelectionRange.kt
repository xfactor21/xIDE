package com.aistudio.xide.core.editor

data class SelectionRange(
    val start: CursorPosition,
    val end: CursorPosition
) {
    val isEmpty: Boolean
        get() = start == end
        
    fun normalize(): SelectionRange {
        return if (start > end) SelectionRange(end, start) else this
    }
}
