package com.aistudio.xide.core.editor

import com.aistudio.xide.core.events.PlatformEvent
import java.util.UUID

sealed class EditorEvent : PlatformEvent {
    override val eventId: String = UUID.randomUUID().toString()
    override val timestamp: Long = System.currentTimeMillis()

    data class DocumentOpened(val documentId: String, val filePath: String) : EditorEvent()
    data class DocumentChanged(val documentId: String, val version: Long) : EditorEvent()
    data class DocumentSaved(val documentId: String) : EditorEvent()
    data class DocumentClosed(val documentId: String) : EditorEvent()
    data class CursorMoved(val documentId: String, val position: CursorPosition) : EditorEvent()
    data class SelectionChanged(val documentId: String, val range: SelectionRange) : EditorEvent()
}
