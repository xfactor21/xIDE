package com.aistudio.xide.core.automation

/**
 * Platform service interface for orchestrating automation requests with full permission verification.
 */
interface AutomationService {
    /**
     * Dispatches the action to a healthy, matching provider after verifying security boundaries.
     */
    suspend fun dispatchAction(action: AutomationAction): AutomationResult
}
