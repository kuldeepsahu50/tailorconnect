package com.example.tailorconnect.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A simple preview to test if Preview annotation works
 */
@Preview(showBackground = true)
@Composable
fun SimplePreview() {
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color.LightGray)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Preview Test",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black
        )
    }
} 