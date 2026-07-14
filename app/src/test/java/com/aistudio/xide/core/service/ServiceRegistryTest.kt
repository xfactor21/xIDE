package com.aistudio.xide.core.service

import com.aistudio.xide.core.provider.ProviderHealth
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ServiceRegistryTest {

    @Test
    fun testServiceDescriptor() {
        val desc = ServiceDescriptor(
            providerType = "CompilerProvider",
            pluginId = "test.plugin",
            priority = 100,
            health = ProviderHealth.HEALTHY
        )
        
        assertEquals(100, desc.priority)
        assertEquals(ProviderHealth.HEALTHY, desc.health)
    }
}
