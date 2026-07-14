package com.aistudio.xide.core.vfs

import com.aistudio.xide.core.events.PlatformEvent
import java.util.UUID

sealed class VfsEvent : PlatformEvent {
    override val eventId: String = UUID.randomUUID().toString()
    override val timestamp: Long = System.currentTimeMillis()
    
    data class FileCreated(val path: String) : VfsEvent()
    data class FileDeleted(val path: String) : VfsEvent()
    data class FileMoved(val oldPath: String, val newPath: String) : VfsEvent()
    data class FileRenamed(val oldPath: String, val newPath: String) : VfsEvent()
    data class FolderCreated(val path: String) : VfsEvent()
    data class FolderDeleted(val path: String) : VfsEvent()
}
