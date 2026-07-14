package com.aistudio.xide.core.automation

/**
 * Immutable representation of a requested automation operation.
 * Describes intent but does not execute itself.
 */
sealed class AutomationAction {
    /**
     * Action to request file creation at a given path.
     */
    data class CreateFile(
        val path: String,
        val content: String
    ) : AutomationAction()

    /**
     * Action to request file modification of existing target content.
     */
    data class ModifyFile(
        val path: String,
        val targetContent: String,
        val replacementContent: String
    ) : AutomationAction()

    /**
     * Action to request a project compilation/build.
     */
    data class RunBuild(
        val projectId: String,
        val buildType: String
    ) : AutomationAction()

    /**
     * Action to request a project structural analysis.
     */
    data class AnalyzeProject(
        val projectPath: String
    ) : AutomationAction()

    /**
     * Action to request command line execution (e.g. Gradle task).
     */
    data class ExecuteCommand(
        val commandString: String
    ) : AutomationAction()
}
