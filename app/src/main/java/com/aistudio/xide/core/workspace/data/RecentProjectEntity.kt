package com.aistudio.xide.core.workspace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "recent_projects",
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
data class RecentProjectEntity(
    @PrimaryKey
    val projectId: String,
    val lastOpenedAt: Long,
    val isPinned: Boolean
)
