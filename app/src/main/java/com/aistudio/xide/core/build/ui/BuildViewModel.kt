package com.aistudio.xide.core.build.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.xide.core.build.*
import com.aistudio.xide.core.diagnostics.BuildDiagnostic
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Build Screen, exposing build lifecycle states and diagnostics.
 */
class BuildViewModel(
    private val buildService: BuildService,
    private val diagnosticsEngine: DiagnosticsEngine,
    private val projectRootProvider: () -> String
) : ViewModel() {

    val buildState: StateFlow<BuildState> = buildService.buildState
    val activeBuildResult: StateFlow<BuildResult?> = buildService.activeBuildResult
    val activeDiagnostics: StateFlow<List<BuildDiagnostic>> = diagnosticsEngine.activeDiagnostics

    fun buildDebugApk() {
        viewModelScope.launch {
            val request = BuildRequest(
                target = "app",
                variant = "debug",
                operation = "assembleDebug"
            )
            buildService.executeBuild(request)
        }
    }

    fun buildReleaseApk() {
        viewModelScope.launch {
            val request = BuildRequest(
                target = "app",
                variant = "release",
                operation = "assembleRelease"
            )
            buildService.executeBuild(request)
        }
    }

    fun clearDiagnostics() {
        diagnosticsEngine.clearDiagnostics()
    }
}
