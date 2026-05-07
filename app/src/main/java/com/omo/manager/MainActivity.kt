package com.omo.manager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omo.manager.ui.screen.FileBrowserScreen
import com.omo.manager.ui.screen.ScriptRunnerScreen
import com.omo.manager.ui.screen.TextEditorScreen
import com.omo.manager.ui.theme.OMOManagerTheme
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            OMOManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OMOManagerApp()
                }
            }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(
                        android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(
                        android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    )
                    startActivity(intent)
                }
            }
        } else {
            val permissions = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (permissions.isNotEmpty()) {
                permissionRequest.launch(permissions.toTypedArray())
            }
        }
    }
}

@Composable
fun OMOManagerApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "browser"
    ) {
        composable("browser") {
            FileBrowserScreen(
                onOpenScript = { scriptPath ->
                    navController.navigate("script/$scriptPath")
                },
                onOpenTextEditor = { filePath ->
                    navController.navigate("editor/$filePath")
                }
            )
        }

        composable(
            route = "script/{path}",
            arguments = listOf(
                navArgument("path") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path") ?: ""
            val decodedPath = try { URLDecoder.decode(path, "UTF-8") } catch (_: Exception) { path }
            ScriptRunnerScreen(scriptPath = decodedPath)
        }

        composable(
            route = "editor/{path}",
            arguments = listOf(
                navArgument("path") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path") ?: ""
            val decodedPath = try { URLDecoder.decode(path, "UTF-8") } catch (_: Exception) { path }
            TextEditorScreen(
                filePath = decodedPath,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
