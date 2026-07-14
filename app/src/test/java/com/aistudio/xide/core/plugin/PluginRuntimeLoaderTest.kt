package com.aistudio.xide.core.plugin

import com.aistudio.xide.core.service.LocalServiceRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PluginRuntimeLoaderTest {
    @Test
    fun testLoadUnload() = runBlocking {
        val registry = LocalServiceRegistry()
        val loader = PluginRuntimeLoader(registry)
        
        val manifest = PluginManifest("test", "test", "1.0", "AI", "1.0", "1.0", emptyList(), emptyList(), emptyList())
        val descriptor = PluginDescriptor(manifest, "/tmp/test", PluginState.DISCOVERED, emptyList())
        
        loader.load(descriptor)
        loader.unload("test")
    }
}
