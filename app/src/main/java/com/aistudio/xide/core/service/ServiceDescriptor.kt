package com.aistudio.xide.core.service

import com.aistudio.xide.core.provider.ProviderHealth

data class ServiceDescriptor(
    val providerType: String,
    val pluginId: String,
    val priority: Int,
    val health: ProviderHealth,
    val capabilities: List<String> = emptyList(),
    val permissions: List<String> = emptyList()
)
