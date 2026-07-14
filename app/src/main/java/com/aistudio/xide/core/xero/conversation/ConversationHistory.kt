package com.aistudio.xide.core.xero.conversation

data class ConversationEntry(
    val question: String,
    val intent: DeveloperIntent,
    val contextUsed: List<String>,
    val responseType: String,
    val timestamp: Long
)

class ConversationHistory(
    private val maxCapacity: Int = 100
) {
    private val history = mutableListOf<ConversationEntry>()

    fun addEntry(entry: ConversationEntry) {
        synchronized(history) {
            val sanitizedQuestion = sanitize(entry.question)
            val sanitizedEntry = entry.copy(question = sanitizedQuestion)
            
            history.add(sanitizedEntry)
            if (history.size > maxCapacity) {
                history.removeAt(0)
            }
        }
    }

    fun getHistory(): List<ConversationEntry> {
        synchronized(history) {
            return history.toList()
        }
    }

    fun clearHistory() {
        synchronized(history) {
            history.clear()
        }
    }

    private fun sanitize(input: String): String {
        var sanitized = input
        sanitized = sanitized.replace(Regex("(?i)(api_?key|secret|password|token)[\\s:=]+[a-zA-Z0-9_\\-]+"), "$1=REDACTED")
        sanitized = sanitized.replace(Regex("AIzaSy[A-Za-z0-9_\\-]{33}"), "AIzaSy_REDACTED")
        return sanitized
    }
}
