package com.omo.manager.native

object RustBridge {
    init {
        System.loadLibrary("omo_native")
    }

    external fun listDirectory(path: String, useRoot: Boolean): Array<String>?
    external fun executeScript(script: String, useRoot: Boolean): String
    external fun executeScriptWithOutput(scriptPath: String, useRoot: Boolean): String
    external fun copyFile(src: String, dst: String, useRoot: Boolean): Boolean
    external fun moveFile(src: String, dst: String, useRoot: Boolean): Boolean
    external fun deleteFile(path: String, useRoot: Boolean): Boolean
    external fun renameFile(oldPath: String, newPath: String, useRoot: Boolean): Boolean
    external fun createDirectory(path: String, useRoot: Boolean): Boolean
    external fun createFile(path: String, useRoot: Boolean): Boolean
    external fun getFilePermissions(path: String, useRoot: Boolean): String
    external fun setFilePermissions(path: String, mode: String, useRoot: Boolean): Boolean
    external fun readTextFile(path: String, useRoot: Boolean): String
    external fun writeTextFile(path: String, content: String, useRoot: Boolean): Boolean
    external fun checkRootAccess(): Boolean
}
