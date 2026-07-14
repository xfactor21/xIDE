package com.aistudio.xide.core.execution

import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Ensures memory limit on terminal output
 */
class TerminalOutputBuffer(private val maxLines: Int = 1000) {
    private val buffer = ConcurrentLinkedDeque<String>()
    
    fun append(line: String) {
        buffer.addLast(line)
        while (buffer.size > maxLines) {
            buffer.removeFirst()
        }
    }
    
    fun getOutput(): String {
        return buffer.joinToString("\n")
    }
    
    fun clear() {
        buffer.clear()
    }
}
