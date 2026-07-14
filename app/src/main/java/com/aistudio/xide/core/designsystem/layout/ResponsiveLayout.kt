package com.aistudio.xide.core.designsystem.layout

enum class WindowSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
    DESKTOP
}

object ResponsiveLayout {
    const val COMPACT_MAX_WIDTH = 600
    const val MEDIUM_MAX_WIDTH = 840
    const val EXPANDED_MAX_WIDTH = 1200

    fun calculateWindowSizeClass(windowWidthDp: Int): WindowSizeClass {
        return when {
            windowWidthDp < COMPACT_MAX_WIDTH -> WindowSizeClass.COMPACT
            windowWidthDp < MEDIUM_MAX_WIDTH -> WindowSizeClass.MEDIUM
            windowWidthDp < EXPANDED_MAX_WIDTH -> WindowSizeClass.EXPANDED
            else -> WindowSizeClass.DESKTOP
        }
    }
}
