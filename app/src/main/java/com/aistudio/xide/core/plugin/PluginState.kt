package com.aistudio.xide.core.plugin

enum class PluginState {
    DISCOVERED,
    VALIDATING,
    INSTALLED,
    ENABLED,
    INITIALIZING,
    ACTIVE,
    DISABLED,
    FAILED,
    REMOVED
}
