package com.aistudio.xide.core.template

/**
 * Foundation for the Project Template Engine.
 */
interface TemplateProvider {
    suspend fun getAvailableTemplates(): List<ProjectTemplate>
    suspend fun generateProject(templateId: String, outputDirectory: String, parameters: Map<String, String>)
}

data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val requiredParameters: List<String>
)
