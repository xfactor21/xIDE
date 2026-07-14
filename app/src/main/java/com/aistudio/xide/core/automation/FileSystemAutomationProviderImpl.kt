package com.aistudio.xide.core.automation

import com.aistudio.xide.core.provider.ProviderHealth

/**
 * Concrete implementation representing the file system action boundary.
 */
class FileSystemAutomationProviderImpl : FileSystemAutomationProvider {
    override val providerId: String = "file_system_automation_provider"
    override val providerName: String = "File System Automation Provider"
    override val providerVersion: String = "1.0"
    override val supportedFeatures: List<String> = listOf("create", "read", "update", "delete")
    override val requirements: List<String> = emptyList()
    override val limitations: List<String> = listOf("Supports workspace operations only")
    override val description: String = "Provides controlled file system manipulations under strict permission boundaries"
    override val author: String = "xIDE Platform Team"
    override val compatibilityVersion: String = "1.0"

    override suspend fun initialize() {}
    override suspend fun shutdown() {}
    override suspend fun healthCheck(): ProviderHealth = ProviderHealth.HEALTHY

    override fun canExecute(action: AutomationAction): Boolean {
        return action is AutomationAction.CreateFile || action is AutomationAction.ModifyFile
    }

    override suspend fun executeAction(action: AutomationAction): AutomationResult {
        return when (action) {
            is AutomationAction.CreateFile -> create(action.path, action.content)
            is AutomationAction.ModifyFile -> update(action.path, action.targetContent, action.replacementContent)
            else -> AutomationResult(success = false, message = "Unsupported action type: ${action::class.simpleName}")
        }
    }

    override suspend fun create(path: String, content: String): AutomationResult {
        return try {
            val file = java.io.File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            AutomationResult(
                success = true,
                message = "File successfully created at path '$path'.",
                metadata = mapOf("action" to "create", "path" to path, "bytes" to content.length.toString())
            )
        } catch (e: Exception) {
            AutomationResult(
                success = false,
                message = "Failed to create file at path '$path': ${e.message}",
                diagnostics = listOf(e.stackTraceToString())
            )
        }
    }

    override suspend fun read(path: String): AutomationResult {
        return try {
            val file = java.io.File(path)
            if (!file.exists()) {
                return AutomationResult(
                    success = false,
                    message = "File not found at path '$path'."
                )
            }
            val content = file.readText()
            AutomationResult(
                success = true,
                message = "File successfully read from path '$path'.",
                metadata = mapOf("action" to "read", "path" to path, "bytes" to content.length.toString())
            )
        } catch (e: Exception) {
            AutomationResult(
                success = false,
                message = "Failed to read file from path '$path': ${e.message}",
                diagnostics = listOf(e.stackTraceToString())
            )
        }
    }

    override suspend fun update(path: String, targetContent: String, replacementContent: String): AutomationResult {
        return try {
            val file = java.io.File(path)
            if (!file.exists()) {
                return AutomationResult(
                    success = false,
                    message = "File not found at path '$path'."
                )
            }
            val currentContent = file.readText()
            val newContent = if (targetContent.isEmpty()) {
                replacementContent
            } else {
                if (!currentContent.contains(targetContent)) {
                    return AutomationResult(
                        success = false,
                        message = "Target content not found in file '$path'."
                    )
                }
                currentContent.replace(targetContent, replacementContent)
            }
            file.writeText(newContent)
            AutomationResult(
                success = true,
                message = "File successfully updated at path '$path'.",
                metadata = mapOf("action" to "update", "path" to path, "bytes" to newContent.length.toString())
            )
        } catch (e: Exception) {
            AutomationResult(
                success = false,
                message = "Failed to update file at path '$path': ${e.message}",
                diagnostics = listOf(e.stackTraceToString())
            )
        }
    }

    override suspend fun delete(path: String): AutomationResult {
        return try {
            val file = java.io.File(path)
            if (!file.exists()) {
                return AutomationResult(
                    success = false,
                    message = "File not found at path '$path'."
                )
            }
            val deleted = file.delete()
            if (deleted) {
                AutomationResult(
                    success = true,
                    message = "File successfully deleted at path '$path'.",
                    metadata = mapOf("action" to "delete", "path" to path)
                )
            } else {
                AutomationResult(
                    success = false,
                    message = "Failed to delete file at path '$path'."
                )
            }
        } catch (e: Exception) {
            AutomationResult(
                success = false,
                message = "Failed to delete file at path '$path': ${e.message}",
                diagnostics = listOf(e.stackTraceToString())
            )
        }
    }
}
