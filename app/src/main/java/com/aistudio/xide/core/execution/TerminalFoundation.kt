package com.aistudio.xide.core.execution

import com.aistudio.xide.core.provider.XideProvider

data class TerminalSession(
    val sessionId: String,
    val workingDirectory: String,
    val commandHistory: List<String>,
    val output: String
)

data class TerminalCommand(
    val command: String,
    val arguments: List<String>,
    val environment: Map<String, String>
)

interface TerminalProvider : XideProvider {
    suspend fun createSession(workingDirectory: String): TerminalSession
    suspend fun executeCommand(sessionId: String, command: TerminalCommand): String
    suspend fun closeSession(sessionId: String)
}
