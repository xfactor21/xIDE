package com.aistudio.xide.core.indexing

import com.aistudio.xide.core.intelligence.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import java.util.UUID

class ProjectIndexerImpl : ProjectIndexer {
    private val indexes = mutableMapOf<String, ProjectIndex>()
    private val activeTasks = mutableMapOf<String, IndexingTaskImpl>()
    private val fileLastModifiedTimes = mutableMapOf<String, Map<String, Long>>()

    override fun indexProject(projectPath: String): IndexingTask {
        val taskId = UUID.randomUUID().toString()
        val task = IndexingTaskImpl(taskId, projectPath) {
            performIndexing(projectPath)
        }
        activeTasks[projectPath] = task
        task.start()
        return task
    }

    override suspend fun getIndex(projectPath: String): ProjectIndex? {
        return indexes[projectPath]
    }

    override suspend fun getContext(projectPath: String): ProjectContext {
        val index = getIndex(projectPath)
        return ProjectContext(
            projectPath = projectPath,
            activeFiles = index?.files ?: emptyList(),
            architectureSummary = "Lightweight project index with ${index?.files?.size ?: 0} files.",
            contextPreparation = "Ready",
            projectType = index?.metadata?.get("projectType") ?: "unknown",
            languages = index?.metadata?.get("languages")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
            frameworks = index?.metadata?.get("frameworks")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
            buildSystem = index?.metadata?.get("buildSystem") ?: "unknown",
            dependencies = index?.metadata?.get("dependencies")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        )
    }

    // Helper queries requested
    fun findClassDefinition(projectPath: String, className: String): ProjectSymbol? {
        val index = indexes[projectPath] ?: return null
        return index.symbols.firstOrNull { it.name == className && it.type == "Class" }
    }

    fun findFilesDependingOn(projectPath: String, className: String): List<String> {
        val index = indexes[projectPath] ?: return emptyList()
        // Find files that import this class or reference it
        return index.relationships
            .filter { it.targetSymbol == className && it.relationshipType == "imports" }
            .map { it.sourceSymbol }
    }

    fun getChangedFilesSinceLastBuild(projectPath: String, buildTimestamp: Long): List<String> {
        val index = indexes[projectPath] ?: return emptyList()
        return index.files.filter { filePath ->
            val currentFile = File(filePath)
            currentFile.exists() && currentFile.lastModified() > buildTimestamp
        }
    }

    suspend fun performIndexing(projectPath: String): ProjectIndex = withContext(Dispatchers.IO) {
        val root = File(projectPath)
        if (!root.exists() || !root.isDirectory) {
            throw IllegalArgumentException("Invalid project path")
        }

        val allFiles = mutableListOf<File>()
        findFilesRecursively(root, allFiles)

        val filesList = allFiles.map { it.absolutePath }
        val symbols = mutableListOf<ProjectSymbol>()
        val relationships = mutableListOf<ProjectRelationship>()
        val dependencies = mutableSetOf<String>()
        val languages = mutableSetOf<String>()
        val frameworks = mutableSetOf<String>()
        var projectType = "kotlin"
        var buildSystem = "none"

        val currentModifications = mutableMapOf<String, Long>()
        var kotlinVersion = "unknown"
        var agpVersion = "unknown"

        for (file in allFiles) {
            currentModifications[file.absolutePath] = file.lastModified()
            val content = try { file.readText() } catch (e: Exception) { "" }
            
            // Language detection
            if (file.name.endsWith(".gradle") || file.name.endsWith(".gradle.kts")) {
                buildSystem = "gradle"
                parseGradleDependencies(content, dependencies)
                
                val agpRegex = Regex("""id\s*\(\s*["']com\.android\.(?:application|library)["']\s*\)\s*version\s*["']([^"']+)["']""")
                val agpMatch = agpRegex.find(content)
                if (agpMatch != null) {
                    agpVersion = agpMatch.groupValues[1]
                }

                val kotlinRegex = Regex("""id\s*\(\s*["']org\.jetbrains\.kotlin\.(?:android|jvm)["']\s*\)\s*version\s*["']([^"']+)["']|kotlin\s*\(\s*["'](?:android|jvm)["']\s*\)\s*version\s*["']([^"']+)["']""")
                val kotlinMatch = kotlinRegex.find(content)
                if (kotlinMatch != null) {
                    kotlinVersion = if (kotlinMatch.groupValues[1].isNotEmpty()) kotlinMatch.groupValues[1] else kotlinMatch.groupValues[2]
                }
            }

            when (file.extension) {
                "kt" -> {
                    languages.add("Kotlin")
                    if (content.contains("import androidx.compose")) {
                        frameworks.add("Jetpack Compose")
                    }
                    parseKotlinFile(file, content, symbols, relationships)
                }
                "kts" -> {
                    languages.add("Kotlin")
                }
                "java" -> {
                    languages.add("Java")
                }
                "xml" -> {
                    if (file.name == "AndroidManifest.xml") {
                        projectType = "android"
                        frameworks.add("Android SDK")
                    }
                }
            }
        }

        fileLastModifiedTimes[projectPath] = currentModifications

        val metadata = mapOf(
            "projectType" to projectType,
            "buildSystem" to buildSystem,
            "languages" to languages.joinToString(","),
            "frameworks" to frameworks.joinToString(","),
            "dependencies" to dependencies.joinToString(","),
            "kotlinVersion" to kotlinVersion,
            "agpVersion" to agpVersion
        )

        val index = ProjectIndex(
            projectPath = projectPath,
            files = filesList,
            symbols = symbols,
            relationships = relationships,
            metadata = metadata
        )
        indexes[projectPath] = index
        index
    }

