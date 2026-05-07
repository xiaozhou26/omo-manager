package com.omo.manager.util

import com.omo.manager.native.RustBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootAccess {
    var hasRoot: Boolean = false
        private set

    suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            hasRoot = RustBridge.checkRootAccess()
            hasRoot
        } catch (e: Exception) {
            hasRoot = false
            false
        }
    }

    fun canAccessPath(path: String): Boolean {
        if (!hasRoot) return false
        return path.startsWith("/data/") ||
                path.startsWith("/system/") ||
                path.startsWith("/vendor/") ||
                path.startsWith("/proc/")
    }
}
