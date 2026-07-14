package com.aistudio.xide.core.command

data class ActionDescriptor(
    val commandId: String,
    val description: String,
    val arguments: Map<String, String> = emptyMap()
)
