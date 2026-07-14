package com.aistudio.xide.core.plugin

data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val requiredXideVersion: String,
    val compatibilityRange: String,
    val exportedProviders: List<String>,
    val permissions: List<String>,
    val dependencies: List<PluginDependency>
)

data class PluginDependency(
    val pluginId: String,
    val versionRange: String
)
