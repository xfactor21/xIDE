package com.aistudio.xide.core.provider

import com.aistudio.xide.core.execution.TerminalProvider
import com.aistudio.xide.core.dependency.PackageManagerProvider
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProviderComplianceTest {

    @Test
    fun testTerminalProviderIsXideProvider() {
        val isProvider = TerminalProvider::class.java.interfaces.contains(XideProvider::class.java)
        assertTrue("TerminalProvider must extend XideProvider", isProvider)
    }

    @Test
    fun testPackageManagerProviderIsXideProvider() {
        val isProvider = PackageManagerProvider::class.java.interfaces.contains(XideProvider::class.java)
        assertTrue("PackageManagerProvider must extend XideProvider", isProvider)
    }
    
    @Test
    fun testXideProviderProperties() {
        val clazz = XideProvider::class.java
        val methods = clazz.methods.map { it.name }
        
        // Check properties (Kotlin properties compile to getX or just X if boolean, etc.)
        assertTrue(methods.contains("getProviderId"))
        assertTrue(methods.contains("getProviderName"))
        assertTrue(methods.contains("getProviderVersion"))
        assertTrue(methods.contains("getSupportedFeatures"))
        assertTrue(methods.contains("getRequirements"))
        assertTrue(methods.contains("getLimitations"))
        assertTrue(methods.contains("getDescription"))
        assertTrue(methods.contains("getAuthor"))
        assertTrue(methods.contains("getCompatibilityVersion"))
        
        // Check lifecycle methods
        // Suspend methods add Continuation parameter, but name is preserved.
        assertTrue(methods.contains("initialize"))
        assertTrue(methods.contains("shutdown"))
        assertTrue(methods.contains("healthCheck"))
    }
}
