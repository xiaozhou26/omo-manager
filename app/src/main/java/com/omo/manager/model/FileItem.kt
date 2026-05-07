package com.omo.manager.model

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val permissions: String,
    val modified: Long,
    val isSelected: Boolean = false
) {
    val extension: String
        get() = if (name.contains(".")) name.substringAfterLast(".") else ""

    val isScriptFile: Boolean
        get() = extension in listOf("sh", "bash", "zsh")

    val isTextFile: Boolean
        get() = extension in listOf(
            "txt", "sh", "bash", "zsh", "conf", "cfg", "ini",
            "properties", "xml", "json", "yaml", "yml", "toml",
            "md", "log", "csv", "py", "js", "ts", "kt", "java",
            "c", "cpp", "h", "rs", "go", "rb", "pl", "lua"
        )

    val displaySize: String
        get() = if (isDirectory) "" else formatSize(size)

    companion object {
        fun formatSize(size: Long): String = when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", size / (1024.0 * 1024 * 1024))
        }

        fun fromEntryString(parentPath: String, entry: String): FileItem? {
            val parts = entry.split("|")
            if (parts.size < 4) return null
            val name = parts[0]
            val isDir = parts[1].toBooleanStrictOrNull() ?: false
            val size = parts[2].toLongOrNull() ?: 0
            val perms = parts.getOrNull(3) ?: ""
            val modified = parts.getOrNull(4)?.toLongOrNull() ?: 0L
            val fullPath = if (parentPath.endsWith("/")) "$parentPath$name" else "$parentPath/$name"
            return FileItem(
                name = name,
                path = fullPath,
                isDirectory = isDir,
                size = size,
                permissions = perms,
                modified = modified
            )
        }
    }
}
