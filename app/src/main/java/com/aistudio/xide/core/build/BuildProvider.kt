package com.aistudio.xide.core.build

import com.aistudio.xide.core.provider.XideProvider

/**
 * Abstraction layer interface for a provider capable of executing builds.
 */
interface BuildProvider : XideProvider {
    suspend fun executeBuild(request: BuildRequest): BuildResult
    fun canBuild(request: BuildRequest): Boolean
}
