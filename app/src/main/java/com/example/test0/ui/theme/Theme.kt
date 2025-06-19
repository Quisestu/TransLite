@file:Suppress("DEPRECATION")

package com.example.test0.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme

@Composable
fun Test0Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = Color(0xFF1A237E),           // 深蓝主色（Indigo 900）
            onPrimary = Color(0xFFE8EAF6),         // 浅紫蓝文字
            primaryContainer = Color(0xFF3F51B5),  // 中蓝容器（Indigo 500）
            onPrimaryContainer = Color(0xFFE8EAF6),
            secondary = Color(0xFF5C6BC0),         // 中紫蓝副色（Indigo 400）
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFF303F9F),// 深紫蓝容器（Indigo 700）
            onSecondaryContainer = Color(0xFFE8EAF6),
            tertiary = Color(0xFF4DD0E1),          // 青蓝强调色（Cyan 300）
            onTertiary = Color(0xFF00363A),
            tertiaryContainer = Color(0xFF006064),
            onTertiaryContainer = Color(0xFFE0F7FA),
            background = Color(0xFF0D1B2A),        // 深蓝背景
            onBackground = Color(0xFFE8EAF6),
            surface = Color(0xFF283593),           // 中深蓝表面（比primary稍亮）
            onSurface = Color(0xFFE8EAF6),
            error = Color(0xFFFF5252),            // 红色错误提示
            onError = Color(0xFF000000),
        )
        else -> lightColorScheme(
            primary = Color(0xFF42A5F5),           // 亮蓝色主色（Blue 400）
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE3F2FD),  // 更浅的蓝色
            onPrimaryContainer = Color(0xFF001E2F),
            secondary = Color(0xFF64B5F6),         // 亮蓝副色（Blue 300/400）
            onSecondary = Color(0xFF001E2F),
            secondaryContainer = Color(0xFFBBDEFB),
            onSecondaryContainer = Color(0xFF001E2F),
            tertiary = Color(0xFF81D4FA),          // 青蓝强调色（Cyan 200）
            onTertiary = Color(0xFF00363A),
            tertiaryContainer = Color(0xFFE0F7FA),
            onTertiaryContainer = Color(0xFF00363A),
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF1E293B),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1E293B),
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
