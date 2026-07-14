package com.aistudio.xide.core.workspace.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aistudio.xide.core.xero.XeroActionEntity
import com.aistudio.xide.core.xero.XeroSolutionEntity
import com.aistudio.xide.core.xero.XeroSummaryEntity
import com.aistudio.xide.core.xero.XeroPreferenceEntity
import com.aistudio.xide.core.xero.XeroMemoryDao

@Database(
    entities = [
        ProjectEntity::class,
        RecentProjectEntity::class,
        WorkspaceSessionEntity::class,
        WorkspaceMetadataEntity::class,
        XeroActionEntity::class,
        XeroSolutionEntity::class,
        XeroSummaryEntity::class,
        XeroPreferenceEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WorkspaceDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun xeroMemoryDao(): XeroMemoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `xero_actions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `action` TEXT NOT NULL, 
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `xero_solutions` (
                        `problem` TEXT PRIMARY KEY NOT NULL, 
                        `solution` TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `xero_summaries` (
                        `projectId` TEXT PRIMARY KEY NOT NULL, 
                        `summary` TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `xero_preferences` (
                        `key` TEXT PRIMARY KEY NOT NULL, 
                        `value` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}

