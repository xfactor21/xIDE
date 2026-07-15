package com.aistudio.xide.core.build

<<<<<<< HEAD
data class ArtifactInfo(
    val path: String,
    val variant: String,
    val timestamp: Long,
    val sizeBytes: Long
=======
/**
 * Metadata representing a real build output artifact.
 */
data class ArtifactInfo(
    val path: String,
    val timestamp: Long,
    val variant: String,
    val metadata: Map<String, String> = emptyMap()
>>>>>>> parent of a3bc74e (Phase 22 complete (testing))
)
