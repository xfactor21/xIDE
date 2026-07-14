package com.aistudio.xide.core.plugin

import com.aistudio.xide.core.command.Command
import com.aistudio.xide.core.command.CommandResult
import java.util.UUID

sealed class PluginCommand : Command {
    override val id: String = UUID.randomUUID().toString()
    override val canUndo: Boolean = false
    
    override suspend fun execute(): CommandResult = CommandResult.Success
    override suspend fun undo(): CommandResult = CommandResult.Failure("Cannot undo plugin command", null)

    data class InstallPluginCommand(val location: String) : PluginCommand() {
        override val name: String = "Install Plugin"
        override val description: String = "Installs a plugin from the specified location"
    }

    data class EnablePluginCommand(val pluginId: String) : PluginCommand() {
        override val name: String = "Enable Plugin"
        override val description: String = "Enables an installed plugin"
    }

    data class DisablePluginCommand(val pluginId: String) : PluginCommand() {
        override val name: String = "Disable Plugin"
        override val description: String = "Disables an active plugin"
    }

    data class RemovePluginCommand(val pluginId: String) : PluginCommand() {
        override val name: String = "Remove Plugin"
        override val description: String = "Removes a plugin from the system"
    }
}
