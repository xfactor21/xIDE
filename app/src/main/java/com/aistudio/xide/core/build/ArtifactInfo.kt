package com.aistudio.xide.core.build

data class ArtifactInfo(
    val path: String,
    val variant: String,
    val timestamp: Long,
    val sizeBytes: Long
)
