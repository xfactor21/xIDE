package com.aistudio.xide.core.intelligence

/**
 * Plans a sequence of actions based on a high-level goal.
 */
interface TaskPlanningEngine {
    suspend fun planTask(goal: String, projectContext: ProjectContext): ActionPlan
}

/**
 * Extension point for domain-specific planners.
 */
interface TaskPlannerProvider {
    suspend fun canHandle(goal: String): Boolean
    suspend fun planTask(goal: String, projectContext: ProjectContext): ActionPlan
}

/**
 * Production implementation that aggregates TaskPlannerProviders.
 */
class DefaultTaskPlanningEngine(
    private val planners: List<TaskPlannerProvider>
) : TaskPlanningEngine {
    override suspend fun planTask(goal: String, projectContext: ProjectContext): ActionPlan {
        for (planner in planners) {
            if (planner.canHandle(goal)) {
                return planner.planTask(goal, projectContext)
            }
        }
        // Fallback empty plan if no planner handles the goal
        return ActionPlan(
            title = goal,
            commands = emptyList(),
            requiresApproval = true
        )
    }
}
