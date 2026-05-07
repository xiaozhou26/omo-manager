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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omo.manager.ui.component.ScriptOutputView
import com.omo.manager.viewmodel.ScriptRunnerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptRunnerScreen(
    viewModel: ScriptRunnerViewModel = viewModel(),
    scriptPath: String = ""
) {
    val scriptContent by remember { derivedStateOf { viewModel.scriptContent } }
    val output by remember { derivedStateOf { viewModel.output } }
    val isRunning by remember { derivedStateOf { viewModel.isRunning } }
    val useRoot by remember { derivedStateOf { viewModel.useRoot } }
    val isRootAvailable by remember { derivedStateOf { viewModel.isRootAvailable } }

    LaunchedEffect(Unit) {
        viewModel.checkRoot()
        if (scriptPath.isNotEmpty()) {
            viewModel.setScriptPath(scriptPath)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (scriptPath.isNotEmpty()) "Script: ${scriptPath.substringAfterLast("/")}"
                        else "Script Runner",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    if (isRootAvailable) {
                        FilterChip(
                            selected = useRoot,
                            onClick = { viewModel.toggleRoot() },
                            label = { Text("Root", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.height(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = scriptContent,
                onValueChange = { viewModel.updateScript(it) },
                label = { Text("Shell Script") },
                placeholder = { Text("#!/bin/sh\necho \"Hello World\"") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                maxLines = Int.MAX_VALUE
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRunning) {
                    Button(
                        onClick = { viewModel.stopScript() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = { viewModel.runScript() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run${if (useRoot) " (Root)" else ""}")
                    }
                }

                OutlinedButton(onClick = { viewModel.clearOutput() }) {
                    Icon(Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }

                OutlinedButton(onClick = { viewModel.clearScript() }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ScriptOutputView(
                output = output,
                isRunning = isRunning,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
