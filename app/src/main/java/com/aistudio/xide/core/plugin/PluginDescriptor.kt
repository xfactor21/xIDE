package com.aistudio.xide.core.plugin

data class PluginDescriptor(
    val manifest: PluginManifest,
    val location: String,
    val state: PluginState,
    val providers: List<String>,
    val error: String? = null
)
