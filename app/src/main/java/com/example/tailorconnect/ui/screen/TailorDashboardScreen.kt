package com.example.tailorconnect.ui.screen

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import android.media.MediaPlayer
import android.content.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TailorDashboardScreen(repository: AppRepository, tailorId: String, navController: NavController) {
    Log.d("TailorDashboard", "Loading dashboard for tailorId: $tailorId")
    
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
    var expandedMeasurementId by remember { mutableStateOf<String?>(null) }
    val themeState = remember { ThemeState() }
    var selectedMeasurementForPdf by remember { mutableStateOf<Measurement?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // PDF save launcher
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { pdfUri ->
            try {
                selectedMeasurementForPdf?.let { measurement ->
                    context.contentResolver.openOutputStream(pdfUri)?.use { outputStream ->
                        PdfWriter(outputStream).use { writer ->
                            val pdfDoc = PdfDocument(writer)
                            Document(pdfDoc).use { document ->
                                // Add title
                                val title = Paragraph("Measurement Details")
                                    .setTextAlignment(TextAlignment.CENTER)
                                    .setFontSize(20f)
                                    .setBold()
                                document.add(title)
                                
                                // Add customer info
                                document.add(Paragraph("\n"))
                                document.add(Paragraph("Customer Name: ${measurement.customerName}")
                                    .setFontSize(14f))
                                document.add(Paragraph("Date: ${formatDate(measurement.timestamp)}")
                                    .setFontSize(12f))
                                document.add(Paragraph("\n"))
                                
                                // Add measurements table
                                val table = Table(UnitValue.createPercentArray(2))
                                    .useAllAvailableWidth()
                                    .setMarginTop(20f)
                                    .setMarginBottom(20f)
                                
                                // Add header
                                val headerCell1 = Cell().add(Paragraph("Measurement")
                                    .setBold()
                                    .setFontSize(12f))
                                val headerCell2 = Cell().add(Paragraph("Value")
                                    .setBold()
                                    .setFontSize(12f))
                                table.addHeaderCell(headerCell1)
                                table.addHeaderCell(headerCell2)
                                
                                // Add measurement data
                                measurement.dimensions.forEach { (key, value) ->
                                    table.addCell(Cell().add(Paragraph(key)
                                        .setFontSize(10f)))
                                    table.addCell(Cell().add(Paragraph(value)
                                        .setFontSize(10f)))
                                }
                                
                                document.add(table)
                                
                                // Add footer
                                document.add(Paragraph("\n"))
                                document.add(Paragraph("Generated by TailorConnect")
                                    .setTextAlignment(TextAlignment.CENTER)
                                    .setFontSize(10f)
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
                            "Admin Measurements",
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
                            "Profile",
                            color = themeState.textColor,
                            fontSize = captionSize
                        ) 
                    }
                )
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
                                style = MaterialTheme.typography.titleLarge,
                                color = themeState.textColor
                            )
                            IconButton(onClick = { 
                                Log.d("TailorDashboard", "Refresh button clicked")
                                tailorViewModel.refreshMeasurements(tailorId)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = themeState.primaryColor
                                )
                            }
                        }

                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = themeState.primaryColor
                                )
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
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = themeState.textColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Check back later for new measurements",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = themeState.secondaryTextColor
                                    )
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                                items(measurements) { measurement ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .clickable { 
                                                expandedMeasurementId = if (expandedMeasurementId == measurement.id) null else measurement.id
                                            },
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
                                                Column {
                                                    Text(
                                                        text = measurement.customerName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = themeState.textColor
                                                    )
                                                    Text(
                                                        text = formatDate(measurement.timestamp),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = themeState.secondaryTextColor
                                                    )
                                                }
                                                Icon(
                                                    imageVector = if (expandedMeasurementId == measurement.id) 
                                                        Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = if (expandedMeasurementId == measurement.id) 
                                                        "Collapse" else "Expand",
                                                    tint = themeState.primaryColor
                                                )
                                            }

                                            if (expandedMeasurementId == measurement.id) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                                Spacer(modifier = Modifier.height(16.dp))
                                                
                                                // Display customer images if available
                                                if (measurement.customerImageUrls.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        text = "Customer Images",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = themeState.textColor,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                    )
                                                    LazyVerticalGrid(
                                                        columns = GridCells.Fixed(2),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(200.dp)
                                                            .padding(bottom = 16.dp)
                                                    ) {
                                                        items(measurement.customerImageUrls) { imageUrl ->
                                                            Card(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .clip(RoundedCornerShape(8.dp)),
                                                                colors = CardDefaults.cardColors(
                                                                    containerColor = themeState.surfaceColor
                                                                )
                                                            ) {
                                                                AsyncImage(
                                                                    model = ImageRequest.Builder(context)
                                                                        .data(imageUrl)
                                                                        .crossfade(true)
                                                                        .build(),
                                                                    contentDescription = "Customer Photo",
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentScale = ContentScale.Crop
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                                // Display body type image if available
                                                measurement.bodyTypeImageId?.let { bodyTypeId ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(bottom = 16.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = themeState.surfaceColor
                                                        )
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(16.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Text(
                                                                text = "Body Type",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                color = themeState.textColor,
                                                                modifier = Modifier.padding(bottom = 8.dp)
                                                            )
                                                            Image(
                                                                painter = painterResource(id = when (bodyTypeId) {
                                                                    1 -> R.drawable.first
                                                                    2 -> R.drawable.second
                                                                    3 -> R.drawable.third
                                                                    4 -> R.drawable.fourth
                                                                    5 -> R.drawable.five
                                                                    else -> R.drawable.sixth
                                                                }),
                                                                contentDescription = "Body Type $bodyTypeId",
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(200.dp)
                                                                    .padding(8.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                // Display all measurement fields
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

                                                // Display pocket style if available
                                                if (measurement.dimensions.containsKey("Top Pocket Style") || measurement.dimensions.containsKey("Bottom Pocket Style")) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        text = "Pocket Style",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = themeState.textColor,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                    )
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(bottom = 16.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = themeState.surfaceColor
                                                        )
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(16.dp)
                                                        ) {
                                                            // Display top pocket style for shirts
                                                            measurement.dimensions["Top Pocket Style"]?.let { pocketStyle ->
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
                                                                            .padding(8.dp),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        RadioButton(
                                                                            selected = pocketStyle == "Single",
                                                                            onClick = { },
                                                                            enabled = false,
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
                                                                            .padding(8.dp),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        RadioButton(
                                                                            selected = pocketStyle == "Double",
                                                                            onClick = { },
                                                                            enabled = false,
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

                                                            // Display bottom pocket style for trousers
                                                            measurement.dimensions["Bottom Pocket Style"]?.let { pocketStyle ->
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
                                                                            .padding(8.dp),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        RadioButton(
                                                                            selected = pocketStyle == "Single",
                                                                            onClick = { },
                                                                            enabled = false,
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
                                                                            .padding(8.dp),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        RadioButton(
                                                                            selected = pocketStyle == "Double",
                                                                            onClick = { },
                                                                            enabled = false,
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

                                                // Display audio recording if available
                                                if (measurement.audioFileUrl != null) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        text = "Voice Recording",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = themeState.textColor,
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
                                                Button(
                                                    onClick = {
                                                        try {
                                                            selectedMeasurementForPdf = measurement
                                                            savePdfLauncher.launch("measurement_${measurement.customerName}.pdf")
                                                        } catch (e: Exception) {
                                                            errorMessage = "Failed to generate PDF: ${e.message}"
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = themeState.primaryColor
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.GetApp,
                                                        contentDescription = "Download PDF",
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                    Text("Download PDF")
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
                    ProfileSection(profileViewModel, tailorId, navController, themeState)
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
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Audio playback functions
private var mediaPlayer: MediaPlayer? = null

private fun playAudio(context: Context, audioFileUrl: String, onPlay: () -> Unit) {
    try {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(audioFileUrl)
            prepare()
            start()
            setOnCompletionListener {
                onPlay() // Toggle back to false when playback completes
            }
        }
        onPlay()
    } catch (e: Exception) {
        Log.e("AudioPlayback", "Error playing audio: ${e.message}", e)
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
    }
}