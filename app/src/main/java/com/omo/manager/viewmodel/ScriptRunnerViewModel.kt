package com.omo.manager.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omo.manager.native.RustBridge
import com.omo.manager.util.RootAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScriptRunnerViewModel : ViewModel() {
    var scriptContent by mutableStateOf("")
        private set
    var output by mutableStateOf("")
        private set
    var isRunning by mutableStateOf(false)
        private set
    var useRoot by mutableStateOf(RootAccess.hasRoot)
        private set
    var isRootAvailable by mutableStateOf(false)
        private set
    var scriptPath by mutableStateOf("")
        private set

    private var runJob: Job? = null

    fun checkRoot() {
        viewModelScope.launch(Dispatchers.IO) {
            isRootAvailable = RootAccess.checkRoot()
            useRoot = isRootAvailable
        }
    }

    fun updateScript(content: String) {
        scriptContent = content
    }

    fun setScriptPath(path: String) {
        scriptPath = path
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = RustBridge.readTextFile(path, useRoot)
                scriptContent = content
            } catch (_: Exception) {
            }
        }
    }

    fun toggleRoot() {
        useRoot = !useRoot && isRootAvailable
    }

    fun runScript() {
        if (isRunning) return
        isRunning = true
        output = ""

        runJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = if (scriptPath.isNotEmpty()) {
                    RustBridge.executeScriptWithOutput(scriptPath, useRoot)
                } else {
                    RustBridge.executeScript(scriptContent, useRoot)
                }

                val json = org.json.JSONObject(result)
                val out = json.optString("output", "")
                val success = json.optBoolean("success", false)

                launch(Dispatchers.Main) {
                    output = buildString {
                        if (!success) append("❌ Exit with error\n")
                        append(out)
                        if (out.isNotEmpty() && !out.endsWith("\n")) append("\n")
                        append("--- Process ${if (success) "completed" else "failed"} ---\n")
                    }
                    isRunning = false
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    output = "Error: ${e.message}\n"
                    isRunning = false
                }
            }
        }
    }

    fun stopScript() {
        runJob?.cancel()
        isRunning = false
        output += "\n--- Process interrupted ---\n"
    }

    fun clearOutput() {
        output = ""
    }

    fun clearScript() {
        scriptContent = ""
        scriptPath = ""
    }

    fun saveScript(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (RustBridge.writeTextFile(path, scriptContent, useRoot)) {
                scriptPath = path
            }
        }
    }
}
