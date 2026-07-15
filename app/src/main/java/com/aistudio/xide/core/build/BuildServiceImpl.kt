package com.aistudio.xide.core.build

import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Concrete implementation of the BuildService managing lifecycle state and provider invocation.
 */
class BuildServiceImpl(
    private val providers: List<BuildProvider>,
    private val diagnosticsEngine: DiagnosticsEngine,
    private val artifactResolver: ArtifactResolver
) : BuildService {

    private val _buildState = MutableStateFlow(BuildState.IDLE)
    override val buildState: StateFlow<BuildState> = _buildState.asStateFlow()

    private val _activeBuildResult = MutableStateFlow<BuildResult?>(null)
    override val activeBuildResult: StateFlow<BuildResult?> = _activeBuildResult.asStateFlow()

    override suspend fun executeBuild(request: BuildRequest): BuildResult {
        _buildState.value = BuildState.RUNNING
        _activeBuildResult.value = null
        diagnosticsEngine.clearDiagnostics()

        val provider = providers.firstOrNull { it.canBuild(request) }
        if (provider == null) {
            val endTime = System.currentTimeMillis()
            val result = BuildResult(
                success = false,
                artifactInfo = null,
                diagnostics = emptyList(),
                startTime = System.currentTimeMillis(),
                endTime = endTime,
                message = "No suitable build provider found for request operation: '${request.operation}'"
            )
            _buildState.value = BuildState.FAILED
            _activeBuildResult.value = result
            return result
        }

        try {
            val rawResult = provider.executeBuild(request)
            
            // Piping extracted diagnostics directly to the centralized linter/parser engine
            diagnosticsEngine.addDiagnostics(rawResult.diagnostics)

            // Secure artifact validation using ArtifactResolver
            val finalArtifact = if (rawResult.success) {
                artifactResolver.resolveArtifact(
                    operation = request.operation,
                    variant = request.variant,
                    exitCode = 0
                )
            } else {
                null
            }

            val finalResult = rawResult.copy(artifactInfo = finalArtifact ?: rawResult.artifactInfo)

            _buildState.value = if (finalResult.success) BuildState.SUCCESS else BuildState.FAILED
            _activeBuildResult.value = finalResult
            return finalResult
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            val result = BuildResult(
                success = false,
                artifactInfo = null,
                diagnostics = emptyList(),
                startTime = System.currentTimeMillis(),
                endTime = endTime,
                message = "Build service encountered exception: ${e.message}"
            )
            _buildState.value = BuildState.FAILED
            _activeBuildResult.value = result
            return result
        }
    }

    override fun cancelBuild() {
        if (_buildState.value == BuildState.RUNNING) {
            _buildState.value = BuildState.CANCELLED
        }
    }
}
