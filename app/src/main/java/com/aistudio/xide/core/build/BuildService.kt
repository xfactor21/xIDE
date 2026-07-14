package com.aistudio.xide.core.build

import kotlinx.coroutines.flow.StateFlow

interface BuildService {
    val buildState: StateFlow<BuildState>
    val activeBuildResult: StateFlow<BuildResult?>
    suspend fun executeBuild(request: BuildRequest): BuildResult
    fun cancelBuild()
}
