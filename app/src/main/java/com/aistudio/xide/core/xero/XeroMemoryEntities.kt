package com.aistudio.xide.core.xero

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "xero_actions")
data class XeroActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "xero_solutions")
data class XeroSolutionEntity(
    @PrimaryKey val problem: String,
    val solution: String
)

@Entity(tableName = "xero_summaries")
data class XeroSummaryEntity(
    @PrimaryKey val projectId: String,
    val summary: String
)

@Entity(tableName = "xero_preferences")
data class XeroPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)
