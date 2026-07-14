package com.aistudio.xide.core.editor

import com.aistudio.xide.core.command.Command
import com.aistudio.xide.core.command.CommandResult

sealed class EditorCommand : Command {
    override val canUndo: Boolean = true
    
    data class InsertTextCommand(
        val documentId: String,
        val position: CursorPosition,
        val text: String
    ) : EditorCommand() {
        override val id: String = "editor.insertText"
        override val name: String = "Insert Text"
        override val description: String = "Inserts text at the specified cursor position."
        
        override suspend fun execute(): CommandResult = CommandResult.Success
        override suspend fun undo(): CommandResult = CommandResult.Success
    }

    data class DeleteTextCommand(
        val documentId: String,
        val range: SelectionRange
    ) : EditorCommand() {
        override val id: String = "editor.deleteText"
        override val name: String = "Delete Text"
        override val description: String = "Deletes text in the specified range."
        
        override suspend fun execute(): CommandResult = CommandResult.Success
        override suspend fun undo(): CommandResult = CommandResult.Success
    }

    data class ReplaceTextCommand(
        val documentId: String,
        val range: SelectionRange,
        val text: String
    ) : EditorCommand() {
        override val id: String = "editor.replaceText"
        override val name: String = "Replace Text"
        override val description: String = "Replaces text in the specified range."
        
        override suspend fun execute(): CommandResult = CommandResult.Success
        override suspend fun undo(): CommandResult = CommandResult.Success
    }

    data class SaveDocumentCommand(
        val documentId: String
    ) : EditorCommand() {
        override val id: String = "editor.saveDocument"
        override val name: String = "Save Document"
        override val description: String = "Saves the specified document."
        override val canUndo: Boolean = false
        
        override suspend fun execute(): CommandResult = CommandResult.Success
        override suspend fun undo(): CommandResult = CommandResult.Success
    }
}
