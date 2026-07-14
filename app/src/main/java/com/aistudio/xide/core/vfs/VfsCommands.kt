package com.aistudio.xide.core.vfs

import com.aistudio.xide.core.command.Command
import com.aistudio.xide.core.command.CommandResult

data class CreateFolderCommand(val path: String) : Command {
    override val id: String = "vfs.createFolder"
    override val name: String = "Create Folder"
    override val description: String = "Creates a new folder."
    override val canUndo: Boolean = true
    override suspend fun execute(): CommandResult = CommandResult.Success
    override suspend fun undo(): CommandResult = CommandResult.Success
}

data class DeleteFolderCommand(val path: String) : Command {
    override val id: String = "vfs.deleteFolder"
    override val name: String = "Delete Folder"
    override val description: String = "Deletes a folder."
    override val canUndo: Boolean = true
    override suspend fun execute(): CommandResult = CommandResult.Success
    override suspend fun undo(): CommandResult = CommandResult.Success
}

data class MoveFileCommand(val sourcePath: String, val destinationPath: String) : Command {
    override val id: String = "vfs.moveFile"
    override val name: String = "Move File"
    override val description: String = "Moves a file or folder."
    override val canUndo: Boolean = true
    override suspend fun execute(): CommandResult = CommandResult.Success
    override suspend fun undo(): CommandResult = CommandResult.Success
}

data class CopyFileCommand(val sourcePath: String, val destinationPath: String) : Command {
    override val id: String = "vfs.copyFile"
    override val name: String = "Copy File"
    override val description: String = "Copies a file or folder."
    override val canUndo: Boolean = true
    override suspend fun execute(): CommandResult = CommandResult.Success
    override suspend fun undo(): CommandResult = CommandResult.Success
}

data class RenameFileCommand(val oldPath: String, val newName: String) : Command {
    override val id: String = "vfs.renameFile"
    override val name: String = "Rename File"
    override val description: String = "Renames a file or folder."
    override val canUndo: Boolean = true
    override suspend fun execute(): CommandResult = CommandResult.Success
    override suspend fun undo(): CommandResult = CommandResult.Success
}
