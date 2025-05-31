package com.example.tailorconnect.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tailorconnect.data.model.Measurement
import com.example.tailorconnect.data.model.MeasurementFields
import com.example.tailorconnect.data.model.repository.AppRepository
import com.example.tailorconnect.ui.components.ProfileSection
import com.example.tailorconnect.viewmodel.ProfileViewModel
import com.example.tailorconnect.viewmodel.TailorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(repository: AppRepository, adminId: String, navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedGender by remember { mutableStateOf("Male") }
    var selectedGarmentType by remember { mutableStateOf("Shirt") }
    var customerName by remember { mutableStateOf("") }
    var measurements by remember { mutableStateOf(mutableMapOf<String, String>()) }
    var showDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var expandedGarmentType by remember { mutableStateOf(false) }
    var allMeasurements by remember { mutableStateOf<List<Measurement>>(emptyList()) }

    val tailorViewModel = remember { TailorViewModel(repository) }
    val tailors by tailorViewModel.tailors.collectAsState()
    val profileViewModel = remember { ProfileViewModel(repository) }

    // Load measurements
    LaunchedEffect(Unit) {
        try {
            allMeasurements = repository.getAllMeasurements()
        } catch (e: Exception) {
            errorMessage = "Failed to load measurements: ${e.message}"
        }
    }

    // Update garment types when gender changes
    LaunchedEffect(selectedGender) {
        val garmentTypes = MeasurementFields.getGarmentTypes(selectedGender)
        if (garmentTypes.isNotEmpty()) {
            selectedGarmentType = garmentTypes[0]
        }
    }

    // Update measurement fields when garment type changes
    LaunchedEffect(selectedGarmentType) {
        val fields = MeasurementFields.getMeasurementFields(selectedGender, selectedGarmentType)
        measurements = fields.associateWith { "" }.toMutableMap()
    }

    Column {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Submit") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Measurements") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Profile") })
        }

        when (selectedTab) {
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Text(
                        text = "New Measurement",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Customer Name Section
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        shape = MaterialTheme.shapes.medium
                    )

                    // Gender Selection Section
                    Text(
                        text = "Gender",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .selectable(
                                    selected = selectedGender == "Male",
                                    onClick = { selectedGender = "Male" }
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedGender == "Male",
                                onClick = { selectedGender = "Male" }
                            )
                            Text("Male", modifier = Modifier.padding(start = 8.dp))
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .selectable(
                                    selected = selectedGender == "Female",
                                    onClick = { selectedGender = "Female" }
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedGender == "Female",
                                onClick = { selectedGender = "Female" }
                            )
                            Text("Female", modifier = Modifier.padding(start = 8.dp))
                        }
                    }

                    // Garment Type Section
                    Text(
                        text = "Garment Type",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val garmentTypes = MeasurementFields.getGarmentTypes(selectedGender)
                    ExposedDropdownMenuBox(
                        expanded = expandedGarmentType,
                        onExpandedChange = { expandedGarmentType = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedGarmentType,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGarmentType) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(
                            expanded = expandedGarmentType,
                            onDismissRequest = { expandedGarmentType = false }
                        ) {
                            garmentTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = { 
                                        selectedGarmentType = type
                                        expandedGarmentType = false
                                    }
                                )
                            }
                        }
                    }

                    // Measurements Section
                    Text(
                        text = "Measurements",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    val measurementFields = MeasurementFields.getMeasurementFields(selectedGender, selectedGarmentType)
                    measurementFields.forEach { field ->
                        OutlinedTextField(
                            value = measurements[field] ?: "",
                            onValueChange = { newValue ->
                                measurements = measurements.toMutableMap().apply {
                                    put(field, newValue)
                                }
                            },
                            label = { Text(field) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = MaterialTheme.shapes.medium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (customerName.isBlank()) {
                                errorMessage = "Please enter customer name"
                                return@Button
                            }
                            if (measurements.values.any { it.isBlank() }) {
                                errorMessage = "Please fill all measurements"
                                return@Button
                            }

                            isLoading = true
                            val measurement = Measurement(
                                id = "",
                                customerName = customerName,
                                tailorId = "", // Empty tailor ID since we removed the selection
                                adminId = adminId,
                                timestamp = System.currentTimeMillis(),
                                dimensions = measurements
                            )

                            // Submit measurement
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    repository.addMeasurement(measurement)
                                    showDialog = true
                                    // Reset form
                                    customerName = ""
                                    measurements = MeasurementFields.getMeasurementFields(selectedGender, selectedGarmentType)
                                        .associateWith { "" }
                                        .toMutableMap()
                                    // Refresh measurements list
                                    allMeasurements = repository.getAllMeasurements()
                                } catch (e: Exception) {
                                    errorMessage = "Failed to submit measurement: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                "Submit Measurement",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
            1 -> {
                // Measurements List
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Measurements",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = { 
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    allMeasurements = repository.getAllMeasurements()
                                } catch (e: Exception) {
                                    errorMessage = "Failed to refresh measurements: ${e.message}"
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }

                    if (allMeasurements.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "No measurements available",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        var expandedMeasurementId by remember { mutableStateOf<String?>(null) }
                        var showDeleteDialog by remember { mutableStateOf<Measurement?>(null) }
                        var showEditDialog by remember { mutableStateOf<Measurement?>(null) }
                        
                        LazyColumn {
                            items(allMeasurements) { measurement ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { 
                                                        expandedMeasurementId = if (expandedMeasurementId == measurement.id) null else measurement.id
                                                    }
                                            ) {
                                                Text(
                                                    text = measurement.customerName,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    text = formatDate(measurement.timestamp),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row {
                                                IconButton(onClick = { showEditDialog = measurement }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit"
                                                    )
                                                }
                                                IconButton(onClick = { showDeleteDialog = measurement }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete"
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { 
                                                        expandedMeasurementId = if (expandedMeasurementId == measurement.id) null else measurement.id
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (expandedMeasurementId == measurement.id) 
                                                            Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = if (expandedMeasurementId == measurement.id) 
                                                            "Collapse" else "Expand"
                                                    )
                                                }
                                            }
                                        }

                                        if (expandedMeasurementId == measurement.id) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Divider()
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            measurement.dimensions.forEach { (key, value) ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = key,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = value,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Delete Confirmation Dialog
                        showDeleteDialog?.let { measurement ->
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = null },
                                title = { Text("Delete Measurement") },
                                text = { Text("Are you sure you want to delete this measurement?") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                try {
                                                    repository.deleteMeasurement(measurement.id)
                                                    allMeasurements = repository.getAllMeasurements()
                                                    showDeleteDialog = null
                                                } catch (e: Exception) {
                                                    errorMessage = "Failed to delete measurement: ${e.message}"
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Delete")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = null }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        // Edit Dialog
                        showEditDialog?.let { measurement ->
                            var editedCustomerName by remember { mutableStateOf(measurement.customerName) }
                            var editedMeasurements by remember { mutableStateOf(measurement.dimensions) }
                            
                            AlertDialog(
                                onDismissRequest = { showEditDialog = null },
                                title = { Text("Edit Measurement") },
                                text = {
                                    Column {
                                        OutlinedTextField(
                                            value = editedCustomerName,
                                            onValueChange = { editedCustomerName = it },
                                            label = { Text("Customer Name") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        editedMeasurements.forEach { (key, value) ->
                                            OutlinedTextField(
                                                value = value,
                                                onValueChange = { newValue ->
                                                    editedMeasurements = editedMeasurements.toMutableMap().apply {
                                                        put(key, newValue)
                                                    }
                                                },
                                                label = { Text(key) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                try {
                                                    // Create updated measurement
                                                    val updatedMeasurement = measurement.copy(
                                                        customerName = editedCustomerName,
                                                        dimensions = editedMeasurements
                                                    )
                                                    
                                                    // Use editMeasurement instead of updateMeasurement
                                                    repository.editMeasurement(updatedMeasurement)
                                                    
                                                    // Refresh the measurements list
                                                    allMeasurements = repository.getAllMeasurements()
                                                    showEditDialog = null
                                                } catch (e: Exception) {
                                                    errorMessage = "Failed to update measurement: ${e.message}"
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Save")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showEditDialog = null }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
            2 -> {
                ProfileSection(profileViewModel, adminId, navController)
            }
        }
    }

    // Error Dialog
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Success Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Success") },
            text = { Text("Measurement submitted successfully") },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
