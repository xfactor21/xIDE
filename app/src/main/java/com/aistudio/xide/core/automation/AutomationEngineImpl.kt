package com.aistudio.xide.core.automation

import com.aistudio.xide.core.provider.ProviderHealth

class AutomationEngineImpl(
    override val taskQueue: TaskQueue,
    private val permissionVerifier: AutomationPermissionVerifier,
    private val providers: List<AutomationProvider>
) : AutomationEngine, AutomationService {

    override suspend fun scheduleTask(task: AutomationTask): String {
        taskQueue.enqueue(task)
        return task.taskId
    }

    override suspend fun cancelTask(taskId: String) {
        taskQueue.cancel(taskId)
    }

    override suspend fun recoverTask(taskId: String) {
        // Implementation for task recovery boundaries
    }

    override suspend fun dispatchAction(action: AutomationAction): AutomationResult {
        // 1. Permission Safety Boundary
        val verdict = permissionVerifier.isPermitted(action)
        if (verdict is PermissionVerdict.Denied) {
            return AutomationResult(
                success = false,
                message = "Permission Denied: ${verdict.reason}"
            )
        }

        // 2. Resolve matching healthy provider
        val provider = providers.firstOrNull { p ->
            p.healthCheck() == ProviderHealth.HEALTHY && p.canExecute(action)
        } ?: return AutomationResult(
            success = false,
            message = "No healthy automation provider registered for action: ${action::class.simpleName}"
        )

        // 3. Execution delegation
        return try {
            provider.executeAction(action)
        } catch (e: Exception) {
            AutomationResult(
                success = false,
                message = "Execution failed: ${e.message}",
                diagnostics = listOf(e.stackTraceToString())
            )
        }
    }
}
