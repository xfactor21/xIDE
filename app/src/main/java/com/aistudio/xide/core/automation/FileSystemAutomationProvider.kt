package com.aistudio.xide.core.automation

/**
 * File system automation provider contract.
 * Represents standard file mutations under a unified, mock-free interface.
 */
interface FileSystemAutomationProvider : AutomationProvider {
    suspend fun create(path: String, content: String): AutomationResult
    suspend fun read(path: String): AutomationResult
    suspend fun update(path: String, targetContent: String, replacementContent: String): AutomationResult
    suspend fun delete(path: String): AutomationResult
}
