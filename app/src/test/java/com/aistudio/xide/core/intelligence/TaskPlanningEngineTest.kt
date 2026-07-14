package com.aistudio.xide.core.intelligence

import com.aistudio.xide.core.command.ActionDescriptor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPlanningEngineTest {
    @Test
    fun testPlanTask() = runBlocking {
        val planner = object : TaskPlannerProvider {
            override suspend fun canHandle(goal: String): Boolean = goal == "Fix build"
            override suspend fun planTask(goal: String, projectContext: ProjectContext): ActionPlan {
                val descriptor = ActionDescriptor("automation.run_terminal", "Run build")
                return ActionPlan(goal, listOf(descriptor), true)
            }
        }
        val engine = DefaultTaskPlanningEngine(listOf(planner))
        val context = ProjectContext("/", emptyList(), "", "", "android_app", emptyList(), emptyList(), "gradle", emptyList(), emptyList(), emptyList())
        val plan = engine.planTask("Fix build", context)
        
        assertEquals("Fix build", plan.title)
        assertTrue(plan.requiresApproval)
        assertEquals(1, plan.commands.size)
    }
}
