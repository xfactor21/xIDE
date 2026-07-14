package com.aistudio.xide.core.workspace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "workspace_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["projectId"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["projectId"])
    ]
)
data class WorkspaceSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val projectId: String,
    val openFilesJson: String,
    val activeFilePath: String?,
    val savedAt: Long
)