    private fun findFilesRecursively(dir: File, result: MutableList<File>) {
        val list = dir.listFiles() ?: return
        for (file in list) {
            // Exclude common build/IDE directories
            if (file.name in listOf(".git", ".gradle", "build", "node_modules", "bin", "obj")) {
                continue
            }
            if (file.isDirectory) {
                findFilesRecursively(file, result)
            } else {
                result.add(file)
            }
        }
    }

    private fun parseKotlinFile(
        file: File,
        content: String,
        symbols: MutableList<ProjectSymbol>,
        relationships: MutableList<ProjectRelationship>
    ) {
        val lines = content.lines()
        val packageNameRegex = Regex("""package\s+([a-zA-Z_][a-zA-Z0-9._]*)""")
        val importRegex = Regex("""import\s+([a-zA-Z_][a-zA-Z0-9._]*)""")
        val classRegex = Regex("""class\s+([a-zA-Z_][a-zA-Z0-9_]*)""")
        val interfaceRegex = Regex("""interface\s+([a-zA-Z_][a-zA-Z0-9_]*)""")
        val objectRegex = Regex("""object\s+([a-zA-Z_][a-zA-Z0-9_]*)""")
        val functionRegex = Regex("""fun\s+([a-zA-Z_][a-zA-Z0-9_]*)""")
        val propertyRegex = Regex("""(val|var)\s+([a-zA-Z_][a-zA-Z0-9_]*)""")
        val constructorRegex = Regex("""constructor\s*\(""")
        val annotationRegex = Regex("""(@[a-zA-Z_][a-zA-Z0-9_]*)""")
        val visibilityRegex = Regex("""\b(private|protected|internal|public)\b""")

        var packageName = ""
        val pendingAnnotations = mutableListOf<String>()

        for ((index, line) in lines.withIndex()) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("//") || trimmedLine.startsWith("/*") || trimmedLine.startsWith("*")) {
                continue
            }
            if (trimmedLine.isEmpty()) {
                continue
            }

            val packMatch = packageNameRegex.find(line)
            if (packMatch != null) {
                packageName = packMatch.groupValues[1]
            }

            val importMatch = importRegex.find(line)
            if (importMatch != null) {
                val importedClass = importMatch.groupValues[1]
                relationships.add(
                    ProjectRelationship(
                        sourceSymbol = file.absolutePath,
                        targetSymbol = importedClass.substringAfterLast("."),
                        relationshipType = "imports"
                    )
                )
            }

            val lineAnnotations = annotationRegex.findAll(line).map { it.groupValues[1] }.toList()
            pendingAnnotations.addAll(lineAnnotations)

            val visibilityMatch = visibilityRegex.find(line)
            val visibility = visibilityMatch?.groupValues?.get(1) ?: "public"
            val isOverride = line.contains("override")

            val classMatch = classRegex.find(line)
            if (classMatch != null) {
                val className = classMatch.groupValues[1]
                val extendsList = mutableListOf<String>()
                val inheritMatch = Regex("""class\s+([a-zA-Z_][a-zA-Z0-9_]*)(?:\s*\([^)]*\))?\s*:\s*([^{]+)""").find(line)
                if (inheritMatch != null) {
                    val bases = inheritMatch.groupValues[2]
                        .split(",")
                        .map { it.trim().substringBefore("(").substringBefore("<").trim() }
                        .filter { it.isNotEmpty() }
                    extendsList.addAll(bases)
                    bases.forEach { base ->
                        relationships.add(
                            ProjectRelationship(
                                sourceSymbol = className,
                                targetSymbol = base,
                                relationshipType = "extends"
                            )
                        )
                    }
                }
                symbols.add(
                    ProjectSymbol(
                        name = className,
                        type = "Class",
                        location = "${file.absolutePath}:${index + 1}",
                        language = "Kotlin",
                        references = emptyList(),
                        visibility = visibility,
                        isOverride = isOverride,
                        annotations = pendingAnnotations.toList(),
                        extendsList = extendsList
                    )
                )
                pendingAnnotations.clear()
                continue
            }

