package com.aistudio.xide.core.editor

data class ImprovementSuggestion(
    val title: String,
    val description: String,
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val suggestedContent: String
)

class CodeImprovementAnalyzer {
    fun analyzeForImprovements(filePath: String, content: String): List<ImprovementSuggestion> {
        val suggestions = mutableListOf<ImprovementSuggestion>()
        val lines = content.lines()
        
        lines.forEachIndexed { index, line ->
            if (line.trim() == "try {") {
                // Check if it's an empty catch
                if (index + 2 < lines.size && lines[index+2].trim().contains("catch")) {
                    suggestions.add(
                        ImprovementSuggestion(
                            title = "Unsafe empty catch block",
                            description = "Empty catch blocks can swallow exceptions and make debugging difficult.",
                            filePath = filePath,
                            startLine = index + 1,
                            endLine = index + 3,
                            suggestedContent = "try {\n} catch(e: Exception) {\n    e.printStackTrace()\n}"
                        )
                    )
                }
            }
        }
        
        return suggestions
    }
}
