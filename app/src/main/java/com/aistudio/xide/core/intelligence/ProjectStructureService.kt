package com.aistudio.xide.core.intelligence

/**
 * Foundational contract for querying and tracking the structural topology of the workspace.
 * Models modules, files, dependencies, and their cross-module relationships.
 */
interface ProjectStructureService {
    /**
     * Resolves the current workspace hierarchy and returns the root node.
     */
    suspend fun getStructure(projectPath: String): WorkspaceStructureNode

    /**
     * Resolves all dependencies (both internal and external package dependencies) for a given module.
     */
    suspend fun getModuleDependencies(modulePath: String): List<StructureDependency>

    /**
     * Tracks code relationships or import chains for structural analysis.
     */
    suspend fun getStructuralRelationships(projectPath: String): List<StructureRelationship>
}

/**
 * Node in the tree of the project/workspace structural topology.
 */
data class WorkspaceStructureNode(
    val name: String,
    val path: String,
    val type: StructureNodeType,
    val children: List<WorkspaceStructureNode> = emptyList(),
    val properties: Map<String, String> = emptyMap()
)

enum class StructureNodeType {
    WORKSPACE, MODULE, DIRECTORY, FILE, PACKAGE
}

/**
 * Foundational model representing a package or module dependency.
 */
data class StructureDependency(
    val name: String,
    val version: String,
    val scope: DependencyScope,
    val type: DependencyType
)

enum class DependencyScope {
    API, IMPLEMENTATION, COMPILE_ONLY, RUNTIME_ONLY, TEST_IMPLEMENTATION
}

enum class DependencyType {
    INTERNAL_MODULE, EXTERNAL_MAVEN, EXTERNAL_NPM, EXTERNAL_GRADLE_PLUGIN
}

/**
 * Models import chains or invocation dependencies between modules or source files.
 */
data class StructureRelationship(
    val sourcePath: String,
    val targetPath: String,
    val type: RelationshipType
)

enum class RelationshipType {
    DEPENDS_ON, IMPORTS, EXTENDS, IMPLEMENTS
}

/**
 * Extension contract for tracking, prioritizing, and categorizing diagnostics across the project
 * to inject as structured context into intelligence requests.
 */
interface DiagnosticsContextResolver {
    /**
     * Aggregates active problems and returns structured diagnostic context.
     */
    suspend fun resolveDiagnosticsContext(projectPath: String): DiagnosticsContextSnapshot
}

data class DiagnosticsContextSnapshot(
    val errorCount: Int,
    val warningCount: Int,
    val fileDiagnosticList: Map<String, List<DiagnosticDetail>>
)

data class DiagnosticDetail(
    val code: String,
    val severity: String,
    val message: String,
    val line: Int,
    val column: Int
)
