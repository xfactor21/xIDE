package com.aistudio.xide.core.workspace

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceDatabaseTest {

    @Test
    fun testDatabaseSchemaExists() {
        val dbClass = Class.forName("com.aistudio.xide.core.workspace.data.WorkspaceDatabase")
        assertTrue(dbClass.superclass.name == "androidx.room.RoomDatabase")
    }
}
