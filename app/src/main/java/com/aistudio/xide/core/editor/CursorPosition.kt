package com.aistudio.xide.core.editor

data class CursorPosition(
    val line: Int,
    val column: Int
) : Comparable<CursorPosition> {
    override fun compareTo(other: CursorPosition): Int {
        if (this.line != other.line) {
            return this.line.compareTo(other.line)
        }
        return this.column.compareTo(other.column)
    }
}
