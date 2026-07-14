package com.aistudio.xide.core.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorContextTest {
    @Test
    fun testEditorContextInitialization() {
        val context = ActiveEditorContext(filePath = "/test/file.kt")
        assertEquals("/test/file.kt", context.filePath)
        assertEquals(1, context.cursorLocation.line)
        assertEquals(1, context.cursorLocation.column)
        assertNull(context.selectedText)
    }

    @Test
    fun testEditorContextUpdates() {
        val context = ActiveEditorContext(
            filePath = "/test/file.kt",
            cursorLocation = CursorPosition(10, 5),
            selectedText = "class",
            currentFileSymbol = "MyClass",
            currentFunction = "myFun",
            currentClass = "MyClass",
            selectedSymbol = "class",
            surroundingCodeContext = "class MyClass { fun myFun() {} }"
        )
        
        assertEquals(10, context.cursorLocation.line)
        assertEquals("class", context.selectedText)
        assertEquals("MyClass", context.currentClass)
        assertEquals("myFun", context.currentFunction)
        assertEquals("class MyClass { fun myFun() {} }", context.surroundingCodeContext)
    }
}
