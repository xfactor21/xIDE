package com.aistudio.xide.core.provider

/**
 * Abstraction for AI capabilities.
 */
interface AIProvider : XideProvider {
    suspend fun generateText(prompt: String): String
    // Other AI related operations
}
