package com.aistudio.xide.core.xero

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface XeroMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: XeroActionEntity)

    @Query("SELECT action FROM xero_actions ORDER BY timestamp ASC")
    suspend fun getAllActions(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSolution(solution: XeroSolutionEntity)

    @Query("SELECT * FROM xero_solutions")
    suspend fun getAllSolutions(): List<XeroSolutionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: XeroSummaryEntity)

    @Query("SELECT * FROM xero_summaries")
    suspend fun getAllSummaries(): List<XeroSummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: XeroPreferenceEntity)

    @Query("SELECT * FROM xero_preferences")
    suspend fun getAllPreferences(): List<XeroPreferenceEntity>
}
