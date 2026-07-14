package com.aistudio.xide.core.build

import com.aistudio.xide.core.diagnostics.BuildDiagnostic

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
)
