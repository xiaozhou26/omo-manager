package com.omo.manager.model

data class ScriptResult(
    val output: String,
    val success: Boolean,
    val exitCode: Int = if (success) 0 else 1
)
