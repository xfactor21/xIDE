package com.aistudio.xide.core.dependency

import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalPackageManagerProviderTest {
    @Test
    fun testLifecycle() = runBlocking {
        val provider = LocalPackageManagerProvider()
        provider.initialize()
        assertEquals(ProviderHealth.HEALTHY, provider.healthCheck())
    }

    @Test
    fun testInstallPackage() = runBlocking {
        val provider = LocalPackageManagerProvider()
        val pkg = PackageDefinition("com.squareup.retrofit2:retrofit", "2.9.0", "maven")
        val req = PackageInstallRequest(listOf(pkg), "/tmp")
        
        val result = provider.installPackages(req)
        assertEquals(true, result.success)
        assertEquals(1, result.installedPackages.size)
    }
}
