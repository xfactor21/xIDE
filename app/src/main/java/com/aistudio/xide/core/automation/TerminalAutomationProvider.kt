package com.aistudio.xide.core.automation

/**
 * Terminal automation provider contract.
 * Represents command line interaction without invoking raw shell systems in Phase 12.
 */
interface TerminalAutomationProvider : AutomationProvider {
    suspend fun executeBuildCommand(projectId: String, buildType: String): AutomationResult
    suspend fun executeArbitraryCommand(command: String): AutomationResult
}
