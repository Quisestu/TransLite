package com.example.test0

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.test0.navigation.NavGraph
import com.example.test0.ui.theme.Test0Theme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.test0.AppThemeMode
import com.example.test0.AppThemeDataStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            val themeModeFlow = remember { AppThemeDataStore.getThemeMode(context) }
            val themeMode by themeModeFlow.collectAsState(initial = AppThemeMode.SYSTEM)
            val coroutineScope = rememberCoroutineScope()
            val isDark = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> systemDarkTheme
            }
            Test0Theme(darkTheme = isDark, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        themeMode = themeMode,
                        onThemeSwitch = { nextMode: AppThemeMode ->
                            coroutineScope.launch {
                                AppThemeDataStore.setThemeMode(context, nextMode)
                            }
                        }
                    )
                }
            }
        }
    }
}