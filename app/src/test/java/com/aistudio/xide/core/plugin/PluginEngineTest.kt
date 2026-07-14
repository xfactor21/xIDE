package com.aistudio.xide.core.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PluginEngineTest {

    @Test
    fun testPluginManifest() {
        val manifest = PluginManifest(
            id = "test.plugin",
            name = "Test Plugin",
            version = "1.0.0",
            author = "AI Studio",
            requiredXideVersion = "1.0.0",
            compatibilityRange = "1.0.x",
            exportedProviders = listOf("CompilerProvider"),
            permissions = listOf("VFS_READ"),
            dependencies = emptyList()
        )
        
        assertEquals("test.plugin", manifest.id)
        assertEquals("VFS_READ", manifest.permissions.first())
    }

    @Test
    fun testPluginDescriptor() {
        val manifest = PluginManifest("test.plugin", "Test", "1.0.0", "AI Studio", "1.0.0", "1.0.x", emptyList(), emptyList(), emptyList())
        val descriptor = PluginDescriptor(
            manifest = manifest,
            location = "/plugins/test.jar",
            state = PluginState.DISCOVERED,
            providers = emptyList()
        )
        
        assertEquals(PluginState.DISCOVERED, descriptor.state)
        assertEquals("/plugins/test.jar", descriptor.location)
    }

    @Test
    fun testPluginCommands() {
        val command = PluginCommand.InstallPluginCommand("/plugins/test.jar")
        assertEquals("Install Plugin", command.name)
    }

    @Test
    fun testPluginEvents() {
        val event = PluginEvent.PluginInstalled("test.plugin")
        assertEquals("test.plugin", event.pluginId)
        assertNotNull(event.eventId)
    }

    @Test
    fun testXeroPluginContext() {
        val ctx = XeroPluginContext(emptyList(), listOf("CompilerProvider"))
        assertEquals(1, ctx.availableCapabilities.size)
    }
}
