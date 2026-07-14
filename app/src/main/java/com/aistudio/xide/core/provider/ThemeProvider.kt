package com.aistudio.xide.core.provider

/**
 * Abstraction for Theming system.
 */
interface ThemeProvider : XideProvider {
    val themeId: String
    val isDark: Boolean
    // Add Design Token mapping here in the future
}
