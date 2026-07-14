package com.aistudio.xide.core.vfs

import com.aistudio.xide.core.events.EventBus
import com.aistudio.xide.core.events.FileChanged
import com.aistudio.xide.core.events.PlatformEvent
import com.aistudio.xide.core.diagnostics.DiagnosticsEngine
import com.aistudio.xide.core.build.BuildService
import com.aistudio.xide.core.ai.AiContextManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class FileChangeTracker(
    private val eventBus: EventBus,
    private val diagnosticsEngine: DiagnosticsEngine?,
    private val buildService: BuildService?,
    private val aiContextManager: AiContextManager?
) {
    private val _createdFiles = mutableSetOf<String>()
    private val _modifiedFiles = mutableSetOf<String>()
    private val _deletedFiles = mutableSetOf<String>()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            eventBus.subscribe(PlatformEvent::class.java).collect { event ->
                handleEvent(event)
            }
        }
    }

    val createdFiles: Set<String> get() = _createdFiles
    val modifiedFiles: Set<String> get() = _modifiedFiles
    val deletedFiles: Set<String> get() = _deletedFiles

    fun clearHistory() {
        _createdFiles.clear()
        _modifiedFiles.clear()
        _deletedFiles.clear()
    }

    private fun handleEvent(event: PlatformEvent) {
        when (event) {
            is VfsEvent.FileCreated -> {
                _createdFiles.add(event.path)
                _deletedFiles.remove(event.path)
                onFileChangedSystemEffect(event.path)
            }
            is VfsEvent.FileDeleted -> {
                _deletedFiles.add(event.path)
                _createdFiles.remove(event.path)
                _modifiedFiles.remove(event.path)
                onFileChangedSystemEffect(event.path)
            }
            is FileChanged -> {
                _modifiedFiles.add(event.filePath)
                onFileChangedSystemEffect(event.filePath)
            }
        }
    }

    private fun onFileChangedSystemEffect(filePath: String) {
        // Refresh diagnostics
        diagnosticsEngine?.clearDiagnostics()

        // Update AI Context
        aiContextManager?.addManualContext("recent_change", "Modified file: $filePath. State is dirty. Build might be required.")
    }
}
