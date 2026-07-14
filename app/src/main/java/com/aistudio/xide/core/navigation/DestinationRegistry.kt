package com.aistudio.xide.core.navigation

class DestinationRegistry {
    private val destinations = mutableMapOf<String, Any>()

    fun registerDestination(route: String, screen: Any) {
        destinations[route] = screen
    }

    fun getDestination(route: String): Any? {
        return destinations[route]
    }
}
