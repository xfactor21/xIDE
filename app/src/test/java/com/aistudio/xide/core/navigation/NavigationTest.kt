package com.aistudio.xide.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationTest {

    @Test
    fun testDestinationRegistry() {
        val registry = DestinationRegistry()
        registry.registerDestination("dashboard", "DashboardScreenMock")
        
        val screen = registry.getDestination("dashboard")
        assertNotNull(screen)
        assertEquals("DashboardScreenMock", screen)
    }
}
