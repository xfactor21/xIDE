package com.aistudio.xide.core.intelligence

import com.aistudio.xide.core.command.ActionDescriptor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryEngineTest {
    @Test
    fun testAnalyzeFailure() = runBlocking {
        val strategy = object : RecoveryStrategyProvider {
            override suspend fun proposeRecovery(failureReason: String, projectContext: ProjectContext): ActionPlan? {
                if (failureReason.contains("missing dependency", ignoreCase = true)) {
                    val installCommand = ActionDescriptor(
                        commandId = "automation.run_terminal",
                        description = "Run gradle build to resolve dependencies",
                        arguments = mapOf("command" to "./gradlew build", "sessionId" to "recovery_session")
                    )
                    return ActionPlan(
                        title = "A missing dependency was detected. Would you like to resolve it?",
                        commands = listOf(installCommand),
                        requiresApproval = true
                    )
                }
                return null
            }
        }
        val engine = DefaultRecoveryEngine(listOf(strategy))
        val context = ProjectContext("/", emptyList(), "", "", "android_app", emptyList(), emptyList(), "gradle", emptyList(), emptyList(), emptyList())
        
        val plan = engine.analyzeFailure("Error: missing dependency androidx.core:core-ktx", context)
        
        assertNotNull(plan)
        assertEquals(1, plan!!.commands.size)
        assertTrue(plan.requiresApproval)
    }
}
