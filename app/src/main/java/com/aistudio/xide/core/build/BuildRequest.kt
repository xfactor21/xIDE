package com.aistudio.xide.core.build

/**
 * Represents an intentional request to execute a build operation.
 */
data class BuildRequest(
    val target: String,           // e.g. "app", "core"
    val variant: String,          // e.g. "debug", "release"
    val operation: String,        // e.g. "assembleDebug", "compileDebugKotlin", "test", "lint"
    val metadata: Map<String, String> = emptyMap()
)
