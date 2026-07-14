package com.aistudio.xide.core.build

import com.aistudio.xide.core.diagnostics.BuildDiagnostic

data class BuildResult(
    val success: Boolean,
    val message: String,
    val startTime: Long,
    val endTime: Long,
    val artifactInfo: ArtifactInfo? = null,
    val diagnostics: List<BuildDiagnostic> = emptyList()
)
