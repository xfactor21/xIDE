package com.aistudio.xide.core.intelligence

import com.aistudio.xide.core.command.ActionDescriptor

data class ActionPlan(
    val title: String,
    val commands: List<ActionDescriptor>,
    val requiresApproval: Boolean
)
