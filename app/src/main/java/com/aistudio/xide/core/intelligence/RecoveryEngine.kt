package com.aistudio.xide.core.intelligence

/**
 * Proposes recovery workflows for build, compilation, or execution failures.
 */
interface RecoveryEngine {
    /**
     * Analyzes a failure and proposes an actionable recovery plan.
     */
    suspend fun analyzeFailure(failureReason: String, projectContext: ProjectContext): ActionPlan?
}

/**
 * Extension point for plugins to provide specific recovery strategies.
 */
interface RecoveryStrategyProvider {
    /**
     * Examines the failure and returns a proposed ActionPlan if it knows how to resolve it.
     */
    suspend fun proposeRecovery(failureReason: String, projectContext: ProjectContext): ActionPlan?
}

/**
 * Production implementation that aggregates recovery proposals from registered strategies.
 */
class DefaultRecoveryEngine(
    private val strategies: List<RecoveryStrategyProvider>
) : RecoveryEngine {
    override suspend fun analyzeFailure(failureReason: String, projectContext: ProjectContext): ActionPlan? {
        for (strategy in strategies) {
            val proposal = strategy.proposeRecovery(failureReason, projectContext)
            if (proposal != null) {
                return proposal // Return the first valid recovery strategy
            }
        }
        return null
    }
}
