package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SkinDatabase
import com.example.data.SkinProjectRepository
import com.example.ui.SkinCraftViewModel
import com.example.ui.SkinCraftViewModelFactory
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = SkinDatabase.getDatabase(applicationContext)
        val repository = SkinProjectRepository(database.skinProjectDao())

        setContent {
            MyApplicationTheme {
                val viewModel: SkinCraftViewModel = viewModel(
                    factory = SkinCraftViewModelFactory(repository)
                )

                var currentScreen by remember { mutableStateOf("LIBRARY") } // "LIBRARY", "EDITOR", "SETTINGS"

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        "LIBRARY" -> {
                            LibraryScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { currentScreen = "EDITOR" },
                                onNavigateToSettings = { currentScreen = "SETTINGS" },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        "EDITOR" -> {
                            StudioScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = "LIBRARY" },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        "SETTINGS" -> {
                            SettingsScreen(
                                onNavigateBack = { currentScreen = "LIBRARY" },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
