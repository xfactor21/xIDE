package com.aistudio.xide.core.ai.actions

import java.security.MessageDigest

data class ChangePreview(
    val affectedFiles: List<String>,
    val originalContentHash: String,
    val proposedChanges: String,
    val linesAdded: Int,
    val linesDeleted: Int,
    val estimatedImpact: String
) {
    companion object {
        fun generate(filePath: String, originalContent: String, proposedContent: String): ChangePreview {
            val hash = sha256(originalContent)
            
            val originalLines = originalContent.lines()
            val proposedLines = proposedContent.lines()
            
            // Basic diff estimation
            val origSet = originalLines.toSet()
            val propSet = proposedLines.toSet()
            
            val added = proposedLines.filter { !origSet.contains(it) }.size
            val deleted = originalLines.filter { !propSet.contains(it) }.size
            
            val impact = when {
                added + deleted > 100 -> "HIGH: Extensive modification of logic."
                added + deleted > 20 -> "MEDIUM: Moderate file modification."
                else -> "LOW: Standard targeted edits."
            }
            
            return ChangePreview(
                affectedFiles = listOf(filePath),
                originalContentHash = hash,
                proposedChanges = proposedContent,
                linesAdded = added,
                linesDeleted = deleted,
                estimatedImpact = impact
            )
        }
        
        private fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
