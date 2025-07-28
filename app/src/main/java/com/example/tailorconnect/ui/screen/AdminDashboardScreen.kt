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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.filled.Search
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
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.graphics.Color
import android.media.MediaPlayer
import android.content.Context
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PhotoLibrary

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
    
    // Add error state for repository
    var repositoryError by remember { mutableStateOf<String?>(null) }
    
    // Validate repository
    if (repository == null) {
        Log.e("AdminDashboard", "Repository is null")
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Error: Repository not available",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

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
    var customerName by remember { mutableStateOf("") }
    var measurements by remember { mutableStateOf(mutableMapOf<String, String>()) }
    var customerImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var audioFileUrl by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var expandedGarmentType by remember { mutableStateOf(false) }
    var allMeasurements by remember { mutableStateOf<List<Measurement>>(emptyList()) }
    var selectedMeasurementForPdf by remember { mutableStateOf<Measurement?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    val tailorViewModel = remember { TailorViewModel(repository) }
    val tailors by tailorViewModel.tailors.collectAsState()
    val profileViewModel = remember { ProfileViewModel(repository) }

    val context = LocalContext.current
    val pdfGenerator = remember { PdfGenerator(context) }
    val csvGenerator = remember { CsvGenerator(context) }
    val storage = Firebase.storage
    
    // Create optimized image loader with better caching
    val imageLoader = remember {
        coil.ImageLoader.Builder(context)
            .memoryCache {
                coil.memory.MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // Use 2% of available disk space
                    .build()
            }
            .crossfade(true)
            .crossfade(300) // 300ms crossfade
            .build()
    }

    // Add network connectivity check
    var isNetworkAvailable by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
            // Check network connectivity
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            isNetworkAvailable = capabilities != null && (
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
            )
            
            if (!isNetworkAvailable) {
                Log.w("AdminDashboard", "No network connectivity detected")
                repositoryError = "No internet connection. Please check your network settings."
                return@LaunchedEffect
            }
            
            Log.d("AdminDashboard", "Network connectivity: $isNetworkAvailable")
        } catch (e: Exception) {
            Log.e("AdminDashboard", "Error checking network connectivity", e)
            // Continue anyway, let the repository handle network errors
        }
    }

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
                                measurement.dimensions?.forEach { (key, value) ->
                                    table.addCell(Cell().add(Paragraph(key ?: "Unknown")
                                        .setFontSize(if (isTablet) 12f else 10f)))
                                    table.addCell(Cell().add(Paragraph(value ?: "")
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
            Log.d("AdminDashboard", "Loading measurements...")
            // Add timeout for network operations
            withTimeout(30000) { // 30 seconds timeout
                allMeasurements = repository.getAllMeasurements()
            }
            Log.d("AdminDashboard", "Loaded ${allMeasurements.size} measurements")
            allMeasurements.forEach { measurement ->
                Log.d("AdminDashboard", "Loaded measurement: id=${measurement.id}, " +
                    "customer=${measurement.customerName}, " +
                    "customerImageUrls=${measurement.customerImageUrls.size} images")
                if (measurement.customerImageUrls.isNotEmpty()) {
                    Log.d("AdminDashboard", "Customer image URLs for ${measurement.customerName}: ${measurement.customerImageUrls}")
                }
            }
        } catch (e: Exception) {
            Log.e("AdminDashboard", "Failed to load measurements: ${e.message}", e)
            when {
                e.message?.contains("network", ignoreCase = true) == true || 
                e.message?.contains("connection", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true -> {
                    repositoryError = "Network connection error. Please check your internet connection."
                }
                e.message?.contains("permission", ignoreCase = true) == true -> {
                    repositoryError = "Permission denied. Please check your Firebase configuration."
                }
                else -> {
                    errorMessage = "Failed to load measurements: ${e.message}"
                }
            }
        }
    }

    // Ensure measurements map is reset when gender changes
    LaunchedEffect(selectedGender) {
        val fields = if (selectedGender == "Male") {
            listOf(
                // Coat
                "COAT LENGTH", "CHEST", "LOWER CHEST", "STOMACH", "CROSS CHEST", "HIP", "SHOULDER", "SLEEVES", "CROSS BACK", "BACK LENGTH", "NECK",
                // Shirt
                "SHIRT LENGTH", "CHEST", "LOWER CHEST", "STOMACH", "CROSS CHEST", "HIP", "SHOULDER", "SLEEVES", "CROSS BACK", "BACK LENGTH", "NECK",
                // Trouser
                "TROUSER LENGTH", "WAIST", "HIP", "THIGH", "KNEE", "BOTTOM", "CROTCH HALF", "CROTCH FULL", "IN SEEM LENGTH",
                // Misc
                "W.COATLENGTH", "KNEE LENGTH", "NEHRU JACKET LENGTH", "CALF", "CHURIDAR BOTTOM", "JJ SHOULDER", "TROUSER BACK POCKET", "SHIRT POCKET", "BISCEP", "ELBOW",
                // COMMENT
                "COMMENT"
            )
        } else if (selectedGender == "Female") {
            listOf(
                "Shoulder", "Upper bust", "Bust", "Waist", "Upper hip", "Hip", "Dp", "Biscep", "Armhole", "Front cross", "Back cross",
                "Sleeve length (half)", "Mohri", "Sleeve length 3/4", "Mohri", "Sleeve length full", "Mohri", "Neck front", "Neck back",
                "Pant Length", "Pant Waist", "Pant Thigh", "Pant Knee", "Pant Calf", "Pant Mohri",
                "Blouse Length", "Blouse Waist",
                "Full length", "Calf length", "Knee length", "Thigh length", "Shirt length",
                "Comments"
            )
        } else emptyList()
        measurements = fields.associateWith { "" }.toMutableMap()
    }

    // Add section selection for Shirt, Coat, Trouser
    val measurementSections = listOf("Shirt", "Coat", "Trouser")
    var selectedSection by remember { mutableStateOf("Shirt") }

    // Show radio buttons for each section
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        measurementSections.forEach { section ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { selectedSection = section }
            ) {
                RadioButton(
                    selected = selectedSection == section,
                    onClick = { selectedSection = section },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = themeState.primaryColor,
                        unselectedColor = themeState.secondaryTextColor
                    )
                )
                Text(section, color = themeState.textColor, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }

    // Show all measurement fields for Male (from chart) together
    if (selectedGender == "Male") {
        val garmentOptions = listOf("Shirt", "Coat", "Trouser", "Misc")
        var selectedGarment by remember { mutableStateOf("Shirt") }

        // Radio buttons for garment selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            garmentOptions.forEach { garment ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { selectedGarment = garment }
                ) {
                    RadioButton(
                        selected = selectedGarment == garment,
                        onClick = { selectedGarment = garment },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = themeState.primaryColor,
                            unselectedColor = themeState.secondaryTextColor
                        )
                    )
                    Text(garment, color = themeState.textColor, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        // Show only the selected garment's measurement fields
        val fields = when (selectedGarment) {
            "Coat" -> listOf(
                "COAT LENGTH", "CHEST", "LOWER CHEST", "STOMACH", "CROSS CHEST", "HIP", "SHOULDER", "SLEEVES", "CROSS BACK", "BACK LENGTH", "NECK"
            )
            "Shirt" -> listOf(
                "SHIRT LENGTH", "CHEST", "LOWER CHEST", "STOMACH", "CROSS CHEST", "HIP", "SHOULDER", "SLEEVES", "CROSS BACK", "BACK LENGTH", "NECK"
            )
            "Trouser" -> listOf(
                "TROUSER LENGTH", "WAIST", "HIP", "THIGH", "KNEE", "BOTTOM", "CROTCH HALF", "CROTCH FULL", "IN SEEM LENGTH"
            )
            "Misc" -> listOf(
                "W.COATLENGTH", "KNEE LENGTH", "NEHRU JACKET LENGTH", "CALF", "CHURIDAR BOTTOM", "JJ SHOULDER", "TROUSER BACK POCKET", "SHIRT POCKET", "BISCEP", "ELBOW"
            )
            else -> emptyList()
        }
        Text(
            text = "$selectedGarment Measurements",
            style = MaterialTheme.typography.titleMedium,
            color = themeState.textColor,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        fields.forEach { field ->
            OutlinedTextField(
                value = measurements[field] ?: "",
                onValueChange = { newValue ->
                    measurements = measurements.toMutableMap().apply { put(field, newValue) }
                },
                label = { Text(field, color = themeState.secondaryTextColor) },
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
                )
            )
        }
    }

    // Submit Button
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (customerName.isBlank()) {
                    errorMessage = "Please enter customer name"
                    return@Button
                }

                // Validate adminId
                if (adminId.isBlank()) {
                    errorMessage = "Invalid admin ID"
                    return@Button
                }

                isLoading = true
                CoroutineScope(Dispatchers.Main).launch {
                    // Check for unique customer name
                    val isUsed = repository.isCustomerNameUsed(customerName.trim())
                    if (isUsed) {
                        errorMessage = "A measurement for this customer name already exists. Please use a unique name."
                        isLoading = false
                        return@launch
                    }
                    Log.d("MeasurementSubmit", "Starting measurement submission for customer: $customerName")
                    Log.d("MeasurementSubmit", "Customer images to save: "+customerImages.size+" images - $customerImages")
                    val filteredImages = customerImages.filter { it.isNotEmpty() }
                    Log.d("MeasurementSubmit", "Filtered images: "+filteredImages.size+" images - $filteredImages")
                    // Remove empty measurement fields before saving
                    val nonEmptyMeasurements = measurements.filterValues { it.isNotBlank() }.toMutableMap()
                    // Always include Garment Type if male
                    if (selectedGender == "Male") {
                        nonEmptyMeasurements["Garment Type"] = measurements["Garment Type"] ?: ""
                    }
                    val measurement = Measurement(
                        id = "",
                        customerName = customerName.trim(),
                        tailorId = "",
                        adminId = adminId,
                        timestamp = System.currentTimeMillis(),
                        bodyTypeImageId = null,
                        customerImageUrls = filteredImages,
                        audioFileUrl = audioFileUrl,
                        dimensions = nonEmptyMeasurements
                    )
                    Log.d("MeasurementSubmit", "Created measurement with "+measurement.customerImageUrls.size+" customer images")
                    Log.d("MeasurementSubmit", "Measurement customerImageUrls: "+measurement.customerImageUrls)
                    try {
                        repository.addMeasurement(measurement)
                        Log.d("MeasurementSubmit", "Measurement submitted successfully with "+measurement.customerImageUrls.size+" images")
                        showDialog = true
                        customerName = ""
                        customerImages = emptyList()
                        audioFileUrl = null
                        measurements = MeasurementFields.getMeasurementFields(selectedGender)
                            .associateWith { "" }
                            .toMutableMap()
                        // Refresh measurements list
                        try {
                            allMeasurements = repository.getAllMeasurements()
                            Log.d("MeasurementSubmit", "Refreshed measurements list: "+allMeasurements.size+" items")
                        } catch (e: Exception) {
                            Log.e("MeasurementSubmit", "Failed to refresh measurements list: "+e.message, e)
                            // Don't show error to user as the submission was successful
                        }
                    } catch (e: Exception) {
                        Log.e("MeasurementSubmit", "Failed to submit measurement", e)
                        errorMessage = "Failed to submit measurement: "+e.message
                    }
                    isLoading = false
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
                            var isDuplicateName by remember { mutableStateOf(false) }
                            var nameCheckJob by remember { mutableStateOf<Job?>(null) }
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { newName ->
                                    customerName = newName
                                    isDuplicateName = false
                                    nameCheckJob?.cancel()
                                    if (newName.isNotBlank()) {
                                        nameCheckJob = CoroutineScope(Dispatchers.Main).launch {
                                            delay(300) // debounce
                                            val isUsed = repository.isCustomerNameUsed(newName.trim())
                                            isDuplicateName = isUsed
                                        }
                                    }
                                },
                                label = { 
                                    Text(
                                        "Customer Name",
                                        color = themeState.secondaryTextColor,
                                        fontSize = bodySize
                                    ) 
                                },
                                isError = isDuplicateName,
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
                            if (isDuplicateName) {
                                Text(
                                    text = "A measurement for this customer name already exists.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                )
                            }
                        }

                        // Gender Selection Section
                        item {
                            Text(
                                text = "Gender:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = themeState.textColor,
                                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(32.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .selectable(
                                            selected = selectedGender == "Male",
                                            onClick = { selectedGender = "Male" }
                                        )
                                        .padding(4.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedGender == "Male",
                                        onClick = { selectedGender = "Male" },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = themeState.primaryColor,
                                            unselectedColor = themeState.secondaryTextColor
                                        )
                                    )
                                    Text("Male", color = themeState.textColor, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .selectable(
                                            selected = selectedGender == "Female",
                                            onClick = { selectedGender = "Female" }
                                        )
                                        .padding(4.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedGender == "Female",
                                        onClick = { selectedGender = "Female" },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = themeState.primaryColor,
                                            unselectedColor = themeState.secondaryTextColor
                                        )
                                    )
                                    Text("Female", color = themeState.textColor, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                        // Pocket Style Section (only for Shirt and Trouser)
                        if (selectedSection == "Shirt" || selectedSection == "Trouser") {
                            item {
                                Text(
                                    text = "Pockets:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = themeState.textColor,
                                    modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                                )
                            }
                            item {
                                val pocketKey = if (selectedSection == "Shirt") "Top Pocket Style" else "Bottom Pocket Style"
                                val pocketValue = measurements[pocketKey] ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 32.dp),
                                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .selectable(
                                                selected = pocketValue == "Single",
                                                onClick = {
                                                    measurements = measurements.toMutableMap().apply {
                                                        if (pocketValue == "Single") {
                                                            put(pocketKey, "") // Unselect if already selected
                                                        } else {
                                                            put(pocketKey, "Single")
                                                        }
                                                    }
                                                }
                                            )
                                            .padding(4.dp)
                                    ) {
                                        RadioButton(
                                            selected = pocketValue == "Single",
                                            onClick = {
                                                measurements = measurements.toMutableMap().apply {
                                                    if (pocketValue == "Single") {
                                                        put(pocketKey, "") // Unselect if already selected
                                                    } else {
                                                        put(pocketKey, "Single")
                                                    }
                                                }
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = themeState.primaryColor,
                                                unselectedColor = themeState.secondaryTextColor
                                            )
                                        )
                                        Text("Single", color = themeState.textColor, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp))
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .selectable(
                                                selected = pocketValue == "Double",
                                                onClick = {
                                                    measurements = measurements.toMutableMap().apply {
                                                        if (pocketValue == "Double") {
                                                            put(pocketKey, "") // Unselect if already selected
                                                        } else {
                                                            put(pocketKey, "Double")
                                                        }
                                                    }
                                                }
                                            )
                                            .padding(4.dp)
                                    ) {
                                        RadioButton(
                                            selected = pocketValue == "Double",
                                            onClick = {
                                                measurements = measurements.toMutableMap().apply {
                                                    if (pocketValue == "Double") {
                                                        put(pocketKey, "") // Unselect if already selected
                                                    } else {
                                                        put(pocketKey, "Double")
                                                    }
                                                }
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = themeState.primaryColor,
                                                unselectedColor = themeState.secondaryTextColor
                                            )
                                        )
                                        Text("Double", color = themeState.textColor, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        }

                        // Customer Images Section
                        item(key = "customer-image-capture") {
                            CustomerImageCapture(
                                customerImages = customerImages,
                                onImagesChanged = { newImages ->
                                    Log.d("AdminDashboard", "Received ${newImages.size} images from CustomerImageCapture: $newImages")
                                    Log.d("AdminDashboard", "Previous customerImages: $customerImages")
                                    customerImages = newImages
                                    Log.d("AdminDashboard", "Updated customerImages to: $customerImages")
                                },
                                audioFileUrl = audioFileUrl,
                                onAudioFileChanged = { newAudioUrl ->
                                    audioFileUrl = newAudioUrl
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
                        if (selectedGender == "Male") {
                            // Garment selection radio buttons
                            item {
                                val garmentOptions = listOf("Shirt", "Coat", "Trouser", "Misc")
                                var selectedGarment by remember { mutableStateOf("Shirt") }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    garmentOptions.forEach { garment ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { selectedGarment = garment }
                                        ) {
                                            RadioButton(
                                                selected = selectedGarment == garment,
                                                onClick = { selectedGarment = garment },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = themeState.primaryColor,
                                                    unselectedColor = themeState.secondaryTextColor
                                                )
                                            )
                                            Text(garment, color = themeState.textColor, modifier = Modifier.padding(start = 4.dp))
                                        }
                                    }
                                }
                                // Save selectedGarment to measurements for submission
                                LaunchedEffect(selectedGarment) {
                                    measurements = measurements.toMutableMap().apply { put("Garment Type", selectedGarment) }
                                }
                            }
                            // Show only the selected garment's measurement fields
                            val garment = measurements["Garment Type"] ?: "Shirt"
                            val fields = when (garment) {
                                "Coat" -> listOf(
                                    "COAT LENGTH", "CHEST", "LOWER CHEST", "STOMACH", "CROSS CHEST", "HIP", "SHOULDER", "SLEEVES", "CROSS BACK", "BACK LENGTH", "NECK"
                                )
                                "Shirt" -> listOf(
                                    "SHIRT LENGTH", "CHEST", "LOWER CHEST", "STOMACH", "CROSS CHEST", "HIP", "SHOULDER", "SLEEVES", "CROSS BACK", "BACK LENGTH", "NECK"
                                )
                                "Trouser" -> listOf(
                                    "TROUSER LENGTH", "WAIST", "HIP", "THIGH", "KNEE", "BOTTOM", "CROTCH HALF", "CROTCH FULL", "IN SEEM LENGTH", "__BOTTOM_POCKET_STYLE__"
                                )
                                "Misc" -> listOf(
                                    "W.COATLENGTH", "KNEE LENGTH", "NEHRU JACKET LENGTH", "CALF", "CHURIDAR BOTTOM", "JJ SHOULDER", "TROUSER BACK POCKET", "SHIRT POCKET", "BISCEP", "ELBOW"
                                )
                                else -> emptyList()
                            }
                            fields.forEach { field ->
                                if (field == "__BOTTOM_POCKET_STYLE__") {
                                    item {
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
                                } else {
                                    item {
                                        OutlinedTextField(
                                            value = measurements[field] ?: "",
                                            onValueChange = { newValue ->
                                                measurements = measurements.toMutableMap().apply { put(field, newValue) }
                                            },
                                            label = { Text(field, color = themeState.secondaryTextColor) },
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
                                            )
                                        )
                                    }
                                }
                            }
                            item {
                                OutlinedTextField(
                                    value = measurements["Comment"] ?: "",
                                    onValueChange = { newValue ->
                                        measurements = measurements.toMutableMap().apply { put("Comment", newValue) }
                                    },
                                    label = { Text("Comment", color = themeState.secondaryTextColor) },
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
                                    )
                                )
                            }
                        }
                        if (selectedGender == "Female") {
                            val femaleFields = listOf(
                                "Shoulder", "Upper bust", "Bust", "Waist", "Upper hip", "Hip", "Dp", "Biscep", "Armhole", "Front cross", "Back cross",
                                "Sleeve length (half)", "Mohri", "Sleeve length 3/4", "Mohri", "Sleeve length full", "Mohri", "Neck front", "Neck back",
                                // Pant section
                                "Pant....", "Length", "Waist", "Thigh", "Knee", "Calf", "Mohri",
                                // Blouse section
                                "Blouse....", "Length", "Waist",
                                // Lengths
                                "Full length", "Calf length", "Knee length", "Thigh length", "Shirt length",
                                "Comments"
                            )
                            var inPantSection = false
                            var inBlouseSection = false
                            femaleFields.forEach { field ->
                                when (field) {
                                    "Pant...." -> item {
                                        Text(
                                            text = "Pant",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = themeState.textColor,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                        )
                                    }
                                    "Blouse...." -> item {
                                            Text(
                                            text = "Blouse",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = themeState.textColor,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                        )
                                    }
                                    else -> item {
                                        OutlinedTextField(
                                            value = measurements[field] ?: "",
                                            onValueChange = { newValue ->
                                                measurements = measurements.toMutableMap().apply { put(field, newValue) }
                                            },
                                            label = { Text(field, color = themeState.secondaryTextColor) },
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
                                            )
                            )
                                    }
                                }
                            }
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

                                    // Validate adminId
                                    if (adminId.isBlank()) {
                                        errorMessage = "Invalid admin ID"
                                        return@Button
                                    }

                                    isLoading = true
                                    CoroutineScope(Dispatchers.Main).launch {
                                        // Check for unique customer name
                                        val isUsed = repository.isCustomerNameUsed(customerName.trim())
                                        if (isUsed) {
                                            errorMessage = "A measurement for this customer name already exists. Please use a unique name."
                                            isLoading = false
                                            return@launch
                                        }
                                        Log.d("MeasurementSubmit", "Starting measurement submission for customer: $customerName")
                                        Log.d("MeasurementSubmit", "Customer images to save: "+customerImages.size+" images - $customerImages")
                                        val filteredImages = customerImages.filter { it.isNotEmpty() }
                                        Log.d("MeasurementSubmit", "Filtered images: "+filteredImages.size+" images - $filteredImages")
                                        // Remove empty measurement fields before saving
                                        val nonEmptyMeasurements = measurements.filterValues { it.isNotBlank() }.toMutableMap()
                                        // Always include Garment Type if male
                                        if (selectedGender == "Male") {
                                            nonEmptyMeasurements["Garment Type"] = measurements["Garment Type"] ?: ""
                                        }
                                        val measurement = Measurement(
                                            id = "",
                                            customerName = customerName.trim(),
                                            tailorId = "",
                                            adminId = adminId,
                                            timestamp = System.currentTimeMillis(),
                                            bodyTypeImageId = null,
                                            customerImageUrls = filteredImages,
                                            audioFileUrl = audioFileUrl,
                                            dimensions = nonEmptyMeasurements
                                        )
                                        Log.d("MeasurementSubmit", "Created measurement with "+measurement.customerImageUrls.size+" customer images")
                                        Log.d("MeasurementSubmit", "Measurement customerImageUrls: "+measurement.customerImageUrls)
                                        try {
                                            repository.addMeasurement(measurement)
                                            Log.d("MeasurementSubmit", "Measurement submitted successfully with "+measurement.customerImageUrls.size+" images")
                                            showDialog = true
                                            customerName = ""
                                            customerImages = emptyList()
                                            audioFileUrl = null
                                            measurements = MeasurementFields.getMeasurementFields(selectedGender)
                                                .associateWith { "" }
                                                .toMutableMap()
                                            // Refresh measurements list
                                            try {
                                                allMeasurements = repository.getAllMeasurements()
                                                Log.d("MeasurementSubmit", "Refreshed measurements list: "+allMeasurements.size+" items")
                                            } catch (e: Exception) {
                                                Log.e("MeasurementSubmit", "Failed to refresh measurements list: "+e.message, e)
                                                // Don't show error to user as the submission was successful
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MeasurementSubmit", "Failed to submit measurement", e)
                                            errorMessage = "Failed to submit measurement: "+e.message
                                        }
                                        isLoading = false
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
                    var searchQuery by remember { mutableStateOf("") }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search by Customer Name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Icon",
                                    tint = themeState.primaryColor
                                )
                            },
                            shape = RoundedCornerShape(32.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = themeState.surfaceColor.copy(alpha = 0.95f),
                                unfocusedContainerColor = themeState.surfaceColor.copy(alpha = 0.85f),
                                focusedBorderColor = themeState.primaryColor,
                                unfocusedBorderColor = themeState.secondaryTextColor,
                                focusedLabelColor = themeState.primaryColor,
                                unfocusedLabelColor = themeState.secondaryTextColor,
                                cursorColor = themeState.primaryColor,
                                focusedTextColor = themeState.textColor,
                                unfocusedTextColor = themeState.textColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                        // Filtered list
                        val filteredMeasurements = if (searchQuery.isBlank()) {
                            allMeasurements
                        } else {
                            allMeasurements.filter {
                                it.customerName.contains(searchQuery, ignoreCase = true)
                            }
                        }
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
                                        Log.d("AdminDashboard", "Refreshing measurements...")
                                        allMeasurements = repository.getAllMeasurements()
                                        Log.d("AdminDashboard", "Refreshed measurements: ${allMeasurements.size} items")
                                    } catch (e: Exception) {
                                        Log.e("AdminDashboard", "Failed to refresh measurements: ${e.message}", e)
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

                        if (filteredMeasurements.isEmpty()) {
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
                                items(filteredMeasurements) { measurement ->
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
                                                
                                                measurement.dimensions?.forEach { (key, value) ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = key ?: "Unknown",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = themeState.secondaryTextColor
                                                        )
                                                        Text(
                                                            text = value ?: "",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = themeState.textColor
                                                        )
                                                    }
                                                } ?: run {
                                                    Text(
                                                        text = "No measurement data available",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = themeState.secondaryTextColor,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                    )
                                                }

                                                // Display customer images if available
                                                if (measurement.customerImageUrls.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        text = "Customer Images (${measurement.customerImageUrls.size})",
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
                                                        items(measurement.customerImageUrls, key = { it }) { imageUrl ->
                                                            var showImagePreview by remember { mutableStateOf(false) }
                                                            var imageLoadError by remember { mutableStateOf(false) }
                                                            
                                                            Card(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .clickable { showImagePreview = true },
                                                                colors = CardDefaults.cardColors(
                                                                    containerColor = themeState.surfaceColor
                                                                )
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    AsyncImage(
                                                                        model = ImageRequest.Builder(context)
                                                                            .data(imageUrl)
                                                                            .crossfade(true)
                                                                            .memoryCacheKey(imageUrl)
                                                                            .diskCacheKey(imageUrl)
                                                                            .build(),
                                                                                contentDescription = "Customer Image",
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .aspectRatio(1f)
                                                                            .clip(RoundedCornerShape(12.dp)),
                                                                        contentScale = ContentScale.Fit,
                                                                        imageLoader = imageLoader,
                                                                        onState = { state ->
                                                                            // You can handle loading/error here if needed
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                            
                                                            // Image preview dialog
                                                            if (showImagePreview) {
                                                                AlertDialog(
                                                                    onDismissRequest = { showImagePreview = false },
                                                                    confirmButton = {
                                                                        TextButton(onClick = { showImagePreview = false }) {
                                                                            Text("Close", color = themeState.primaryColor)
                                                                        }
                                                                    },
                                                                    text = {
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .aspectRatio(1f)
                                                                                .padding(8.dp),
                                                                            contentAlignment = Alignment.Center
                                                                        ) {
                                                                            AsyncImage(
                                                                                model = ImageRequest.Builder(context)
                                                                                    .data(imageUrl)
                                                                                    .crossfade(true)
                                                                                    .memoryCacheKey(imageUrl)
                                                                                    .diskCacheKey(imageUrl)
                                                                                    .build(),
                                                                                contentDescription = "Customer Image Preview",
                                                                                modifier = Modifier
                                                                                    .fillMaxWidth()
                                                                                    .clip(RoundedCornerShape(12.dp)),
                                                                                contentScale = ContentScale.Fit,
                                                                                imageLoader = imageLoader
                                                                            )
                                                                        }
                                                                    },
                                                                    containerColor = themeState.surfaceColor
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    // Show message when no images are available
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(bottom = 16.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = themeState.surfaceColor.copy(alpha = 0.5f)
                                                        )
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(16.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.PhotoLibrary,
                                                                contentDescription = "No Images",
                                                                tint = themeState.secondaryTextColor,
                                                                modifier = Modifier.size(32.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Text(
                                                                text = "No Customer Images",
                                                                style = MaterialTheme.typography.titleSmall,
                                                                color = themeState.secondaryTextColor
                                                            )
                                                            Text(
                                                                text = "No images were captured for this measurement",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = themeState.secondaryTextColor.copy(alpha = 0.7f)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Display audio recording if available
                                                if (measurement.audioFileUrl != null) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        text = "Voice Recording",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = themeState.textColor,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                    )
                                                    
                                                    var isPlaying by remember { mutableStateOf(false) }
                                                    
                                                    Button(
                                                        onClick = {
                                                            if (isPlaying) {
                                                                stopPlaying(context) {
                                                                    isPlaying = false
                                                                }
                                                            } else {
                                                                playAudio(context, measurement.audioFileUrl!!) {
                                                                    isPlaying = true
                                                                }
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isPlaying) Color.Green else themeState.primaryColor
                                                        ),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                            contentDescription = if (isPlaying) "Stop Playing" else "Play Recording",
                                                            modifier = Modifier.size(20.dp),
                                                            tint = Color.White
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = if (isPlaying) "Stop Playing" else "Play Voice Recording",
                                                            color = Color.White
                                                        )
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

    // Repository Error Dialog
    if (repositoryError != null) {
        AlertDialog(
            onDismissRequest = { 
                repositoryError = null
                // Navigate back to login on repository error
                navController.navigate("login") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            },
            title = { Text("Connection Error", color = themeState.textColor) },
            text = { 
                Text(
                    "There was an error connecting to the database. Please check your internet connection and try again.",
                    color = themeState.textColor
                ) 
            },
            confirmButton = {
                TextButton(onClick = { 
                    repositoryError = null
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }) {
                    Text("Go to Login", color = themeState.primaryColor)
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

    BackHandler {
        when (selectedTab) {
            2 -> selectedTab = 1
            1 -> selectedTab = 0
            0 -> showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App", color = themeState.textColor) },
            text = { Text("Are you sure you want to exit?", color = themeState.textColor) },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; (context as? android.app.Activity)?.finish() }) {
                    Text("Exit", color = themeState.primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel", color = themeState.primaryColor)
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

// Audio playback functions
private var mediaPlayer: MediaPlayer? = null

private fun playAudio(context: Context, audioFileUrl: String, onPlay: () -> Unit) {
    try {
        // Stop any existing player first
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        
        mediaPlayer = MediaPlayer().apply {
            setDataSource(audioFileUrl)
            setOnPreparedListener { mp ->
                try {
                    mp.start()
                    onPlay()
                } catch (e: Exception) {
                    Log.e("AudioPlayback", "Error starting playback: ${e.message}", e)
                }
            }
            setOnCompletionListener {
                onPlay() // Toggle back to false when playback completes
            }
            setOnErrorListener { mp, what, extra ->
                Log.e("AudioPlayback", "MediaPlayer error: what=$what, extra=$extra")
                mp.release()
                false
            }
            prepareAsync() // Use async prepare for network URLs
        }
    } catch (e: Exception) {
        Log.e("AudioPlayback", "Error playing audio: ${e.message}", e)
        // Clean up on error
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

private fun stopPlaying(context: Context, onStop: () -> Unit) {
    try {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        onStop()
    } catch (e: Exception) {
        Log.e("AudioPlayback", "Error stopping playback: ${e.message}", e)
        // Ensure cleanup even on error
        mediaPlayer?.release()
        mediaPlayer = null
        onStop()
    }
}
