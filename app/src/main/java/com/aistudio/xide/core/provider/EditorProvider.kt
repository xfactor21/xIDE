package com.aistudio.xide.core.provider

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for the text editing surface and workspace editor component.
 */
interface EditorProvider : XideProvider {
    /**
     * The current text content of the editor.
     */
    val contentFlow: StateFlow<String>
    
    suspend fun openFile(filePath: String)
    suspend fun saveFile()
    suspend fun insertText(text: String, position: Int)
    suspend fun replaceText(start: Int, end: Int, text: String)
}
