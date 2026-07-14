package com.aistudio.xide.core.automation

import com.aistudio.xide.core.provider.XideProvider

/**
 * Decoupled provider interface for performing low-level actions.
 * Prevents the automation engine from depending directly on terminal or filesystem implementation details.
 */
interface AutomationProvider : XideProvider {
    /**
     * Executes the given automation action on the underlying subsystem.
     */
    suspend fun executeAction(action: AutomationAction): AutomationResult

    /**
     * Checks if this provider is capable of executing the requested action.
     */
    fun canExecute(action: AutomationAction): Boolean
}
