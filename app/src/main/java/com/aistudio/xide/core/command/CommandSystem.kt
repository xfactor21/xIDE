package com.aistudio.xide.core.command

import kotlinx.coroutines.flow.Flow

/**
 * Unified execution architecture.
 */
interface CommandSystem {
    suspend fun execute(command: Command): CommandResult
    suspend fun undoLast()
    suspend fun redoLast()
    val commandHistory: Flow<List<Command>>
}

interface Command {
    val id: String
    val name: String
    val description: String
    
    suspend fun execute(): CommandResult
    suspend fun undo(): CommandResult
    val canUndo: Boolean
}

sealed class CommandResult {
    object Success : CommandResult()
    data class Failure(val reason: String, val error: Throwable?) : CommandResult()
}
