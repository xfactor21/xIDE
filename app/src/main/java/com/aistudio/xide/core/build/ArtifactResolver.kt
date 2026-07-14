package com.aistudio.xide.core.build

import java.io.File

/**
 * Validates and resolves real, evidence-based build artifacts (such as APKs) produced by Gradle.
 */
class ArtifactResolver(private val rootPathProvider: () -> String) {

    fun resolveArtifact(operation: String, variant: String, exitCode: Int): ArtifactInfo? {
        if (exitCode != 0) return null

        val rootDir = File(rootPathProvider())
        val (relativeDir, expectedPrefix) = when (operation) {
            "assembleDebug" -> Pair("app/build/outputs/apk/debug", "app-debug")
            "assembleRelease" -> Pair("app/build/outputs/apk/release", "app-release")
            else -> return null
        }

        val outputDir = File(rootDir, relativeDir)
        if (!outputDir.exists() || !outputDir.isDirectory) return null

        // Find the first valid APK that meets all verification criteria
        val apkFile = outputDir.listFiles()?.firstOrNull { file ->
            file.isFile &&
            file.name.endsWith(".apk") &&
            file.name.startsWith(expectedPrefix) &&
            file.length() > 0
        } ?: return null

        // Security check: Must reside within the canonical root directory
        val canonicalRoot = rootDir.canonicalPath
        val canonicalApk = apkFile.canonicalPath
        if (!canonicalApk.startsWith(canonicalRoot + File.separator)) {
            return null
        }

        return ArtifactInfo(
            path = apkFile.absolutePath,
            timestamp = apkFile.lastModified(),
            variant = variant,
            metadata = mapOf(
                "filename" to apkFile.name,
                "size_bytes" to apkFile.length().toString(),
                "build_type" to variant
            )
        )
    }
}
