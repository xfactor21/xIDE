package com.aistudio.xide.core.build

import com.aistudio.xide.core.diagnostics.BuildDiagnostic

<<<<<<< HEAD
data class BuildResult(
    val success: Boolean,
    val message: String,
    val startTime: Long,
    val endTime: Long,
    val artifactInfo: ArtifactInfo? = null,
    val diagnostics: List<BuildDiagnostic> = emptyList()
=======
/**
 * Represents the final result of a build execution request.
 */
data class BuildResult(
    val success: Boolean,
    val artifactInfo: ArtifactInfo?,
    val diagnostics: List<BuildDiagnostic>,
    val startTime: Long,
    val endTime: Long,
    val message: String
>>>>>>> parent of a3bc74e (Phase 22 complete (testing))
)
