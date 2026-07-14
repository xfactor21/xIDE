package com.aistudio.xide.core.execution

import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalTerminalProviderTest {
    @Test
    fun testProviderLifecycle() = runBlocking {
        val provider = LocalTerminalProvider()
        provider.initialize()
        assertEquals(ProviderHealth.HEALTHY, provider.healthCheck())
        
        provider.shutdown()
        assertEquals(ProviderHealth.DEGRADED, provider.healthCheck())
    }

    @Test
    fun testSessionManagement() = runBlocking {
        val provider = LocalTerminalProvider()
        val session = provider.createSession("/tmp")
        assertNotNull(session.sessionId)
        assertEquals("/tmp", session.workingDirectory)
        
        provider.closeSession(session.sessionId)
    }

    @Test
    fun testOutputBuffer() {
        val buffer = TerminalOutputBuffer(maxLines = 2)
        buffer.append("line1")
        buffer.append("line2")
        buffer.append("line3")
        
        val output = buffer.getOutput()
        assertTrue(output.contains("line2"))
        assertTrue(output.contains("line3"))
        assertTrue(!output.contains("line1"))
    }
}
