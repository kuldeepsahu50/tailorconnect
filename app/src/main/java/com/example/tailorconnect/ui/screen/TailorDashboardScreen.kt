package com.example.tailorconnect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tailorconnect.data.model.Measurement
import com.example.tailorconnect.data.repository.AppRepository
import com.example.tailorconnect.ui.components.ProfileSection
import com.example.tailorconnect.viewmodel.ProfileViewModel
import com.example.tailorconnect.viewmodel.TailorViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TailorDashboardScreen(repository: AppRepository, tailorId: String, navController: NavController) {
    Log.d("TailorDashboard", "Loading dashboard for tailorId: $tailorId")
    
    if (tailorId.isBlank()) {
        Log.e("TailorDashboard", "Invalid tailorId provided")
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Error: Invalid tailor ID",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val tailorViewModel = remember { TailorViewModel(repository) }
    val profileViewModel = remember { ProfileViewModel(repository) }
    var selectedTab by remember { mutableStateOf(0) }

    // Collect StateFlow values
    val measurements by tailorViewModel.measurements.collectAsState()
    val isLoading by tailorViewModel.isLoading.collectAsState()
    val error by tailorViewModel.error.collectAsState()

    // Debug logging for state changes
    LaunchedEffect(measurements) {
        Log.d("TailorDashboard", "Measurements updated: size=${measurements.size}")
        measurements.forEach { measurement ->
            Log.d("TailorDashboard", "Admin measurement: id=${measurement.id}, " +
                "customer=${measurement.customerName}, " +
                "adminId=${measurement.adminId}")
        }
    }

    LaunchedEffect(isLoading) {
        Log.d("TailorDashboard", "Loading state changed: $isLoading")
    }

    LaunchedEffect(error) {
        Log.d("TailorDashboard", "Error state changed: $error")
    }

    // Load measurements when the screen is first displayed
    LaunchedEffect(Unit) {
        Log.d("TailorDashboard", "Loading admin measurements")
        tailorViewModel.loadMeasurements(tailorId)
    }

    Column {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Admin Measurements") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Profile") })
        }
        when (selectedTab) {
            0 -> {
                Column {
                    // Add refresh button at the top
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Admin Submitted Measurements",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = { 
                            Log.d("TailorDashboard", "Refresh button clicked")
                            tailorViewModel.refreshMeasurements(tailorId)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    } else if (error != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else if (measurements.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No admin measurements available",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Check back later for new measurements",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                            items(measurements) { measurement ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Customer: ${measurement.customerName}",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Admin Submitted",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Date: ${formatDate(measurement.timestamp)}")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Chest: ${measurement.dimensions["chest"] ?: "N/A"}")
                                        Text("Waist: ${measurement.dimensions["waist"] ?: "N/A"}")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                // Basic PDF generation (placeholder)
                                                val file = File.createTempFile("measurement_${measurement.id}", ".txt")
                                                FileWriter(file).use { writer ->
                                                    writer.write("Customer: ${measurement.customerName}\n")
                                                    writer.write("Date: ${formatDate(measurement.timestamp)}\n")
                                                    writer.write("Chest: ${measurement.dimensions["chest"] ?: "N/A"}\n")
                                                    writer.write("Waist: ${measurement.dimensions["waist"] ?: "N/A"}\n")
                                                    writer.write("Submitted by: Admin\n")
                                                }
                                            }
                                        ) {
                                            Text("Download PDF")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                ProfileSection(profileViewModel, tailorId, navController)
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}