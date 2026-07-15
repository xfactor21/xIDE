package com.aistudio.xide.core.automation

import com.aistudio.xide.core.build.ArtifactResolver
import com.aistudio.xide.core.build.BuildProvider
import com.aistudio.xide.core.build.BuildRequest
import com.aistudio.xide.core.build.BuildService
import com.aistudio.xide.core.build.BuildServiceImpl
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import com.aistudio.xide.core.provider.ProviderHealth

/**
 * Connects the build system to the Automation Engine.
 * Ensures build requests map safely from AutomationActions, check permissions, and funnel
 * raw compiler logs into diagnostics and the centralized DiagnosticsEngine context.
 */
class BuildAutomationProvider(
    private val buildService: BuildService,
    private val diagnosticsEngine: DiagnosticsEngine
) : AutomationProvider {

    // Secondary constructor to remain fully compatible with existing tests that pass a BuildProvider
    constructor(
        buildProvider: BuildProvider,
        diagnosticsEngine: DiagnosticsEngine
    ) : this(
        BuildServiceImpl(listOf(buildProvider), diagnosticsEngine, ArtifactResolver { "." }),
        diagnosticsEngine
    )

    override val providerId: String = "build_automation_provider"
    override val providerName: String = "Build Automation Provider"
    override val providerVersion: String = "1.0.0"
    override val supportedFeatures: List<String> = listOf("build_project")
    override val requirements: List<String> = emptyList()
    override val limitations: List<String> = emptyList()
    override val description: String = "Integrates BuildProviders with the AutomationEngine flow."
    override val author: String = "xIDE Architect"
    override val compatibilityVersion: String = "1.0.0"

    override suspend fun initialize() {}
    override suspend fun shutdown() {}
    override suspend fun healthCheck(): ProviderHealth = ProviderHealth.HEALTHY

    override fun canExecute(action: AutomationAction): Boolean {
        return action is AutomationAction.RunBuild
    }

    override suspend fun executeAction(action: AutomationAction): AutomationResult {
        if (action !is AutomationAction.RunBuild) {
            return AutomationResult(success = false, message = "Unsupported action type: ${action::class.simpleName}")
        }

        // Dynamically resolve target Gradle operation based on requested buildType/variant
        val operation = when {
            action.buildType.equals("compile", ignoreCase = true) -> "compileDebugKotlin"
            action.buildType.equals("test", ignoreCase = true) -> "test"
            action.buildType.equals("lint", ignoreCase = true) -> "lint"
            action.buildType.equals("release", ignoreCase = true) -> "assembleRelease"
            else -> "assembleDebug"
        }

        val buildRequest = BuildRequest(
            target = action.projectId,
            variant = action.buildType,
            operation = operation
        )

        val buildResult = buildService.executeBuild(buildRequest)

        return AutomationResult(
            success = buildResult.success,
            message = buildResult.message,
            metadata = mapOf(
                "action" to "build",
                "success" to buildResult.success.toString(),
                "startTime" to buildResult.startTime.toString(),
                "endTime" to buildResult.endTime.toString(),
                "artifactPath" to (buildResult.artifactInfo?.path ?: "")
            )
        )
    }
}

