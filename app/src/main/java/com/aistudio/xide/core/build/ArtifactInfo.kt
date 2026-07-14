package com.aistudio.xide.core.build

/**
 * Metadata representing a real build output artifact.
 */
data class ArtifactInfo(
    val path: String,
    val timestamp: Long,
    val variant: String,
    val metadata: Map<String, String> = emptyMap()
)
