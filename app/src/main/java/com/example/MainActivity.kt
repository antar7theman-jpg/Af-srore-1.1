package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.AntarRepository
import com.example.data.database.AntarDatabase
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.AntarStoreTheme
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.ui.viewmodels.AntarSalesViewModelFactory
import com.example.utils.AppPreferencesManager

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "تم منح جميع الأذونات بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    private val viewModel: AntarSalesViewModel by viewModels {
        val database = AntarDatabase.getInstance(applicationContext)
        val repository = AntarRepository(database.storeDao())
        val preferencesManager = AppPreferencesManager(applicationContext)
        AntarSalesViewModelFactory(repository, preferencesManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestAppPermissions()

        setContent {
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val currentThemePalette by viewModel.currentThemePalette.collectAsState()
            val currentThemeMode by viewModel.currentThemeMode.collectAsState()

            val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                AntarStoreTheme(
                    palette = currentThemePalette,
                    themeMode = currentThemeMode
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (!isLoggedIn) {
                            LoginScreen(viewModel = viewModel)
                        } else {
                            MainAppScreen(
                                viewModel = viewModel,
                                isDarkTheme = currentThemeMode == com.example.ui.theme.AppThemeMode.DARK,
                                onToggleDarkTheme = { viewModel.toggleDarkMode(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestAppPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
