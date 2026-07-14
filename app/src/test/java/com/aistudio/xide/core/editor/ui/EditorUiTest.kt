package com.aistudio.xide.core.editor.ui

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.aistudio.xide.core.editor.EditorState
import com.aistudio.xide.core.editor.EditorSession
import com.aistudio.xide.core.editor.DocumentModel

@RunWith(RobolectricTestRunner::class)
class EditorUiTest {

    @Test
    fun testEditorStateMapper() {
        val doc1 = DocumentModel(id = "doc1", filePath = "/src/main.kt", languageType = "kotlin")
        val session = EditorSession(
            sessionId = "session1",
            openDocuments = listOf(doc1),
            activeDocumentId = "doc1"
        )
        val state = EditorState(activeSession = session, isReady = true)

        val uiState = EditorStateMapper.mapToUiState(state, "fun main() {}")

        assertTrue(uiState.isReady)
        assertEquals(1, uiState.openDocuments.size)
        assertEquals("doc1", uiState.activeDocumentId)
        assertEquals("fun main() {}", uiState.activeContent)
        assertEquals("kotlin", uiState.activeLanguage)
        assertEquals(doc1, uiState.activeDocument)
    }

    @Test
    fun testSyntaxHighlightProvider() {
        val provider = PlainTextHighlightProvider()
        assertEquals("text/plain", provider.detectLanguage("/file.kt", "fun main()"))
        assertEquals("fun main()", provider.highlight("fun main()", "text/plain").text)
    }
}
