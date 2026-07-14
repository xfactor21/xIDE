package com.aistudio.xide.core.workspace.commands

import com.aistudio.xide.core.command.Command
import com.aistudio.xide.core.command.CommandResult
import java.util.UUID

data class CreateProjectCommand(
    val projectName: String,
    val path: String
) : Command {
    override val id: String = "workspace.createProject"
    override val name: String = "Create Project"
    override val description: String = "Creates a new workspace project."
    override val canUndo: Boolean = true

    override suspend fun execute(): CommandResult = CommandResult.Success
    override suspend fun undo(): CommandResult = CommandResult.Success
}

data class OpenProjectCommand(
    val projectId: String
) : Command {
    override val id: String = "workspace.openProject"
    override val name: String = "Open Project"
    override val description: String = "Opens an existing workspace project."
    override val canUndo: Boolean = false

    override suspend fun execute(): CommandResult = CommandResult.Success
    override suspend fun undo(): CommandResult = CommandResult.Success
}

data class CloseProjectCommand(
    val projectId: String
) : Command {
    override val id: String = "workspace.closeProject"
    override val name: String = "Close Project"
    override val description: String = "Closes the current workspace project."
    override val canUndo: Boolean = false

    override suspend fun execute(): CommandResult = CommandResult.Success
    override suspend fun undo(): CommandResult = CommandResult.Success
}

data class DeleteProjectCommand(
    val projectId: String
) : Command {
    override val id: String = "workspace.deleteProject"
    override val name: String = "Delete Project"
    override val description: String = "Deletes a workspace project."
    override val canUndo: Boolean = false

    override suspend fun execute(): CommandResult = CommandResult.Success
    override suspend fun undo(): CommandResult = CommandResult.Success
}

data class RestoreWorkspaceCommand(
    val projectId: String
) : Command {
    override val id: String = "workspace.restoreWorkspace"
    override val name: String = "Restore Workspace"
    override val description: String = "Restores a workspace session."
    override val canUndo: Boolean = false

    override suspend fun execute(): CommandResult = CommandResult.Success
    override suspend fun undo(): CommandResult = CommandResult.Success
}
