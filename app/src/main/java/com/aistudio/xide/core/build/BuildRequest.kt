package com.aistudio.xide.core.build

data class BuildRequest(
    val target: String,
    val variant: String,
    val operation: String
)
