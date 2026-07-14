package com.aistudio.xide.core.provider

import com.aistudio.xide.core.events.EventBus
import com.aistudio.xide.core.events.PlatformEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class MockEventBus : EventBus {
    override fun publish(event: PlatformEvent) {}
    override fun <T : PlatformEvent> subscribe(eventType: Class<T>): Flow<T> = emptyFlow()
}

@RunWith(RobolectricTestRunner::class)
class ProviderHealthMonitorTest {
    @Test
    fun testMonitorStartStop() {
        val eventBus = MockEventBus()
        val monitor = ProviderHealthMonitor(eventBus, emptyList())
        monitor.startMonitoring(100L)
        monitor.stopMonitoring()
    }
}
