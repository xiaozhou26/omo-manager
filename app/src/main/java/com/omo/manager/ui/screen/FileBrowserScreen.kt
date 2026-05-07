package com.omo.manager.ui.screen

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omo.manager.model.FileItem
import com.omo.manager.ui.component.FileItemRow
import com.omo.manager.ui.component.PathBar
import com.omo.manager.viewmodel.FileBrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel = viewModel(),
    onOpenScript: (String) -> Unit = {},
    onOpenTextEditor: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val leftPane by remember { derivedStateOf { viewModel.leftPane } }
    val rightPane by remember { derivedStateOf { viewModel.rightPane } }
    val activePane by remember { derivedStateOf { viewModel.activePane } }
    val isRootEnabled by remember { derivedStateOf { viewModel.isRootEnabled } }
    val statusMessage by remember { derivedStateOf { viewModel.statusMessage } }

    LaunchedEffect(Unit) {
        viewModel.checkRoot()
        viewModel.loadDirectory(leftPane.currentPath, FileBrowserViewModel.PaneSide.LEFT)
        viewModel.loadDirectory(rightPane.currentPath, FileBrowserViewModel.PaneSide.RIGHT)
    }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearStatus()
        }
    }

    var newFileName by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }
    var renameValue by remember { mutableStateOf("") }
    var pathInput by remember { mutableStateOf("") }
    var searchInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "OMO Manager",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    if (isRootEnabled) {
                        IconButton(onClick = { viewModel.goToDataAdb() }) {
                            Icon(Icons.Filled.Security, "Root /data/adb/")
                        }
                    }
                    IconButton(onClick = { viewModel.showSearchDialog = true }) {
                        Icon(Icons.Filled.Search, "Search")
                    }
                    IconButton(onClick = { viewModel.showBookmarkDialog = true }) {
                        Icon(Icons.Filled.Bookmark, "Bookmarks")
                    }
                    IconButton(onClick = { viewModel.showPathInput() }) {
                        Icon(Icons.Filled.GpsFixed, "Go to path")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = { viewModel.selectAll() }) {
                    Icon(Icons.Filled.SelectAll, "Select All")
                }
                IconButton(onClick = { viewModel.copySelectedToOtherPane() }) {
                    Icon(Icons.Filled.ContentCopy, "Copy")
                }
                IconButton(onClick = { viewModel.moveSelectedToOtherPane() }) {
                    Icon(Icons.Filled.ContentCut, "Move")
                }
                IconButton(onClick = { viewModel.deleteSelected() }) {
                    Icon(Icons.Filled.Delete, "Delete")
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.showNewFileDialog = true }) {
                    Icon(Icons.Filled.NoteAdd, "New File")
                }
                IconButton(onClick = { viewModel.showNewFolderDialog = true }) {
                    Icon(Icons.Filled.CreateNewFolder, "New Folder")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                PaneView(
                    pane = leftPane,
                    isActive = activePane == FileBrowserViewModel.PaneSide.LEFT,
                    onFileClick = { file ->
                        viewModel.setActivePane(FileBrowserViewModel.PaneSide.LEFT)
                        if (file.isDirectory) {
                            viewModel.loadDirectory(file.path, FileBrowserViewModel.PaneSide.LEFT)
                        }
                    },
                    onFileLongClick = { file ->
                        viewModel.setActivePane(FileBrowserViewModel.PaneSide.LEFT)
                        viewModel.toggleSelection(file.path)
                    },
                    onNavigateUp = {
                        viewModel.setActivePane(FileBrowserViewModel.PaneSide.LEFT)
                        val current = leftPane.currentPath
                        val parent = current.substringBeforeLast("/")
                        if (parent.isNotEmpty() && parent != current) {
                            viewModel.loadDirectory(parent, FileBrowserViewModel.PaneSide.LEFT)
                        }
                    },
                    onPathClick = {
                        viewModel.setActivePane(FileBrowserViewModel.PaneSide.LEFT)
                        pathInput = leftPane.currentPath
                        viewModel.showPathInputDialog = true
                    },
                    onSyncClick = {
                        viewModel.setActivePane(FileBrowserViewModel.PaneSide.LEFT)
                        viewModel.syncPanes()
                    },
                    onBookmarkClick = {
                        viewModel.setActivePane(FileBrowserViewModel.PaneSide.LEFT)
                        viewModel.addBookmark()
                    },
                    onRunScript = { file -> onOpenScript(file.path) },
                    onEdit = { file -> onOpenTextEditor(file.path) },
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider()

                PaneView(
                    pane = rightPane,
                    isActive = activePane == FileBrowserViewModel.PaneSide.RIGHT,
                    onFileClick = { file ->
                        viewModel.setActivePane(FileBrowserViewModel.PaneSide.RIGHT)
                        if (file.isDirectory) {
                            viewModel.loadDirectory(file.path, FileBrowserViewModel.PaneSide.RIGHT)
                        }
                    },
                    onFileLongClick = { file ->
                        viewModel.setActivePane(FileBrowserViewModel.PaneSide.RIGHT)
                        viewModel.toggleSelection(file.path)
                    },
                    onNavigateUp = {
                        viewModel.setActivePane(FileBrowserViewModel.PaneSide.RIGHT)
                        val current = rightPane.currentPath
                        val parent = current.substringBeforeLast("/")
                        if (parent.isNotEmpty() && parent != current) {
                            viewModel.loadDirectory(parent, FileBrowserViewModel.PaneSide.RIGHT)
                        }
                    },
                    onPathClick = {
                        viewModel.setActivePane(FileBrowserViewModel.PaneSide.RIGHT)
                        pathInput = rightPane.currentPath
                        viewModel.showPathInputDialog = true
                    },
                    onSyncClick = {
                        viewModel.setActivePane(FileBrowserViewModel.PaneSide.RIGHT)
                        viewModel.syncPanes()
                    },
                    onBookmarkClick = {
                        viewModel.setActivePane(FileBrowserViewModel.PaneSide.RIGHT)
                        viewModel.addBookmark()
                    },
                    onRunScript = { file -> onOpenScript(file.path) },
                    onEdit = { file -> onOpenTextEditor(file.path) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (viewModel.showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showNewFileDialog = false },
            title = { Text("New File") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("File name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFileName.isNotBlank()) {
                        viewModel.createFile(newFileName)
                        newFileName = ""
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showNewFileDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (viewModel.showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showNewFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        viewModel.createFolder(newFolderName)
                        newFolderName = ""
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    viewModel.showRenameDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { viewModel.showRenameDialog = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameValue.isNotBlank()) {
                        viewModel.renameFile(file, renameValue)
                        renameValue = ""
                        viewModel.showRenameDialog = null
                    }
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showRenameDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (viewModel.showDeleteConfirm.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteConfirm = emptyList() },
            title = { Text("Confirm Delete") },
            text = {
                Text("Delete ${viewModel.showDeleteConfirm.size} item(s)?")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteConfirm = emptyList() }) { Text("Cancel") }
            }
        )
    }

    if (viewModel.showPathInputDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showPathInputDialog = false },
            title = { Text("Go to Path") },
            text = {
                OutlinedTextField(
                    value = pathInput,
                    onValueChange = { pathInput = it },
                    label = { Text("Path") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pathInput.isNotBlank()) {
                        viewModel.navigateToPath(pathInput)
                    }
                }) { Text("Go") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showPathInputDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (viewModel.showBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showBookmarkDialog = false },
            title = { Text("Bookmarks") },
            text = {
                if (viewModel.bookmarks.isEmpty()) {
                    Text("No bookmarks yet")
                } else {
                    LazyColumn {
                        items(viewModel.bookmarks) { bookmark ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        bookmark,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.clickable {
                                            viewModel.navigateToBookmark(bookmark)
                                            viewModel.showBookmarkDialog = false
                                        }
                                    )
                                },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.removeBookmark(bookmark) }) {
                                        Icon(Icons.Filled.Close, "Remove", modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.showBookmarkDialog = false }) { Text("Close") }
            }
        )
    }

    if (viewModel.showSearchDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.showSearchDialog = false
                searchInput = ""
                viewModel.search("")
            },
            title = { Text("Search") },
            text = {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        viewModel.search(it)
                    },
                    label = { Text("Filter files") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.showSearchDialog = false }) { Text("Close") }
            }
        )
    }

    viewModel.showPropertiesDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { viewModel.showPropertiesDialog = null },
            title = { Text("Properties") },
            text = {
                Column {
                    PropertyRow("Name", file.name)
                    PropertyRow("Path", file.path)
                    PropertyRow("Type", if (file.isDirectory) "Directory" else "File")
                    if (!file.isDirectory) PropertyRow("Size", file.displaySize)
                    PropertyRow("Permissions", file.permissions)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.showPropertiesDialog = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PaneView(
    pane: FileBrowserViewModel.PaneState,
    isActive: Boolean,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit,
    onNavigateUp: () -> Unit,
    onPathClick: () -> Unit,
    onSyncClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onRunScript: (FileItem) -> Unit,
    onEdit: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight()) {
        PathBar(
            path = pane.currentPath,
            isActive = isActive,
            onNavigateUp = onNavigateUp,
            onPathClick = onPathClick,
            onSyncClick = onSyncClick,
            onBookmarkClick = onBookmarkClick
        )

        if (pane.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else if (pane.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        pane.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else if (pane.files.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Empty folder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(pane.files, key = { it.path }) { file ->
                    FileItemRow(
                        file = file,
                        isSelected = file.path in pane.selectedFiles,
                        is_active = isActive,
                        onSelect = { onFileLongClick(file) },
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) },
                        onRunScript = if (file.isScriptFile) {{ onRunScript(file) }} else null,
                        onEdit = if (file.isTextFile) {{ onEdit(file) }} else null
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
    ) {
        Divider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
