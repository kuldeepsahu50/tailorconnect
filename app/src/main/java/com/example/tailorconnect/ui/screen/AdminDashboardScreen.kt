package com.example.tailorconnect.ui.screen

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tailorconnect.R
import com.example.tailorconnect.data.model.Measurement
import com.example.tailorconnect.data.model.MeasurementFields
import com.example.tailorconnect.data.model.repository.AppRepository
import com.example.tailorconnect.ui.components.ProfileSection
import com.example.tailorconnect.ui.components.CustomerImageCapture
import com.example.tailorconnect.utils.PdfGenerator
import com.example.tailorconnect.utils.CsvGenerator
import com.example.tailorconnect.viewmodel.ProfileViewModel
import com.example.tailorconnect.viewmodel.TailorViewModel
import com.example.tailorconnect.ui.theme.ThemeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.tooling.preview.Preview
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    repository: AppRepository,
    adminId: String,
    navController: NavController,
    themeState: ThemeState = remember { ThemeState() }
) {
    // Add validation for adminId
    if (adminId.isBlank()) {
        Log.e("AdminDashboard", "Invalid adminId provided")
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Error: Invalid admin ID",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    Log.d("AdminDashboard", "Loading dashboard for adminId: $adminId")
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Calculate responsive dimensions
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp
    
    // Responsive spacing
    val defaultPadding = if (isTablet) 24.dp else 16.dp
    val smallPadding = if (isTablet) 16.dp else 8.dp
    val largePadding = if (isTablet) 32.dp else 24.dp
    
    // Responsive font sizes
    val titleSize = if (isTablet) 28.sp else 24.sp
    val subtitleSize = if (isTablet) 20.sp else 16.sp
    val bodySize = if (isTablet) 16.sp else 14.sp
    val captionSize = if (isTablet) 14.sp else 12.sp
    
    // Responsive component sizes
    val buttonHeight = if (isTablet) 64.dp else 56.dp
    val iconSize = if (isTablet) 32.dp else 24.dp
    
    var selectedTab by remember { mutableStateOf(0) }
    var selectedGender by remember { mutableStateOf("Male") }
    var selectedGarmentType by remember { mutableStateOf("Shirt") }
    var customerName by remember { mutableStateOf("") }
    var measurements by remember { mutableStateOf(mutableMapOf<String, String>()) }
    var customerImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var expandedGarmentType by remember { mutableStateOf(false) }
    var allMeasurements by remember { mutableStateOf<List<Measurement>>(emptyList()) }
    var selectedMeasurementForPdf by remember { mutableStateOf<Measurement?>(null) }

    val tailorViewModel = remember { TailorViewModel(repository) }
    val tailors by tailorViewModel.tailors.collectAsState()
    val profileViewModel = remember { ProfileViewModel(repository) }

    val context = LocalContext.current
    val pdfGenerator = remember { PdfGenerator(context) }
    val csvGenerator = remember { CsvGenerator(context) }
    val storage = Firebase.storage

    // PDF save launcher
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { pdfUri ->
            try {
                selectedMeasurementForPdf?.let { measurement ->
                    // Create PDF directly to the output stream
                    context.contentResolver.openOutputStream(pdfUri)?.use { outputStream ->
                        PdfWriter(outputStream).use { writer ->
                            val pdfDoc = PdfDocument(writer)
                            Document(pdfDoc).use { document ->
                                // Add title with responsive font size
                                val title = Paragraph("Measurement Details")
                                    .setTextAlignment(TextAlignment.CENTER)
                                    .setFontSize(if (isTablet) 24f else 20f)
                                    .setBold()
                                document.add(title)
                                
                                // Add customer info with responsive font sizes
                                document.add(Paragraph("\n"))
                                document.add(Paragraph("Customer Name: ${measurement.customerName}")
                                    .setFontSize(if (isTablet) 16f else 14f))
                                document.add(Paragraph("Date: ${formatDate(measurement.timestamp)}")
                                    .setFontSize(if (isTablet) 14f else 12f))
                                document.add(Paragraph("\n"))
                                
                                // Add measurements table with responsive margins
                                val table = Table(UnitValue.createPercentArray(2))
                                    .useAllAvailableWidth()
                                    .setMarginTop(if (isTablet) 30f else 20f)
                                    .setMarginBottom(if (isTablet) 30f else 20f)
                                
                                // Add header with responsive font sizes
                                val headerCell1 = Cell().add(Paragraph("Measurement")
                                    .setBold()
                                    .setFontSize(if (isTablet) 14f else 12f))
                                val headerCell2 = Cell().add(Paragraph("Value")
                                    .setBold()
                                    .setFontSize(if (isTablet) 14f else 12f))
                                table.addHeaderCell(headerCell1)
                                table.addHeaderCell(headerCell2)
                                
                                // Add measurement data with responsive font sizes
                                measurement.dimensions.forEach { (key, value) ->
                                    table.addCell(Cell().add(Paragraph(key)
                                        .setFontSize(if (isTablet) 12f else 10f)))
                                    table.addCell(Cell().add(Paragraph(value)
                                        .setFontSize(if (isTablet) 12f else 10f)))
                                }
                                
                                document.add(table)
                                
                                // Add footer with responsive font size
                                document.add(Paragraph("\n"))
                                document.add(Paragraph("Generated by TailorConnect")
                                    .setTextAlignment(TextAlignment.CENTER)
                                    .setFontSize(if (isTablet) 12f else 10f)
                                    .setItalic())
                            }
                        }
                    }
                } ?: run {
                    errorMessage = "Measurement not found"
                }
            } catch (e: Exception) {
                errorMessage = "Failed to save PDF: ${e.message}"
            }
        }
    }

    // CSV save launcher
    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { csvUri ->
            try {
                selectedMeasurementForPdf?.let { measurement ->
                    context.contentResolver.openOutputStream(csvUri)?.use { outputStream ->
                        val csvContent = csvGenerator.generateCsvContent(measurement)
                        outputStream.write(csvContent.toByteArray())
                    }
                } ?: run {
                    errorMessage = "Measurement not found"
                }
            } catch (e: Exception) {
                errorMessage = "Failed to save CSV: ${e.message}"
            }
        }
    }

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = themeState.backgroundColor
    ) {
        Column {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = themeState.surfaceColor,
                contentColor = themeState.textColor
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Text(
                            "Submit",
                            color = themeState.textColor,
                            fontSize = captionSize
                        ) 
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Text(
                            "Measurements",
                            color = themeState.textColor,
                            fontSize = captionSize
                        ) 
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { 
                        Text(
                            "Profile",
                            color = themeState.textColor,
                            fontSize = captionSize
                        ) 
                    }
                )
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(defaultPadding)
                    ) {
                        // Header
                        item {
                            Text(
                                text = "New Measurement",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = titleSize,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = themeState.textColor,
                                modifier = Modifier.padding(bottom = largePadding)
                            )
                        }

                        // Customer Name Section
                        item {
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { 
                                    Text(
                                        "Customer Name",
                                        color = themeState.secondaryTextColor,
                                        fontSize = bodySize
                                    ) 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = defaultPadding),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = themeState.primaryColor,
                                    unfocusedBorderColor = themeState.secondaryTextColor,
                                    focusedLabelColor = themeState.primaryColor,
                                    unfocusedLabelColor = themeState.secondaryTextColor,
                                    cursorColor = themeState.primaryColor,
                                    focusedTextColor = themeState.textColor,
                                    unfocusedTextColor = themeState.textColor
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = bodySize
                                )
                            )
                        }

                        // Gender Selection Section
                        item {
                            Text(
                                text = "Gender",
                                style = MaterialTheme.typography.titleMedium,
                                color = themeState.textColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        item {
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
                                        onClick = { selectedGender = "Male" },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = themeState.primaryColor,
                                            unselectedColor = themeState.secondaryTextColor
                                        )
                                    )
                                    Text("Male", color = themeState.textColor, modifier = Modifier.padding(start = 8.dp))
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
                                        onClick = { selectedGender = "Female" },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = themeState.primaryColor,
                                            unselectedColor = themeState.secondaryTextColor
                                        )
                                    )
                                    Text("Female", color = themeState.textColor, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }

                        // Garment Type Section
                        item {
                            Text(
                                text = "Garment Type",
                                style = MaterialTheme.typography.titleMedium,
                                color = themeState.textColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        item {
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
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = themeState.primaryColor,
                                        unfocusedBorderColor = themeState.secondaryTextColor,
                                        focusedLabelColor = themeState.primaryColor,
                                        unfocusedLabelColor = themeState.secondaryTextColor,
                                        cursorColor = themeState.primaryColor,
                                        focusedTextColor = themeState.textColor,
                                        unfocusedTextColor = themeState.textColor
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedGarmentType,
                                    onDismissRequest = { expandedGarmentType = false }
                                ) {
                                    garmentTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type, color = themeState.textColor) },
                                            onClick = { 
                                                selectedGarmentType = type
                                                expandedGarmentType = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Pocket Style Section (only for Shirt and Trouser)
                        if (selectedGarmentType == "Shirt" || selectedGarmentType == "Trouser") {
                            item {
                                Text(
                                    text = "Pocket Style",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = themeState.textColor,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = themeState.surfaceColor
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        // Top Pocket Style for Shirt
                                        if (selectedGarmentType == "Shirt") {
                                            Text(
                                                text = "Top Pocket Style",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = themeState.textColor,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .selectable(
                                                            selected = measurements["Top Pocket Style"] == "Single",
                                                            onClick = {
                                                                measurements = measurements.toMutableMap().apply {
                                                                    put("Top Pocket Style", "Single")
                                                                }
                                                            }
                                                        )
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = measurements["Top Pocket Style"] == "Single",
                                                        onClick = {
                                                            measurements = measurements.toMutableMap().apply {
                                                                put("Top Pocket Style", "Single")
                                                            }
                                                        },
                                                        colors = RadioButtonDefaults.colors(
                                                            selectedColor = themeState.primaryColor,
                                                            unselectedColor = themeState.secondaryTextColor
                                                        )
                                                    )
                                                    Text(
                                                        "Single Pocket",
                                                        color = themeState.textColor,
                                                        modifier = Modifier.padding(start = 8.dp)
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .selectable(
                                                            selected = measurements["Top Pocket Style"] == "Double",
                                                            onClick = {
                                                                measurements = measurements.toMutableMap().apply {
                                                                    put("Top Pocket Style", "Double")
                                                                }
                                                            }
                                                        )
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = measurements["Top Pocket Style"] == "Double",
                                                        onClick = {
                                                            measurements = measurements.toMutableMap().apply {
                                                                put("Top Pocket Style", "Double")
                                                            }
                                                        },
                                                        colors = RadioButtonDefaults.colors(
                                                            selectedColor = themeState.primaryColor,
                                                            unselectedColor = themeState.secondaryTextColor
                                                        )
                                                    )
                                                    Text(
                                                        "Double Pocket",
                                                        color = themeState.textColor,
                                                        modifier = Modifier.padding(start = 8.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Bottom Pocket Style for Trouser
                                        if (selectedGarmentType == "Trouser") {
                                            Text(
                                                text = "Bottom Pocket Style",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = themeState.textColor,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .selectable(
                                                            selected = measurements["Bottom Pocket Style"] == "Single",
                                                            onClick = {
                                                                measurements = measurements.toMutableMap().apply {
                                                                    put("Bottom Pocket Style", "Single")
                                                                }
                                                            }
                                                        )
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = measurements["Bottom Pocket Style"] == "Single",
                                                        onClick = {
                                                            measurements = measurements.toMutableMap().apply {
                                                                put("Bottom Pocket Style", "Single")
                                                            }
                                                        },
                                                        colors = RadioButtonDefaults.colors(
                                                            selectedColor = themeState.primaryColor,
                                                            unselectedColor = themeState.secondaryTextColor
                                                        )
                                                    )
                                                    Text(
                                                        "Single Pocket",
                                                        color = themeState.textColor,
                                                        modifier = Modifier.padding(start = 8.dp)
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .selectable(
                                                            selected = measurements["Bottom Pocket Style"] == "Double",
                                                            onClick = {
                                                                measurements = measurements.toMutableMap().apply {
                                                                    put("Bottom Pocket Style", "Double")
                                                                }
                                                            }
                                                        )
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = measurements["Bottom Pocket Style"] == "Double",
                                                        onClick = {
                                                            measurements = measurements.toMutableMap().apply {
                                                                put("Bottom Pocket Style", "Double")
                                                            }
                                                        },
                                                        colors = RadioButtonDefaults.colors(
                                                            selectedColor = themeState.primaryColor,
                                                            unselectedColor = themeState.secondaryTextColor
                                                        )
                                                    )
                                                    Text(
                                                        "Double Pocket",
                                                        color = themeState.textColor,
                                                        modifier = Modifier.padding(start = 8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Customer Images Section
                        item {
                            CustomerImageCapture(
                                customerImages = customerImages,
                                onImagesChanged = { newImages ->
                                    customerImages = newImages
                                },
                                themeState = themeState,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                        }

                        // Measurements Section
                        item {
                            Text(
                                text = "Measurements",
                                style = MaterialTheme.typography.titleMedium,
                                color = themeState.textColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        val measurementFields = MeasurementFields.getMeasurementFields(selectedGender, selectedGarmentType)
                        items(measurementFields) { field ->
                            val isOptional = field == "Message" || field == "Comment"
                            OutlinedTextField(
                                value = measurements[field] ?: "",
                                onValueChange = { newValue ->
                                    measurements = measurements.toMutableMap().apply {
                                        put(field, newValue)
                                    }
                                },
                                label = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = field,
                                            color = themeState.secondaryTextColor
                                        )
                                        if (isOptional) {
                                            Text(
                                                text = " (Optional)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = themeState.secondaryTextColor.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = themeState.primaryColor,
                                    unfocusedBorderColor = themeState.secondaryTextColor,
                                    focusedLabelColor = themeState.primaryColor,
                                    unfocusedLabelColor = themeState.secondaryTextColor,
                                    cursorColor = themeState.primaryColor,
                                    focusedTextColor = themeState.textColor,
                                    unfocusedTextColor = themeState.textColor
                                ),
                                placeholder = if (isOptional) {
                                    { Text("Enter ${field.lowercase()} (optional)") }
                                } else null
                            )
                        }

                        // Submit Button
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (customerName.isBlank()) {
                                        errorMessage = "Please enter customer name"
                                        return@Button
                                    }

                                    isLoading = true
                                    CoroutineScope(Dispatchers.Main).launch {
                                        try {
                                            val measurement = Measurement(
                                                id = "",
                                                customerName = customerName,
                                                tailorId = "",
                                                adminId = adminId,
                                                timestamp = System.currentTimeMillis(),
                                                bodyTypeImageId = null,
                                                customerImageUrls = customerImages,
                                                dimensions = measurements.toMutableMap().apply {
                                                    put("Garment Type", selectedGarmentType)
                                                }
                                            )

                                            try {
                                                repository.addMeasurement(measurement)
                                                Log.d("MeasurementSubmit", "Measurement submitted successfully")
                                                showDialog = true
                                                customerName = ""
                                                customerImages = emptyList()
                                                measurements = MeasurementFields.getMeasurementFields(selectedGender, selectedGarmentType)
                                                    .associateWith { "" }
                                                    .toMutableMap()
                                                allMeasurements = repository.getAllMeasurements()
                                            } catch (e: Exception) {
                                                Log.e("MeasurementSubmit", "Failed to submit measurement", e)
                                                errorMessage = "Failed to submit measurement: ${e.message}"
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MeasurementSubmit", "Error in submission process", e)
                                            errorMessage = "Failed to submit measurement: ${e.message}"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = themeState.primaryColor
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(
                                        "Submit",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
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
                                style = MaterialTheme.typography.titleLarge,
                                color = themeState.textColor
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
                                    contentDescription = "Refresh",
                                    tint = themeState.primaryColor
                                )
                            }
                        }

                        if (allMeasurements.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "No measurements available",
                                    color = themeState.textColor,
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
                                            .padding(vertical = 8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = themeState.surfaceColor
                                        )
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
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = themeState.textColor
                                                    )
                                                    Text(
                                                        text = "${measurement.dimensions["Garment Type"] ?: ""} • ${formatDate(measurement.timestamp)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = themeState.secondaryTextColor
                                                    )
                                                }
                                                Row {
                                                    IconButton(onClick = { showEditDialog = measurement }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit",
                                                            tint = themeState.primaryColor
                                                        )
                                                    }
                                                    IconButton(onClick = { showDeleteDialog = measurement }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            tint = themeState.primaryColor
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
                                                                "Collapse" else "Expand",
                                                            tint = themeState.primaryColor
                                                        )
                                                    }
                                                }
                                            }

                                            if (expandedMeasurementId == measurement.id) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Divider(color = themeState.secondaryTextColor.copy(alpha = 0.2f))
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
                                                            color = themeState.secondaryTextColor
                                                        )
                                                        Text(
                                                            text = value,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = themeState.textColor
                                                        )
                                                    }
                                                }

                                                // Display customer images if available
                                                if (measurement.customerImageUrls.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        text = "Customer Images",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = themeState.textColor,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                    )
                                                    LazyVerticalGrid(
                                                        columns = GridCells.Fixed(2),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(200.dp)
                                                    ) {
                                                        items(measurement.customerImageUrls) { imageUrl ->
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(context)
                                                                    .data(imageUrl)
                                                                    .crossfade(true)
                                                                    .build(),
                                                                contentDescription = "Customer Image",
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .clip(RoundedCornerShape(8.dp)),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                Button(
                                                    onClick = {
                                                        try {
                                                            selectedMeasurementForPdf = measurement
                                                            savePdfLauncher.launch("measurement_${measurement.customerName}.pdf")
                                                        } catch (e: Exception) {
                                                            errorMessage = "Failed to generate PDF: ${e.message}"
                                                        }
                                                    },
                                                        modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = themeState.primaryColor
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.GetApp,
                                                        contentDescription = "Download PDF",
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                        Text("PDF")
                                                    }
                                                    
                                                    Button(
                                                        onClick = {
                                                            try {
                                                                selectedMeasurementForPdf = measurement
                                                                saveCsvLauncher.launch("measurement_${measurement.customerName}.csv")
                                                            } catch (e: Exception) {
                                                                errorMessage = "Failed to generate CSV: ${e.message}"
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = themeState.primaryColor
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.FileDownload,
                                                            contentDescription = "Download CSV",
                                                            modifier = Modifier.padding(end = 8.dp)
                                                        )
                                                        Text("CSV")
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
                                    title = { Text("Delete Measurement", color = themeState.textColor) },
                                    text = { Text("Are you sure you want to delete this measurement?", color = themeState.textColor) },
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
                                            Text("Delete", color = themeState.primaryColor)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteDialog = null }) {
                                            Text("Cancel", color = themeState.primaryColor)
                                        }
                                    },
                                    containerColor = themeState.surfaceColor
                                )
                            }

                            // Edit Dialog
                            showEditDialog?.let { measurement ->
                                var editedCustomerName by remember { mutableStateOf(measurement.customerName) }
                                var editedMeasurements by remember { mutableStateOf(measurement.dimensions) }
                                
                                AlertDialog(
                                    onDismissRequest = { showEditDialog = null },
                                    title = { Text("Edit Measurement", color = themeState.textColor) },
                                    text = {
                                        Column(
                                            modifier = Modifier
                                                .verticalScroll(rememberScrollState())
                                                .fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = editedCustomerName,
                                                onValueChange = { editedCustomerName = it },
                                                label = { Text("Customer Name", color = themeState.secondaryTextColor) },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = themeState.primaryColor,
                                                    unfocusedBorderColor = themeState.secondaryTextColor,
                                                    focusedLabelColor = themeState.primaryColor,
                                                    unfocusedLabelColor = themeState.secondaryTextColor,
                                                    cursorColor = themeState.primaryColor,
                                                    focusedTextColor = themeState.textColor,
                                                    unfocusedTextColor = themeState.textColor
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            editedMeasurements.forEach { (key, value) ->
                                                val isOptional = key == "Message" || key == "Comment"
                                                OutlinedTextField(
                                                    value = value,
                                                    onValueChange = { newValue ->
                                                        editedMeasurements = editedMeasurements.toMutableMap().apply {
                                                            put(key, newValue)
                                                        }
                                                    },
                                                    label = { 
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = key,
                                                                color = themeState.secondaryTextColor
                                                            )
                                                            if (isOptional) {
                                                                Text(
                                                                    text = " (Optional)",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = themeState.secondaryTextColor.copy(alpha = 0.7f)
                                                                )
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = themeState.primaryColor,
                                                        unfocusedBorderColor = themeState.secondaryTextColor,
                                                        focusedLabelColor = themeState.primaryColor,
                                                        unfocusedLabelColor = themeState.secondaryTextColor,
                                                        cursorColor = themeState.primaryColor,
                                                        focusedTextColor = themeState.textColor,
                                                        unfocusedTextColor = themeState.textColor
                                                    ),
                                                    placeholder = if (isOptional) {
                                                        { Text("Enter ${key.lowercase()} (optional)") }
                                                    } else null
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    try {
                                                        val updatedMeasurement = measurement.copy(
                                                            customerName = editedCustomerName,
                                                            dimensions = editedMeasurements
                                                        )
                                                        repository.editMeasurement(updatedMeasurement)
                                                        allMeasurements = repository.getAllMeasurements()
                                                        showEditDialog = null
                                                    } catch (e: Exception) {
                                                        errorMessage = "Failed to update measurement: ${e.message}"
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("Save", color = themeState.primaryColor)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showEditDialog = null }) {
                                            Text("Cancel", color = themeState.primaryColor)
                                        }
                                    },
                                    containerColor = themeState.surfaceColor
                                )
                            }
                        }
                    }
                }
                2 -> {
                    ProfileSection(profileViewModel, adminId, navController, themeState)
                }
            }
        }
    }

    // Error Dialog
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error", color = themeState.textColor) },
            text = { Text(errorMessage!!, color = themeState.textColor) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK", color = themeState.primaryColor)
                }
            },
            containerColor = themeState.surfaceColor
        )
    }

    // Success Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Success", color = themeState.textColor) },
            text = { Text("Measurement submitted successfully", color = themeState.textColor) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK", color = themeState.primaryColor)
                }
            },
            containerColor = themeState.surfaceColor
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
