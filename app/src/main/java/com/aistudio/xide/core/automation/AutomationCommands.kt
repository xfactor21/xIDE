package com.aistudio.xide.core.automation

import com.aistudio.xide.core.command.Command
import com.aistudio.xide.core.command.CommandResult
import java.util.UUID

sealed class AutomationCommand : Command {
    override val id: String = UUID.randomUUID().toString()
    override val canUndo: Boolean = false
    
    override suspend fun execute(): CommandResult = CommandResult.Success
    override suspend fun undo(): CommandResult = CommandResult.Failure("Cannot undo automation command", null)

    data class BuildProjectCommand(val projectId: String, val buildType: String) : AutomationCommand() {
        override val name: String = "Build Project"
        override val description: String = "Starts a project build process"
    }

    data class CancelTaskCommand(val taskId: String) : AutomationCommand() {
        override val name: String = "Cancel Task"
        override val description: String = "Cancels a running or queued background task"
    }

    data class InstallPackageCommand(val packageName: String, val version: String) : AutomationCommand() {
        override val name: String = "Install Package"
        override val description: String = "Installs a package dependency"
    }

    data class RunTerminalCommand(val sessionId: String, val command: String) : AutomationCommand() {
        override val name: String = "Run Terminal Command"
        override val description: String = "Executes a command in the terminal session"
    }
}
