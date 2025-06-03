package com.example.tailorconnect.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class ThemeState {
    var isDarkTheme by mutableStateOf(false)
        private set

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
    }

    // Custom colors for both themes
    val primaryColor: Color
        get() = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF1976D2)
    
    val backgroundColor: Color
        get() = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F5)
    
    val surfaceColor: Color
        get() = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    
    val textColor: Color
        get() = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF000000)
    
    val secondaryTextColor: Color
        get() = if (isDarkTheme) Color(0xFFB0B0B0) else Color(0xFF666666)
} 