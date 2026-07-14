package com.aistudio.xide.core.automation

import java.io.File

/**
 * Concrete implementation enforcing path restrictions and execution guards.
 */
class AutomationPermissionVerifierImpl(
    private val projectRootProvider: () -> String?
) : AutomationPermissionVerifier {

    override fun isPermitted(action: AutomationAction): PermissionVerdict {
        val root = projectRootProvider() ?: return PermissionVerdict.Denied("No active project root established.")
        
        return when (action) {
            is AutomationAction.CreateFile -> {
                if (isPathWithinRoot(action.path, root)) {
                    PermissionVerdict.Allowed
                } else {
                    PermissionVerdict.Denied("Path is outside active workspace boundary: ${action.path}")
                }
            }
            is AutomationAction.ModifyFile -> {
                if (isPathWithinRoot(action.path, root)) {
                    PermissionVerdict.Allowed
                } else {
                    PermissionVerdict.Denied("Path is outside active workspace boundary: ${action.path}")
                }
            }
            is AutomationAction.RunBuild -> {
                PermissionVerdict.Allowed
            }
            is AutomationAction.AnalyzeProject -> {
                if (isPathWithinRoot(action.projectPath, root)) {
                    PermissionVerdict.Allowed
                } else {
                    PermissionVerdict.Denied("Project path is outside active workspace boundary: ${action.projectPath}")
                }
            }
            is AutomationAction.ExecuteCommand -> {
                val cmd = action.commandString.trim()
                // Strict guard: Only allow safe Gradle command invocations in Phase 12.
                if (cmd.startsWith("gradlew ") || cmd.startsWith("gradle ") || cmd.startsWith("./gradlew ")) {
                    PermissionVerdict.Allowed
                } else {
                    PermissionVerdict.Denied("Arbitrary command execution is forbidden. Only Gradle toolchain commands are permitted.")
                }
            }
        }
    }

    private fun isPathWithinRoot(path: String, root: String): Boolean {
        return try {
            val file = File(path)
            val rootFile = File(root)
            val normalizedPath = file.canonicalPath
            val normalizedRoot = rootFile.canonicalPath
            normalizedPath.startsWith(normalizedRoot)
        } catch (e: Exception) {
            false
        }
    }
}
