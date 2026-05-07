package com.omo.manager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.omo.manager.native.RustBridge
import com.omo.manager.util.RootAccess
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    filePath: String,
    useRoot: Boolean = RootAccess.hasRoot,
    onBack: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isModified by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(filePath) {
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                content = RustBridge.readTextFile(filePath, useRoot)
                isLoading = false
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        filePath.substringAfterLast("/"),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isModified) {
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                if (RustBridge.writeTextFile(filePath, content, useRoot)) {
                                    isModified = false
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Save, "Save")
                        }
                    }
                    IconButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            content = RustBridge.readTextFile(filePath, useRoot)
                            isModified = false
                        }
                    }) {
                        Icon(Icons.Filled.Refresh, "Reload")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            }
        } else {
            OutlinedTextField(
                value = content,
                onValueChange = {
                    content = it
                    isModified = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(8.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                maxLines = Int.MAX_VALUE
            )
        }
    }
}
