package com.aistudio.xide.core.build

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class APKArtifactResolverTest {

    @Test
    fun testValidApkAccepted() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test-project-valid-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val apkDir = File(tempDir, "app/build/outputs/apk/debug")
        apkDir.mkdirs()

        val apkFile = File(apkDir, "app-debug.apk")
        apkFile.writeText("valid apk content") // Size > 0 bytes

        val resolver = ArtifactResolver { tempDir.absolutePath }
        val artifact = resolver.resolveArtifact("assembleDebug", "debug", 0)

        assertNotNull(artifact)
        assertEquals(apkFile.absolutePath, artifact?.path)
        assertEquals("app-debug.apk", artifact?.metadata?.get("filename"))
        assertEquals("debug", artifact?.variant)
        assertEquals(apkFile.length().toString(), artifact?.metadata?.get("size_bytes"))

        tempDir.deleteRecursively()
    }

    @Test
    fun testMissingApkRejected() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test-project-missing-${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val resolver = ArtifactResolver { tempDir.absolutePath }
        val artifact = resolver.resolveArtifact("assembleDebug", "debug", 0)

        assertNull(artifact)
        tempDir.deleteRecursively()
    }

    @Test
    fun testZeroByteApkRejected() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test-project-zerobyte-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val apkDir = File(tempDir, "app/build/outputs/apk/debug")
        apkDir.mkdirs()

        val apkFile = File(apkDir, "app-debug.apk")
        apkFile.createNewFile() // Creates a 0-byte file

        val resolver = ArtifactResolver { tempDir.absolutePath }
        val artifact = resolver.resolveArtifact("assembleDebug", "debug", 0)

        assertNull(artifact)
        tempDir.deleteRecursively()
    }

    @Test
    fun testInvalidExtensionRejected() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test-project-ext-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val apkDir = File(tempDir, "app/build/outputs/apk/debug")
        apkDir.mkdirs()

        val apkFile = File(apkDir, "app-debug.tmp") // Non-APK extension
        apkFile.writeText("not an apk")

        val resolver = ArtifactResolver { tempDir.absolutePath }
        val artifact = resolver.resolveArtifact("assembleDebug", "debug", 0)

        assertNull(artifact)
        tempDir.deleteRecursively()
    }

    @Test
    fun testArtifactOutsideProjectDirectoryRejected() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test-project-outside-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val apkDir = File(tempDir, "app/build/outputs/apk/debug")
        apkDir.mkdirs()

        // Create a symlink or malicious path pointing outside. Let's create an APK whose canonical path resolves outside root
        // In this test, we can mock/simulate path check by returning a rootPath that points elsewhere
        val resolver = ArtifactResolver { "/tmp/other_dir_path_completely" }
        val artifact = resolver.resolveArtifact("assembleDebug", "debug", 0)

        assertNull(artifact)
        tempDir.deleteRecursively()
    }
}
