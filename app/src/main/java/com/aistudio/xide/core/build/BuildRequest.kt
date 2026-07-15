package com.aistudio.xide.core.build

<<<<<<< HEAD
data class BuildRequest(
    val target: String,
    val variant: String,
    val operation: String
=======
/**
 * Represents an intentional request to execute a build operation.
 */
data class BuildRequest(
    val target: String,           // e.g. "app", "core"
    val variant: String,          // e.g. "debug", "release"
    val operation: String,        // e.g. "assembleDebug", "compileDebugKotlin", "test", "lint"
    val metadata: Map<String, String> = emptyMap()
>>>>>>> parent of a3bc74e (Phase 22 complete (testing))
)
