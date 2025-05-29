package com.example.tailorconnect.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tailorconnect.data.model.Measurement
import com.example.tailorconnect.data.model.User
import com.example.tailorconnect.ui.components.ProfileSection
import com.example.tailorconnect.viewmodel.AdminViewModel
import com.example.tailorconnect.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.tailorconnect.data.repository.AppRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(repository: AppRepository, userId: String, navController: NavController) {
    Log.d("TailorConnect", "AdminDashboardScreen loaded with userId: $userId")
    val adminViewModel = AdminViewModel(repository)
    val profileViewModel = ProfileViewModel(repository)
    var measurements by remember { mutableStateOf(listOf<Measurement>()) }
    var customerName by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var tailorId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var tailors by remember { mutableStateOf(listOf<User>()) }
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    fun loadData() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null
                successMessage = null
                measurements = adminViewModel.getAllMeasurements()
                // Load tailors for the dropdown
                tailors = adminViewModel.getAllTailors()
            } catch (e: Exception) {
                errorMessage = "Failed to load data: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Column {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Measurements") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Submit") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Profile") })
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
                            text = "All Measurements",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = { loadData() }) {
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
                    } else if (errorMessage != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = errorMessage ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else if (measurements.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "No measurements available",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                            items(measurements) { measurement ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Customer: ${measurement.customerName}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Date: ${formatDate(measurement.timestamp)}")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Chest: ${measurement.dimensions["chest"] ?: "N/A"}")
                                        Text("Waist: ${measurement.dimensions["waist"] ?: "N/A"}")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        try {
                                                            adminViewModel.deleteMeasurement(measurement.id)
                                                            loadData()
                                                            successMessage = "Measurement deleted successfully"
                                                        } catch (e: Exception) {
                                                            errorMessage = "Failed to delete measurement: ${e.message}"
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text("Delete")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    customerName = measurement.customerName
                                                    chest = measurement.dimensions["chest"] ?: ""
                                                    waist = measurement.dimensions["waist"] ?: ""
                                                    tailorId = measurement.tailorId
                                                    selectedTab = 1
                                                }
                                            ) {
                                                Text("Edit")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    if (successMessage != null) {
                        Text(
                            text = successMessage ?: "",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = chest,
                        onValueChange = { chest = it },
                        label = { Text("Chest") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = waist,
                        onValueChange = { waist = it },
                        label = { Text("Waist") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Tailor selection dropdown
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = tailors.find { it.id == tailorId }?.name ?: "Select Tailor",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            tailors.forEach { tailor ->
                                DropdownMenuItem(
                                    text = { Text(tailor.name) },
                                    onClick = {
                                        tailorId = tailor.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (customerName.isBlank() || chest.isBlank() || waist.isBlank() || tailorId.isBlank()) {
                                errorMessage = "Please fill in all fields"
                                return@Button
                            }
                            scope.launch {
                                try {
                                    val measurement = Measurement(
                                        customerName = customerName,
                                        dimensions = mapOf("chest" to chest, "waist" to waist),
                                        tailorId = tailorId,
                                        adminId = userId,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    adminViewModel.addMeasurement(measurement)
                                    loadData()
                                    successMessage = "Measurement submitted successfully"
                                    // Clear form
                                    customerName = ""
                                    chest = ""
                                    waist = ""
                                    tailorId = ""
                                    selectedTab = 0
                                } catch (e: Exception) {
                                    errorMessage = "Failed to submit measurement: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit")
                    }
                }
            }
            2 -> {
                ProfileSection(profileViewModel, userId, navController)
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Preview
@Composable
fun AdminDashboardScreenPreview() {
    AdminDashboardScreen(repository = AppRepository(), userId = "admin_id", navController = NavController(LocalContext.current))
}