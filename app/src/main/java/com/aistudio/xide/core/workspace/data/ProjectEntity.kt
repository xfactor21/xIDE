package com.aistudio.xide.core.workspace.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val projectId: String,
    val name: String,
    val rootPath: String,
    val fileSystemProtocol: String,
    val createdAt: Long,
    val lastAccessedAt: Long
)
