package com.aistudio.xide.core.workspace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "workspace_metadata",
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
data class WorkspaceMetadataEntity(
    @PrimaryKey
    val metadataId: String,
    val projectId: String,
    val key: String,
    val value: String
)
