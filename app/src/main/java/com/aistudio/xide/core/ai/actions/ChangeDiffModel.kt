package com.aistudio.xide.core.ai.actions

data class ChangeDiffModel(
    val originalContent: String,
    val proposedContent: String,
    val addedLines: Int,
    val removedLines: Int,
    val changedSections: List<DiffSection>
)

data class DiffSection(
    val type: DiffType,
    val startLineOriginal: Int,
    val endLineOriginal: Int,
    val startLineProposed: Int,
    val endLineProposed: Int,
    val content: String
)

enum class DiffType {
    ADDED, REMOVED, UNCHANGED, MODIFIED
}
