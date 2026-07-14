package com.aistudio.xide.core.automation

/**
 * Immutable outcome of an automation action execution.
 */
data class AutomationResult(
    val success: Boolean,
    val message: String,
    val diagnostics: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)
