package com.aistudio.xide.core.vfs

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VfsArchitectureTest {

    @Test
    fun testVfsImplementationExists() {
        assertNotNull(Class.forName("com.aistudio.xide.core.vfs.local.LocalFileSystemProvider"))
    }
}
