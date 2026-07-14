package com.aistudio.xide.core.editor

import com.aistudio.xide.core.command.CommandResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditorArchitectureTest {

    @Test
    fun testCursorPositionComparable() {
        val pos1 = CursorPosition(1, 5)
        val pos2 = CursorPosition(1, 10)
        val pos3 = CursorPosition(2, 0)
        
        assertTrue(pos1 < pos2)
        assertTrue(pos2 < pos3)
        assertEquals(pos1, CursorPosition(1, 5))
    }

    @Test
    fun testSelectionRange() {
        val start = CursorPosition(1, 5)
        val end = CursorPosition(1, 10)
        val range = SelectionRange(start, end)
        
        assertFalse(range.isEmpty)
        
        val emptyRange = SelectionRange(start, start)
        assertTrue(emptyRange.isEmpty)
        
        val invertedRange = SelectionRange(end, start)
        val normalized = invertedRange.normalize()
        assertEquals(start, normalized.start)
        assertEquals(end, normalized.end)
    }

    @Test
    fun testEditorCommands() = runBlocking {
        val insertCmd = EditorCommand.InsertTextCommand("doc1", CursorPosition(1, 0), "text")
        assertTrue(insertCmd.canUndo)
        assertEquals(CommandResult.Success, insertCmd.execute())
        assertEquals(CommandResult.Success, insertCmd.undo())
        
        val saveCmd = EditorCommand.SaveDocumentCommand("doc1")
        assertFalse(saveCmd.canUndo)
    }

    @Test
    fun testTextBuffer() {
        val buffer = SimpleTextBuffer("Hello\nWorld")
        assertEquals(11, buffer.length)
        assertEquals(2, buffer.lineCount)
        assertEquals("Hello\nWorld", buffer.getText())
    }
}
