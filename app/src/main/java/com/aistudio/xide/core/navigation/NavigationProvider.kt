package com.aistudio.xide.core.navigation

import com.aistudio.xide.core.provider.XideProvider

interface NavigationProvider : XideProvider {
    val destinationRegistry: DestinationRegistry
    fun navigateTo(route: String)
    fun goBack(): Boolean
}
