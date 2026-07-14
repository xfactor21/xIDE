package com.aistudio.xide.core.workspace

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.aistudio.xide.core.workspace.data.WorkspaceDatabase
import com.aistudio.xide.core.workspace.data.ProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceMigrationTest {

    @Test
    fun testMigration1To2() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_workspace_db"
        context.deleteDatabase(dbName)
        
        // 1. Create a raw SQLite DB at version 1 and insert a project
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `projects` (
                                `projectId` TEXT NOT NULL, 
                                `name` TEXT NOT NULL, 
                                `rootPath` TEXT NOT NULL, 
                                `fileSystemProtocol` TEXT NOT NULL, 
                                `createdAt` INTEGER NOT NULL, 
                                `lastAccessedAt` INTEGER NOT NULL, 
                                PRIMARY KEY(`projectId`)
                            )
                        """.trimIndent())

                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `recent_projects` (
                                `projectId` TEXT NOT NULL, 
                                `lastOpenedAt` INTEGER NOT NULL, 
                                `isPinned` INTEGER NOT NULL, 
                                PRIMARY KEY(`projectId`), 
                                FOREIGN KEY(`projectId`) REFERENCES `projects`(`projectId`) ON UPDATE NO ACTION ON DELETE CASCADE 
                            )
                        """.trimIndent())

                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `workspace_sessions` (
                                `sessionId` TEXT NOT NULL, 
                                `projectId` TEXT NOT NULL, 
                                `openFilesJson` TEXT NOT NULL, 
                                `activeFilePath` TEXT, 
                                `savedAt` INTEGER NOT NULL, 
                                PRIMARY KEY(`sessionId`), 
                                FOREIGN KEY(`projectId`) REFERENCES `projects`(`projectId`) ON UPDATE NO ACTION ON DELETE CASCADE 
                            )
                        """.trimIndent())

                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `workspace_metadata` (
                                `metadataId` TEXT NOT NULL, 
                                `projectId` TEXT NOT NULL, 
                                `key` TEXT NOT NULL, 
                                `value` TEXT NOT NULL, 
                                PRIMARY KEY(`metadataId`), 
                                FOREIGN KEY(`projectId`) REFERENCES `projects`(`projectId`) ON UPDATE NO ACTION ON DELETE CASCADE 
                            )
                        """.trimIndent())

                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recent_projects_projectId` ON `recent_projects` (`projectId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_sessions_projectId` ON `workspace_sessions` (`projectId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_metadata_projectId` ON `workspace_metadata` (`projectId`)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        
        val v1Db = openHelper.writableDatabase
        v1Db.execSQL("INSERT INTO projects (projectId, name, rootPath, fileSystemProtocol, createdAt, lastAccessedAt) VALUES ('p1', 'Project 1', '/path/1', 'local', 1000, 2000)")
        v1Db.close()
        
        // 2. Open DB with Room at version 2 using migration
        val roomDb = Room.databaseBuilder(context, WorkspaceDatabase::class.java, dbName)
            .addMigrations(WorkspaceDatabase.MIGRATION_1_2)
            .build()
            
        kotlinx.coroutines.runBlocking {
            val project = roomDb.workspaceDao().getProject("p1")
            assertEquals("Project 1", project?.name)
            assertEquals("/path/1", project?.rootPath)
            
            // Verify new memory tables are created and functional
            val action = com.aistudio.xide.core.xero.XeroActionEntity(action = "Migration check")
            roomDb.xeroMemoryDao().insertAction(action)
            val actions = roomDb.xeroMemoryDao().getAllActions()
            assertEquals(1, actions.size)
            assertEquals("Migration check", actions[0])
        }
        
        roomDb.close()
    }
}
