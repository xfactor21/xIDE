package com.aistudio.xide.core.execution

import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.UUID

class LocalTerminalProvider : TerminalProvider {
    override val providerId: String = "local-terminal"
    override val providerName: String = "Local Terminal"
    override val providerVersion: String = "1.0.0"
    override val supportedFeatures: List<String> = listOf("local_shell")
    override val requirements: List<String> = emptyList()
    override val limitations: List<String> = listOf("single_machine")
    override val description: String = "Executes local shell commands."
    override val author: String = "AI Studio"
    override val compatibilityVersion: String = "1.0.0"

    private val sessions = mutableMapOf<String, TerminalSession>()
    private var health = ProviderHealth.HEALTHY

    override suspend fun initialize() {
        health = ProviderHealth.HEALTHY
    }

    override suspend fun shutdown() {
        sessions.clear()
        health = ProviderHealth.DEGRADED
    }

    override suspend fun healthCheck(): ProviderHealth {
        return health
    }

    override suspend fun createSession(workingDirectory: String): TerminalSession {
        val session = TerminalSession(
            sessionId = UUID.randomUUID().toString(),
            workingDirectory = workingDirectory,
            commandHistory = emptyList(),
            output = ""
        )
        sessions[session.sessionId] = session
        return session
    }

    override suspend fun executeCommand(sessionId: String, command: TerminalCommand): String {
        val session = sessions[sessionId] ?: throw IllegalArgumentException("Session not found")
        // Enforce permission externally via CommandSystem. Here we do the raw execution safely.
        
        return withContext(Dispatchers.IO) {
            try {
                val pb = ProcessBuilder()
                pb.command(listOf(command.command) + command.arguments)
                pb.directory(File(session.workingDirectory))
                pb.environment().putAll(command.environment)
                
                val process = pb.start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                while (errorReader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                
                process.waitFor()
                
                val updatedSession = session.copy(
                    commandHistory = session.commandHistory + command.command,
                    output = session.output + output.toString()
                )
                sessions[sessionId] = updatedSession
                
                output.toString()
            } catch (e: Exception) {
                health = ProviderHealth.DEGRADED
                throw e
            }
        }
    }

    override suspend fun closeSession(sessionId: String) {
        sessions.remove(sessionId)
    }
}
