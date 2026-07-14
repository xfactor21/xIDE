package com.aistudio.xide.core.build

import com.aistudio.xide.core.diagnostics.*
import com.aistudio.xide.core.provider.ProviderHealth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Executes safe, controlled Gradle builds on the workspace filesystem.
 * Decouples the platform from direct CLI invocation and parses raw outputs into structural Diagnostics.
 */
class GradleBuildProvider(
    private val rootPathProvider: () -> String
) : BuildProvider {

    override val providerId: String = "gradle_build_provider"
    override val providerName: String = "Gradle Build Provider"
    override val providerVersion: String = "1.0.0"
    override val supportedFeatures: List<String> = listOf("gradle_build", "diagnostics_extraction")
    override val requirements: List<String> = listOf("gradle_wrapper", "jdk")
    override val limitations: List<String> = listOf("local_machine_only")
    override val description: String = "Executes Gradle builds, monitors compiler state, and extracts rich compiler diagnostics."
    override val author: String = "xIDE Architect"
    override val compatibilityVersion: String = "1.0.0"

    private var health = ProviderHealth.HEALTHY

    override suspend fun initialize() {
        health = ProviderHealth.HEALTHY
    }

    override suspend fun shutdown() {
        health = ProviderHealth.DEGRADED
    }

    override suspend fun healthCheck(): ProviderHealth {
        val rootDir = File(rootPathProvider())
        val gradlew = File(rootDir, "gradlew")
        return if (gradlew.exists() && gradlew.canExecute()) {
            ProviderHealth.HEALTHY
        } else {
            ProviderHealth.DEGRADED
        }
    }

    override fun canBuild(request: BuildRequest): Boolean {
        return request.operation in listOf("assembleDebug", "compileDebugKotlin", "test", "lint", "build")
    }

    override suspend fun executeBuild(request: BuildRequest): BuildResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val rootDir = File(rootPathProvider())
        val gradlew = File(rootDir, "gradlew")

        // 0. Enforce whitelisted task execution
        if (!canBuild(request)) {
            val endTime = System.currentTimeMillis()
            return@withContext BuildResult(
                success = false,
                artifactInfo = null,
                diagnostics = listOf(
                    BuildDiagnostic(
                        category = "security_validation",
                        severity = DiagnosticSeverity.ERROR,
                        message = "Unauthorized Gradle task requested: '${request.operation}'. Allowed tasks: assembleDebug, compileDebugKotlin, test, lint, build."
                    )
                ),
                startTime = startTime,
                endTime = endTime,
                message = "Build rejected: Unauthorized or unvalidated Gradle task requested."
            )
        }

        // 1. Validate environment
        if (!gradlew.exists()) {
            val endTime = System.currentTimeMillis()
            return@withContext BuildResult(
                success = false,
                artifactInfo = null,
                diagnostics = listOf(
                    BuildDiagnostic(
                        category = "environment",
                        severity = DiagnosticSeverity.ERROR,
                        message = "Gradle wrapper not found at path: ${gradlew.absolutePath}"
                    )
                ),
                startTime = startTime,
                endTime = endTime,
                message = "Build failed: Environment support is incomplete (Missing Gradle wrapper)."
            )
        }

        // 2. Start controlled build execution
        try {
            val pb = ProcessBuilder()
            pb.command(listOf("./gradlew", request.operation))
            pb.directory(rootDir)
            
            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val outputLog = StringBuilder()
            val diagnostics = mutableListOf<BuildDiagnostic>()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                outputLog.append(line).append("\n")
                parseDiagnosticLine(line!!, diagnostics)
            }
            while (errorReader.readLine().also { line = it } != null) {
                outputLog.append(line).append("\n")
                parseDiagnosticLine(line!!, diagnostics)
            }

            val exitCode = process.waitFor()
            val endTime = System.currentTimeMillis()
            val success = exitCode == 0

            if (!success && diagnostics.none { it.severity == DiagnosticSeverity.ERROR }) {
                diagnostics.add(
                    BuildDiagnostic(
                        category = "build_execution",
                        severity = DiagnosticSeverity.ERROR,
                        message = "Gradle task '${request.operation}' failed with exit code $exitCode",
                        rawOutput = outputLog.toString()
                    )
                )
            }

            var artifactInfo: ArtifactInfo? = null
            if (success && request.operation == "assembleDebug") {
                val apkDir = File(rootDir, "app/build/outputs/apk/debug")
                val apkFile = File(apkDir, "app-debug.apk")
                if (apkFile.exists()) {
                    artifactInfo = ArtifactInfo(
                        path = apkFile.absolutePath,
                        timestamp = apkFile.lastModified(),
                        variant = request.variant,
                        metadata = mapOf("size_bytes" to apkFile.length().toString())
                    )
                }
            }

            BuildResult(
                success = success,
                artifactInfo = artifactInfo,
                diagnostics = diagnostics,
                startTime = startTime,
                endTime = endTime,
                message = if (success) "Build completed successfully." else "Build failed with exit code $exitCode."
            )

        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            BuildResult(
                success = false,
                artifactInfo = null,
                diagnostics = listOf(
                    BuildDiagnostic(
                        category = "execution_exception",
                        severity = DiagnosticSeverity.ERROR,
                        message = "Exception during build execution: ${e.message}",
                        rawOutput = e.stackTraceToString()
                    )
                ),
                startTime = startTime,
                endTime = endTime,
                message = "Build failed due to internal exception: ${e.message}"
            )
        }
    }

    private fun parseDiagnosticLine(line: String, list: MutableList<BuildDiagnostic>) {
        val kotlinPattern = """([ew]):\s+([^:]+):\s+\((\d+),\s+(\d+)\):\s+(.*)""".toRegex()
        val matchKotlin = kotlinPattern.find(line)
        if (matchKotlin != null) {
            val (sevChar, filePath, lineStr, colStr, msg) = matchKotlin.destructured
            val severity = if (sevChar == "e") DiagnosticSeverity.ERROR else DiagnosticSeverity.WARNING
            val location = DiagnosticLocation(filePath = filePath, line = lineStr.toIntOrNull(), column = colStr.toIntOrNull())
            val compDiag = CompilerDiagnostic(
                code = null,
                message = msg,
                severity = severity,
                location = location
            )
            list.add(
                BuildDiagnostic(
                    category = "compiler",
                    severity = severity,
                    message = msg,
                    compilerDiagnostic = compDiag,
                    rawOutput = line
                )
            )
            return
        }

        val genericPattern = """([^:]+):(\d+):\s+(error|warning):\s+(.*)""".toRegex()
        val matchGeneric = genericPattern.find(line)
        if (matchGeneric != null) {
            val (filePath, lineStr, sevStr, msg) = matchGeneric.destructured
            val severity = if (sevStr.lowercase() == "error") DiagnosticSeverity.ERROR else DiagnosticSeverity.WARNING
            val location = DiagnosticLocation(filePath = filePath, line = lineStr.toIntOrNull(), column = null)
            val compDiag = CompilerDiagnostic(
                code = null,
                message = msg,
                severity = severity,
                location = location
            )
            list.add(
                BuildDiagnostic(
                    category = "compiler",
                    severity = severity,
                    message = msg,
                    compilerDiagnostic = compDiag,
                    rawOutput = line
                )
            )
            return
        }
    }
}
