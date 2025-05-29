package com.example.tailorconnect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tailorconnect.data.model.Measurement

/**
 * This class contains preview functions for the AdminDashboardScreen
 */

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AdminDashboardPreview() {
    // Sample data for preview
    val sampleMeasurements = listOf(
        Measurement(
            id = "m1",
            customerName = "John Doe",
            dimensions = mapOf("chest" to "42"),
            tailorId = "t1",
            adminId = "admin123",
            timestamp = System.currentTimeMillis()
        ),
        Measurement(
            id = "m2",
            customerName = "Jane Smith",
            dimensions = mapOf("chest" to "38"),
            tailorId = "t2",
            adminId = "admin123",
            timestamp = System.currentTimeMillis()
        ),
        Measurement(
            id = "m3",
            customerName = "Robert Johnson",
            dimensions = mapOf("chest" to "44"),
            tailorId = "t1",
            adminId = "admin123",
            timestamp = System.currentTimeMillis()
        )
    )
    
    AdminDashboardPreviewContent(measurements = sampleMeasurements)
}

/**
 * A simplified version of AdminDashboardScreen for preview purposes
 */
@Composable
fun AdminDashboardPreviewContent(measurements: List<Measurement>) {
    var selectedTab by remember { mutableStateOf(0) }
    var customerName by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var tailorId by remember { mutableStateOf("") }
    
    Column {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Measurements") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Submit") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Profile") })
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(measurements) { measurement ->
                        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Customer: ${measurement.customerName}")
                                Text("Chest: ${measurement.dimensions["chest"] ?: "N/A"}")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(onClick = {}) {
                                        Text("Edit")
                                    }
                                    Button(onClick = {}) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = customerName, 
                        onValueChange = { customerName = it }, 
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = chest, 
                        onValueChange = { chest = it }, 
                        label = { Text("Chest") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = tailorId, 
                        onValueChange = { tailorId = it }, 
                        label = { Text("Tailor ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit")
                    }
                }
            }
            2 -> {
                // Simple profile preview
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Admin Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    Text("Name: Admin User")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Email: admin@example.com")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Phone: 555-123-4567")
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(onClick = {}) {
                        Text("Update Profile")
                    }
                }
            }
        }
    }
} 