            val interfaceMatch = interfaceRegex.find(line)
            if (interfaceMatch != null) {
                val interfaceName = interfaceMatch.groupValues[1]
                val extendsList = mutableListOf<String>()
                val inheritMatch = Regex("""interface\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*:\s*([^{]+)""").find(line)
                if (inheritMatch != null) {
                    val bases = inheritMatch.groupValues[2]
                        .split(",")
                        .map { it.trim().substringBefore("(").substringBefore("<").trim() }
                        .filter { it.isNotEmpty() }
                    extendsList.addAll(bases)
                    bases.forEach { base ->
                        relationships.add(
                            ProjectRelationship(
                                sourceSymbol = interfaceName,
                                targetSymbol = base,
                                relationshipType = "extends"
                            )
                        )
                    }
                }
                symbols.add(
                    ProjectSymbol(
                        name = interfaceName,
                        type = "Interface",
                        location = "${file.absolutePath}:${index + 1}",
                        language = "Kotlin",
                        references = emptyList(),
                        visibility = visibility,
                        isOverride = isOverride,
                        annotations = pendingAnnotations.toList(),
                        extendsList = extendsList
                    )
                )
                pendingAnnotations.clear()
                continue
            }

            val objectMatch = objectRegex.find(line)
            if (objectMatch != null) {
                val objectName = objectMatch.groupValues[1]
                symbols.add(
                    ProjectSymbol(
                        name = objectName,
                        type = "Object",
                        location = "${file.absolutePath}:${index + 1}",
                        language = "Kotlin",
                        references = emptyList(),
                        visibility = visibility,
                        isOverride = isOverride,
                        annotations = pendingAnnotations.toList(),
                        extendsList = emptyList()
                    )
                )
                pendingAnnotations.clear()
                continue
            }

            val functionMatch = functionRegex.find(line)
            if (functionMatch != null) {
                val functionName = functionMatch.groupValues[1]
                symbols.add(
                    ProjectSymbol(
                        name = functionName,
                        type = "Function",
                        location = "${file.absolutePath}:${index + 1}",
                        language = "Kotlin",
                        references = emptyList(),
                        visibility = visibility,
                        isOverride = isOverride,
                        annotations = pendingAnnotations.toList(),
                        extendsList = emptyList()
                    )
                )
                pendingAnnotations.clear()
                continue
            }

            val propertyMatch = propertyRegex.find(line)
            if (propertyMatch != null) {
                val propertyName = propertyMatch.groupValues[2]
                symbols.add(
                    ProjectSymbol(
                        name = propertyName,
                        type = "Property",
                        location = "${file.absolutePath}:${index + 1}",
                        language = "Kotlin",
                        references = emptyList(),
                        visibility = visibility,
                        isOverride = isOverride,
                        annotations = pendingAnnotations.toList(),
                        extendsList = emptyList()
                    )
                )
                pendingAnnotations.clear()
                continue
            }

            val constructorMatch = constructorRegex.find(line)
            if (constructorMatch != null) {
                symbols.add(
                    ProjectSymbol(
                        name = "constructor",
                        type = "Constructor",
                        location = "${file.absolutePath}:${index + 1}",
                        language = "Kotlin",
                        references = emptyList(),
                        visibility = visibility,
                        isOverride = isOverride,
                        annotations = pendingAnnotations.toList(),
                        extendsList = emptyList()
                    )
                )
                pendingAnnotations.clear()
                continue
            }

            if (trimmedLine.isNotEmpty() && lineAnnotations.isEmpty() && packMatch == null && importMatch == null) {
                pendingAnnotations.clear()
            }
        }
    }

    private fun parseGradleDependencies(content: String, dependencies: MutableSet<String>) {
        val depRegex = Regex("""(?:implementation|api|testImplementation)\s*\(?\s*["']([^"']+)["']\s*\)?""")
        depRegex.findAll(content).forEach { match ->
            dependencies.add(match.groupValues[1])
        }
    }
}

class IndexingTaskImpl(
    override val taskId: String,
    override val projectPath: String,
    private val block: suspend () -> ProjectIndex
) : IndexingTask {
    private val _progress = MutableStateFlow(0f)
    override val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _status = MutableStateFlow(IndexingStatus.PENDING)
    override val status: StateFlow<IndexingStatus> = _status.asStateFlow()

    private val job = CompletableDeferred<ProjectIndex>()

    fun start() {
        _status.value = IndexingStatus.RUNNING
        _progress.value = 0.1f
        CoroutineScope(Dispatchers.Default).launch {
            try {
                _progress.value = 0.5f
                val result = block()
                job.complete(result)
                _progress.value = 1.0f
                _status.value = IndexingStatus.COMPLETED
            } catch (e: Exception) {
                job.completeExceptionally(e)
                _status.value = IndexingStatus.FAILED
            }
        }
    }

    override suspend fun await() {
        job.await()
    }

    override fun cancel() {
        _status.value = IndexingStatus.CANCELED
    }
}
