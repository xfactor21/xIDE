package com.aistudio.xide.core.automation

/**
 * Verification contract to enforce capability safety boundaries.
 */
interface AutomationPermissionVerifier {
    /**
     * Verifies if an action is permitted to execute.
     */
    fun isPermitted(action: AutomationAction): PermissionVerdict
}

/**
 * Verdict representing permission validation outcome.
 */
sealed class PermissionVerdict {
    object Allowed : PermissionVerdict()
    data class Denied(val reason: String) : PermissionVerdict()
}
