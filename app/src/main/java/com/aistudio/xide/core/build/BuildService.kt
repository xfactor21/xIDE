package com.aistudio.xide.core.build

import kotlinx.coroutines.flow.StateFlow

<<<<<<< HEAD
<<<<<<< HEAD
interface BuildService {
    val buildState: StateFlow<BuildState>
    val activeBuildResult: StateFlow<BuildResult?>
=======
=======
>>>>>>> parent of a3bc74e (Phase 22 complete (testing))
/**
 * States that a build execution process can occupy.
 */
enum class BuildState {
    IDLE,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED
}

/**
 * Orchestration layer interface that manages requests, tracks execution states,
 * and handles artifact discovery.
 */
interface BuildService {
    val buildState: StateFlow<BuildState>
    val activeBuildResult: StateFlow<BuildResult?>

<<<<<<< HEAD
>>>>>>> parent of a3bc74e (Phase 22 complete (testing))
=======
>>>>>>> parent of a3bc74e (Phase 22 complete (testing))
    suspend fun executeBuild(request: BuildRequest): BuildResult
    fun cancelBuild()
}
