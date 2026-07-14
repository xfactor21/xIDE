package com.aistudio.xide.core.dependency

import com.aistudio.xide.core.provider.XideProvider

data class PackageDefinition(
    val name: String,
    val version: String,
    val source: String
)

data class PackageInstallRequest(
    val packages: List<PackageDefinition>,
    val targetDirectory: String
)

data class PackageInstallResult(
    val success: Boolean,
    val installedPackages: List<PackageDefinition>,
    val error: Throwable?
)

interface PackageManagerProvider : XideProvider {
    val supportedPackageTypes: List<String>
    suspend fun installPackages(request: PackageInstallRequest): PackageInstallResult
    suspend fun removePackages(request: PackageInstallRequest): PackageInstallResult
    suspend fun resolveDependencies(projectPath: String): List<PackageDefinition>
}
