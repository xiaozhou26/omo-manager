package com.omo.manager.util

import com.omo.manager.native.RustBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object ScriptExecutor {

    suspend fun executeInline(
        script: String,
        useRoot: Boolean = RootAccess.hasRoot
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = RustBridge.executeScript(script, useRoot)
            val json = JSONObject(result)
            val output = json.optString("output", "")
            val success = json.optBoolean("success", false)
            if (success) {
                Result.success(output)
            } else {
                Result.failure(Exception(output.ifEmpty { "Script execution failed" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeFile(
        scriptPath: String,
        useRoot: Boolean = RootAccess.hasRoot
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = RustBridge.executeScriptWithOutput(scriptPath, useRoot)
            val json = JSONObject(result)
            val output = json.optString("output", "")
            val success = json.optBoolean("success", false)
            if (success) {
                Result.success(output)
            } else {
                Result.failure(Exception(output.ifEmpty { "Script execution failed" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
