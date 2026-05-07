package com.omo.manager.viewmodel

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omo.manager.model.FileItem
import com.omo.manager.native.RustBridge
import com.omo.manager.util.RootAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class PaneState(
    val currentPath: String = "/storage/emulated/0",
    val files: List<FileItem> = emptyList(),
    val selectedFiles: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FileBrowserViewModel : ViewModel() {
    var leftPane by mutableStateOf(PaneState(currentPath = "/storage/emulated/0"))
        private set
    var rightPane by mutableStateOf(PaneState(currentPath = "/data/adb"))
        private set
    var activePane by mutableStateOf(PaneSide.LEFT)
        private set
    var isRootEnabled by mutableStateOf(false)
        private set
    var bookmarks by mutableStateOf<List<String>>(emptyList())
        private set
    var showBookmarkDialog by mutableStateOf(false)
        private set
    var showNewFileDialog by mutableStateOf(false)
        private set
    var showNewFolderDialog by mutableStateOf(false)
        private set
    var showRenameDialog by mutableStateOf<FileItem?>(null)
        private set
    var showDeleteConfirm by mutableStateOf<List<FileItem>>(emptyList())
        private set
    var showPropertiesDialog by mutableStateOf<FileItem?>(null)
        private set
    var showPathInputDialog by mutableStateOf(false)
        private set
    var showSearchDialog by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set

    enum class PaneSide { LEFT, RIGHT }

    fun setActivePane(side: PaneSide) {
        activePane = side
    }

    fun currentPane(): PaneState = when (activePane) {
        PaneSide.LEFT -> leftPane
        PaneSide.RIGHT -> rightPane
    }

    fun otherPane(): PaneState = when (activePane) {
        PaneSide.LEFT -> rightPane
        PaneSide.RIGHT -> leftPane
    }

    private fun updateCurrentPane(transform: (PaneState) -> PaneState) {
        when (activePane) {
            PaneSide.LEFT -> leftPane = transform(leftPane)
            PaneSide.RIGHT -> rightPane = transform(rightPane)
        }
    }

    fun checkRoot() {
        viewModelScope.launch(Dispatchers.IO) {
            isRootEnabled = RootAccess.checkRoot()
        }
    }

    fun loadDirectory(path: String, pane: PaneSide? = null) {
        val targetPane = pane ?: activePane
        val useRoot = isRootEnabled && RootAccess.canAccessPath(path)

        when (targetPane) {
            PaneSide.LEFT -> leftPane = leftPane.copy(isLoading = true, error = null)
            PaneSide.RIGHT -> rightPane = rightPane.copy(isLoading = true, error = null)
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entries = RustBridge.listDirectory(path, useRoot)
                if (entries != null) {
                    val files = entries.mapNotNull { entry ->
                        FileItem.fromEntryString(path, entry)
                    }.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })

                    when (targetPane) {
                        PaneSide.LEFT -> leftPane = leftPane.copy(
                            currentPath = path,
                            files = files,
                            selectedFiles = emptySet(),
                            isLoading = false
                        )
                        PaneSide.RIGHT -> rightPane = rightPane.copy(
                            currentPath = path,
                            files = files,
                            selectedFiles = emptySet(),
                            isLoading = false
                        )
                    }
                } else {
                    when (targetPane) {
                        PaneSide.LEFT -> leftPane = leftPane.copy(
                            isLoading = false,
                            error = "Cannot access $path"
                        )
                        PaneSide.RIGHT -> rightPane = rightPane.copy(
                            isLoading = false,
                            error = "Cannot access $path"
                        )
                    }
                }
            } catch (e: Exception) {
                when (targetPane) {
                    PaneSide.LEFT -> leftPane = leftPane.copy(
                        isLoading = false,
                        error = e.message
                    )
                    PaneSide.RIGHT -> rightPane = rightPane.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun navigateUp() {
        val current = currentPane().currentPath
        val parent = current.substringBeforeLast("/")
        if (parent.isNotEmpty() && parent != current) {
            loadDirectory(parent)
        }
    }

    fun navigateTo(path: String) {
        loadDirectory(path)
    }

    fun syncPanes() {
        val currentPath = currentPane().currentPath
        when (activePane) {
            PaneSide.LEFT -> loadDirectory(currentPath, PaneSide.RIGHT)
            PaneSide.RIGHT -> loadDirectory(currentPath, PaneSide.LEFT)
        }
    }

    fun toggleSelection(path: String) {
        updateCurrentPane { pane ->
            val newSelection = if (path in pane.selectedFiles) {
                pane.selectedFiles - path
            } else {
                pane.selectedFiles + path
            }
            pane.copy(selectedFiles = newSelection)
        }
    }

    fun selectAll() {
        updateCurrentPane { pane ->
            pane.copy(selectedFiles = pane.files.map { it.path }.toSet())
        }
    }

    fun clearSelection() {
        updateCurrentPane { pane ->
            pane.copy(selectedFiles = emptySet())
        }
    }

    fun copySelectedToOtherPane() {
        val source = currentPane()
        val dest = otherPane().currentPath
        val selected = source.files.filter { it.path in source.selectedFiles }

        viewModelScope.launch(Dispatchers.IO) {
            var successCount = 0
            for (file in selected) {
                val destPath = "$dest/${file.name}"
                val useRoot = isRootEnabled
                if (RustBridge.copyFile(file.path, destPath, useRoot)) {
                    successCount++
                }
            }
            statusMessage = "Copied $successCount/${selected.size} files"
            loadDirectory(dest, if (activePane == PaneSide.LEFT) PaneSide.RIGHT else PaneSide.LEFT)
            clearSelection()
        }
    }

    fun moveSelectedToOtherPane() {
        val source = currentPane()
        val dest = otherPane().currentPath
        val selected = source.files.filter { it.path in source.selectedFiles }

        viewModelScope.launch(Dispatchers.IO) {
            var successCount = 0
            for (file in selected) {
                val destPath = "$dest/${file.name}"
                val useRoot = isRootEnabled
                if (RustBridge.moveFile(file.path, destPath, useRoot)) {
                    successCount++
                }
            }
            statusMessage = "Moved $successCount/${selected.size} files"
            loadDirectory(source.currentPath)
            loadDirectory(dest, if (activePane == PaneSide.LEFT) PaneSide.RIGHT else PaneSide.LEFT)
            clearSelection()
        }
    }

    fun deleteSelected() {
        val source = currentPane()
        val selected = source.files.filter { it.path in source.selectedFiles }
        showDeleteConfirm = selected
    }

    fun confirmDelete() {
        val toDelete = showDeleteConfirm
        showDeleteConfirm = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            var successCount = 0
            for (file in toDelete) {
                if (RustBridge.deleteFile(file.path, isRootEnabled)) {
                    successCount++
                }
            }
            statusMessage = "Deleted $successCount/${toDelete.size} files"
            loadDirectory(currentPane().currentPath)
            clearSelection()
        }
    }

    fun renameFile(file: FileItem, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val parent = file.path.substringBeforeLast("/")
            val newPath = "$parent/$newName"
            if (RustBridge.renameFile(file.path, newPath, isRootEnabled)) {
                statusMessage = "Renamed to $newName"
                loadDirectory(currentPane().currentPath)
            } else {
                statusMessage = "Rename failed"
            }
        }
    }

    fun createFolder(name: String) {
        showNewFolderDialog = false
        val path = "${currentPane().currentPath}/$name"
        viewModelScope.launch(Dispatchers.IO) {
            if (RustBridge.createDirectory(path, isRootEnabled)) {
                statusMessage = "Folder created: $name"
                loadDirectory(currentPane().currentPath)
            } else {
                statusMessage = "Failed to create folder"
            }
        }
    }

    fun createFile(name: String) {
        showNewFileDialog = false
        val path = "${currentPane().currentPath}/$name"
        viewModelScope.launch(Dispatchers.IO) {
            if (RustBridge.createFile(path, isRootEnabled)) {
                statusMessage = "File created: $name"
                loadDirectory(currentPane().currentPath)
            } else {
                statusMessage = "Failed to create file"
            }
        }
    }

    fun addBookmark() {
        val path = currentPane().currentPath
        if (path !in bookmarks) {
            bookmarks = bookmarks + path
        }
    }

    fun removeBookmark(path: String) {
        bookmarks = bookmarks - path
    }

    fun navigateToBookmark(path: String) {
        loadDirectory(path)
    }

    fun goToDataAdb() {
        loadDirectory("/data/adb")
    }

    fun showPathInput() {
        showPathInputDialog = true
    }

    fun navigateToPath(path: String) {
        showPathInputDialog = false
        loadDirectory(path)
    }

    fun search(query: String) {
        searchQuery = query
        updateCurrentPane { pane ->
            if (query.isBlank()) {
                pane
            } else {
                pane.copy(
                    files = pane.files.filter {
                        it.name.contains(query, ignoreCase = true)
                    }
                )
            }
        }
    }

    fun clearStatus() {
        statusMessage = null
    }
}
