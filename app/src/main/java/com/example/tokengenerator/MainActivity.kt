package com.example.tokengenerator

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.tokengenerator.ui.screen.MainScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RequestPermissions()
            MainScreen()
        }
    }
}

@Composable
fun RequestPermissions() {
    val permissionsList = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissionsList.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            // Handle permission results here if needed
        }
    )
    LaunchedEffect(true) {
        requestPermissionLauncher.launch(permissionsList.toTypedArray())
    }
}