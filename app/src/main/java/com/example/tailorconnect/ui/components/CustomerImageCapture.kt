package com.example.tailorconnect.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.tailorconnect.ui.theme.ThemeState
import com.google.firebase.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.FileOutputStream

data class CustomerImage(
    val localUri: Uri?,
    val remoteUrl: String?,
    val isUploading: Boolean = false
)

@Composable
fun CustomerImageCapture(
    customerImages: List<String>, // legacy, for parent compatibility
    onImagesChanged: (List<String>) -> Unit,
    audioFileUrl: String? = null,
    onAudioFileChanged: (String?) -> Unit = {},
    themeState: ThemeState,
    modifier: Modifier = Modifier,
    onUploadingStateChanged: ((Boolean) -> Unit)? = null
) {
    Log.d("CustomerImageCapture", "Component recomposed with ${customerImages.size} parent images")
    val context = LocalContext.current
    val maxImages = 5
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) } // For image preview
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) } // For retake/preview
    var showCapturePreview by remember { mutableStateOf(false) }
    var retakeIndex by remember { mutableStateOf<Int?>(null) } // If retaking, which index
    var isRecording by remember { mutableStateOf(false) }
    var audioFilePath by remember { mutableStateOf<String?>(audioFileUrl) } // Local audio file path
    var isPlaying by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) } // Duration in milliseconds
    var showDeleteRecordingDialog by remember { mutableStateOf(false) }

    // NEW: Use a state list for images with localUri/remoteUrl/isUploading
    val images = remember { mutableStateListOf<CustomerImage>() }
    
    // Track if we've been initialized to prevent clearing on recomposition
    var isInitialized by remember { mutableStateOf(false) }
    
    // Use derived state to prevent unnecessary recompositions
    val currentImageUrls by remember(images) {
        derivedStateOf {
            images.mapNotNull { img ->
                img.remoteUrl ?: img.localUri?.toString()
            }
        }
    }

    // Initialize images from customerImages only once, then maintain internal state
    LaunchedEffect(customerImages) {
        Log.d("CustomerImageCapture", "customerImages changed: ${customerImages.size} images, isInitialized: $isInitialized")
        
        if (!isInitialized && customerImages.isNotEmpty()) {
            // Only initialize once when we have parent images and haven't been initialized
            Log.d("CustomerImageCapture", "Initializing images from parent: $customerImages")
            images.clear()
            images.addAll(customerImages.map { CustomerImage(localUri = null, remoteUrl = it, isUploading = false) })
            isInitialized = true
        } else if (!isInitialized && customerImages.isEmpty() && images.isEmpty()) {
            // Mark as initialized even if no images to prevent future clearing
            isInitialized = true
        }
    }
    // Notify parent when remote URLs change, but only after initialization
    LaunchedEffect(currentImageUrls) {
        if (isInitialized) {
            Log.d("CustomerImageCapture", "Notifying parent with ${currentImageUrls.size} images: $currentImageUrls")
            onImagesChanged(currentImageUrls)
        }
    }
    // Notify parent of uploading state
    val anyUploading = images.any { it.isUploading }
    LaunchedEffect(anyUploading) {
        onUploadingStateChanged?.invoke(anyUploading)
    }

    // Add these state variables for instance management
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }

    // Add cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.apply {
                    if (isRecording) {
                        stop()
                    }
                    release()
                }
                mediaPlayer?.apply {
                    if (isPlaying) {
                        stop()
                    }
                    release()
                }
                mediaRecorder = null
                mediaPlayer = null
                currentAudioFile = null
            } catch (e: Exception) {
                Log.e("CustomerImageCapture", "Error during cleanup: ${e.message}", e)
            }
        }
    }

    // Update local audioFilePath when audioFileUrl changes
    LaunchedEffect(audioFileUrl) {
        audioFilePath = audioFileUrl
    }

    // Timer for recording duration
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(100) // Update every 100ms for smooth timer
                recordingDuration += 100
            }
        } else {
            recordingDuration = 0L
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempImageUri != null) {
            pendingImageUri = tempImageUri
            showCapturePreview = true
        } else {
            tempImageUri = null
        }
    }

    // Permission launcher for camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Launch camera after permission is granted
            try {
                tempImageUri = createImageUri(context)
                cameraLauncher.launch(tempImageUri!!)
                // retakeIndex is already set from the calling function
            } catch (e: Exception) {
                Log.e("CustomerImageCapture", "Error launching camera after permission: ${e.message}", e)
            }
        } else {
            Log.w("CustomerImageCapture", "Camera permission denied")
        }
    }

    // Permission launcher for audio recording
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Start recording after permission is granted
            startRecordingLocal(context) { recorder, file ->
                mediaRecorder = recorder
                currentAudioFile = file
                isRecording = true
            }
        }
    }

    // Gallery launcher (optional, not used for retake in this flow)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            pendingImageUri = selectedUri
            showCapturePreview = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Customer Images (${images.size}/$maxImages)",
            style = MaterialTheme.typography.titleMedium,
            color = themeState.textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "(Optional) Capture up to 5 customer images for reference.",
            style = MaterialTheme.typography.bodySmall,
            color = themeState.secondaryTextColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        // Capture Image Button (Material 3)
        Button(
            onClick = {
                if (images.size < maxImages) {
                    // Check camera permission before launching camera
                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                        PackageManager.PERMISSION_GRANTED -> {
                            try {
                                tempImageUri = createImageUri(context)
                                cameraLauncher.launch(tempImageUri!!)
                                retakeIndex = null
                            } catch (e: Exception) {
                                Log.e("CustomerImageCapture", "Error launching camera: ${e.message}", e)
                            }
                        }
                        PackageManager.PERMISSION_DENIED -> {
                            // Request camera permission
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                        else -> {
                            // Permission denied permanently or other state
                            Log.w("CustomerImageCapture", "Camera permission not granted")
                        }
                    }
                }
            },
            enabled = images.size < maxImages,
            modifier = Modifier.padding(bottom = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeState.primaryColor)
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "Capture Image",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Capture Image", color = MaterialTheme.colorScheme.onPrimary)
        }
        // Image grid
        if (images.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                itemsIndexed(images) { idx, img ->
                    Box {
                        CustomerImageItem(
                            imageUrl = img.remoteUrl ?: img.localUri?.toString() ?: "",
                            onDelete = {
                                if (!anyUploading) {
                                    images.removeAt(idx)
                                }
                            },
                            themeState = themeState,
                            onClick = { previewImageUrl = img.remoteUrl ?: img.localUri?.toString() },
                            onRetake = {
                                if (!anyUploading) {
                                    // Check camera permission before launching camera for retake
                                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                                        PackageManager.PERMISSION_GRANTED -> {
                                            try {
                                                tempImageUri = createImageUri(context)
                                                cameraLauncher.launch(tempImageUri!!)
                                                retakeIndex = idx
                                            } catch (e: Exception) {
                                                Log.e("CustomerImageCapture", "Error launching camera for retake: "+e.message, e)
                                            }
                                        }
                                        PackageManager.PERMISSION_DENIED -> {
                                            // Request camera permission
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                        else -> {
                                            Log.w("CustomerImageCapture", "Camera permission not granted for retake")
                                        }
                                    }
                                }
                            }
                        )
                        if (img.isUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }
                    }
                }
            }
            Text(
                text = "Tap image to preview, delete, or retake.",
                style = MaterialTheme.typography.bodySmall,
                color = themeState.secondaryTextColor,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Voice recording section
        Text(
            text = "(Optional) Add a voice note",
            style = MaterialTheme.typography.bodySmall,
            color = themeState.primaryColor,
            modifier = Modifier.padding(end = 8.dp)        )

        VoiceRecorderSection(
            isRecording = isRecording,
            isPlaying = isPlaying,
            audioFilePath = audioFilePath,
            recordingDuration = recordingDuration,
            onRecordClick = {
                if (isRecording) {
                    stopRecordingLocal(context, mediaRecorder, currentAudioFile) { path ->
                        audioFilePath = path
                        isRecording = false
                        mediaRecorder = null
                        currentAudioFile = null
                        // Upload audio file to Firebase and get URL
                        uploadAudioToFirebase(context, path) { url ->
                            onAudioFileChanged(url)
                        }
                    }
                } else {
                    // Check for RECORD_AUDIO permission before starting
                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
                        PackageManager.PERMISSION_GRANTED -> {
                            startRecordingLocal(context) { recorder, file ->
                                mediaRecorder = recorder
                                currentAudioFile = file
                                isRecording = true
                            }
                        }
                        else -> {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            },
            onPlayClick = if (audioFilePath != null) {
                {
                    if (isPlaying) {
                        stopPlayingLocal(mediaPlayer) {
                            isPlaying = false
                            mediaPlayer = null
                        }
                    } else {
                        playAudioLocal(context, audioFilePath!!, mediaPlayer) { player ->
                            isPlaying = true
                            mediaPlayer = player
                        }
                    }
                }
            } else null,
            onDeleteClick = if (audioFilePath != null) {
                {
                    showDeleteRecordingDialog = true
                }
            } else null,
            onReRecordClick = if (audioFilePath != null) {
                {
                    // Delete current recording and start new one
                    deleteAudioFile(audioFilePath!!)
                    audioFilePath = null
                    onAudioFileChanged(null)
                    isPlaying = false
                    // Start new recording
                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
                        PackageManager.PERMISSION_GRANTED -> {
                            startRecordingLocal(context) { recorder, file ->
                                mediaRecorder = recorder
                                currentAudioFile = file
                                isRecording = true
                            }
                        }
                        else -> {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            } else null,
            themeState = themeState
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Delete recording confirmation dialog
    if (showDeleteRecordingDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteRecordingDialog = false },
            title = { Text("Delete Recording", color = themeState.textColor) },
            text = { Text("Are you sure you want to delete this voice recording?", color = themeState.textColor) },
            confirmButton = {
                TextButton(
                    onClick = {
                        audioFilePath?.let { path ->
                            deleteAudioFile(path)
                            audioFilePath = null
                            onAudioFileChanged(null)
                            isPlaying = false
                        }
                        showDeleteRecordingDialog = false
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRecordingDialog = false }) {
                    Text("Cancel", color = themeState.primaryColor)
                }
            },
            containerColor = themeState.surfaceColor
        )
    }

    // Preview dialog for tap
    if (previewImageUrl != null) {
        AlertDialog(
            onDismissRequest = { previewImageUrl = null },
            confirmButton = {
                TextButton(onClick = { previewImageUrl = null }) {
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
                            .data(previewImageUrl)
                            .crossfade(true)
                            .memoryCacheKey(previewImageUrl)
                            .diskCacheKey(previewImageUrl)
                            .build(),
                        contentDescription = "Preview Customer Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            containerColor = themeState.surfaceColor
        )
    }

    // Preview dialog after capture (save/retake)
    if (showCapturePreview && pendingImageUri != null) {
        AlertDialog(
            onDismissRequest = {
                showCapturePreview = false
                pendingImageUri = null
                retakeIndex = null
            },
            title = { Text("Preview Image", color = themeState.textColor) },
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
                            .data(pendingImageUri)
                            .crossfade(true)
                            .memoryCacheKey(pendingImageUri.toString())
                            .diskCacheKey(pendingImageUri.toString())
                            .build(),
                        contentDescription = "Preview Captured Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = {
                        // Retake
                        tempImageUri = createImageUri(context)
                        cameraLauncher.launch(tempImageUri!!)
                        // Keep retakeIndex
                    }) {
                        Text("Retake", color = themeState.primaryColor)
                    }
                    Button(onClick = {
                        // Save: add local image instantly, start upload in background
                        showCapturePreview = false
                        pendingImageUri?.let { uri ->
                            val idx = retakeIndex
                            if (idx != null && idx in images.indices) {
                                images[idx] = CustomerImage(localUri = uri, remoteUrl = null, isUploading = true)
                                uploadImageToFirebaseReplace(
                                    context = context,
                                    imageUri = uri,
                                    index = idx,
                                    images = images
                                )
                            } else {
                                images.add(CustomerImage(localUri = uri, remoteUrl = null, isUploading = true))
                                uploadImageToFirebaseAppend(
                                    context = context,
                                    imageUri = uri,
                                    images = images
                                )
                            }
                        }
                        pendingImageUri = null
                        retakeIndex = null
                    }, colors = ButtonDefaults.buttonColors(containerColor = themeState.primaryColor)) {
                        Text("Save", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCapturePreview = false
                    pendingImageUri = null
                    retakeIndex = null
                }) {
                    Text("Cancel", color = themeState.primaryColor)
                }
            },
            containerColor = themeState.surfaceColor
        )
    }
}

@Composable
private fun VoiceRecorderSection(
    isRecording: Boolean,
    isPlaying: Boolean,
    audioFilePath: String?,
    recordingDuration: Long,
    onRecordClick: () -> Unit,
    onPlayClick: (() -> Unit)?,
    onDeleteClick: (() -> Unit)?,
    onReRecordClick: (() -> Unit)?,
    themeState: ThemeState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Recording duration timer
        if (isRecording) {
            Text(
                text = "Recording: ${formatDuration(recordingDuration)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // Main recording controls - Compact layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Record button - Compact
            Button(
                onClick = onRecordClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color.Red else themeState.primaryColor
                ),
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                // No text for compact button
            }

            // Play button (only show if audio file exists) - Compact
            if (audioFilePath != null && onPlayClick != null) {
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) Color.Green else themeState.primaryColor
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop Playing" else "Play Recording",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "Stop" else "Play",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Secondary controls (Delete and Re-record) - Compact
        if (audioFilePath != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button - Compact
                if (onDeleteClick != null) {
                    OutlinedButton(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Red
                        ),
                        border = BorderStroke(1.dp, Color.Red),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Recording",
                            modifier = Modifier.size(14.dp),
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Re-record button - Compact
                if (onReRecordClick != null) {
                    OutlinedButton(
                        onClick = onReRecordClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = themeState.primaryColor
                        ),
                        border = BorderStroke(1.dp, themeState.primaryColor),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Re-record",
                            modifier = Modifier.size(14.dp),
                            tint = themeState.primaryColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Re-record",
                            color = themeState.primaryColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// Helper function to format duration
private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

@Composable
private fun CustomerImageItem(
    imageUrl: String,
    onDelete: () -> Unit,
    themeState: ThemeState,
    onClick: (() -> Unit)? = null,
    onRetake: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = themeState.secondaryTextColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick?.invoke() },
        contentAlignment = Alignment.TopEnd
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .memoryCacheKey(imageUrl)
                .diskCacheKey(imageUrl)
                .build(),
            contentDescription = "Customer Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { onRetake?.invoke() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "Retake Image",
                    tint = themeState.primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Image",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddImageButton(
    onClick: () -> Unit,
    themeState: ThemeState
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 2.dp,
                color = themeState.primaryColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .background(themeState.surfaceColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Image",
                tint = themeState.primaryColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Add Photo",
                style = MaterialTheme.typography.bodySmall,
                color = themeState.primaryColor,
                fontSize = 12.sp
            )
        }
    }
}

private fun startRecordingLocal(context: Context, onStart: (MediaRecorder, File) -> Unit) {
    try {
        val audioFile = createAudioFile(context)
        val recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }
        onStart(recorder, audioFile)
    } catch (e: Exception) {
        Log.e("VoiceRecorder", "Error starting recording: ${e.message}", e)
    }
}

private fun stopRecordingLocal(context: Context, recorder: MediaRecorder?, file: File?, onStop: (String) -> Unit) {
    try {
        recorder?.apply {
            stop()
            release()
        }
        file?.let { audioFile ->
            onStop(audioFile.absolutePath)
        }
    } catch (e: Exception) {
        Log.e("VoiceRecorder", "Error stopping recording: ${e.message}", e)
    }
}

private fun playAudioLocal(context: Context, audioFileUrl: String, currentPlayer: MediaPlayer?, onPlay: (MediaPlayer) -> Unit) {
    try {
        // Stop any existing player
        currentPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }

        val player = MediaPlayer().apply {
            setDataSource(audioFileUrl)
            setOnPreparedListener { mp ->
                try {
                    mp.start()
                    onPlay(mp)
                } catch (e: Exception) {
                    Log.e("AudioPlayback", "Error starting playback: ${e.message}", e)
                }
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
    }
}

private fun stopPlayingLocal(player: MediaPlayer?, onStop: () -> Unit) {
    try {
        player?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        onStop()
    } catch (e: Exception) {
        Log.e("AudioPlayback", "Error stopping playback: ${e.message}", e)
    }
}

private fun createAudioFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir("VoiceNotes")
    if (!storageDir?.exists()!!) {
        storageDir.mkdirs()
    }
    return File.createTempFile(
        "VOICE_${timeStamp}_",
        ".m4a",
        storageDir
    )
}

private fun createImageUri(context: Context): Uri {
    val photoFile = createImageFile(context)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        photoFile
    )
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir("CustomerImages")
    if (!storageDir?.exists()!!) {
        storageDir.mkdirs()
    }
    return File.createTempFile(
        "CUSTOMER_${timeStamp}_",
        ".jpg",
        storageDir
    )
}

private fun uploadImageToFirebase(
    context: Context,
    imageUri: Uri,
    currentImages: List<String>,
    onImagesChanged: (List<String>) -> Unit,
    retakeIndex: Int? = null
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "customer_images/${timeStamp}_${UUID.randomUUID()}.jpg"
            val imageRef = storageRef.child(imageFileName)
            // Upload the image
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            val newImages = currentImages.toMutableList()
            if (retakeIndex != null && retakeIndex in newImages.indices) {
                newImages[retakeIndex] = downloadUrl.toString()
            } else {
                newImages.add(downloadUrl.toString())
            }
            CoroutineScope(Dispatchers.Main).launch {
                onImagesChanged(newImages)
            }
        } catch (e: Exception) {
            Log.e("ImageUpload", "Error uploading image: ${e.message}", e)
        }
    }
}

fun uploadImageToFirebase(uri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
    val storageRef = Firebase.storage.reference
    val fileRef = storageRef.child("customer_images/${UUID.randomUUID()}.jpg")
    fileRef.putFile(uri)
        .addOnSuccessListener {
            fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                onSuccess(downloadUri.toString())
            }
        }
        .addOnFailureListener { exception ->
            onFailure(exception)
        }
}

private fun deleteAudioFile(audioFilePath: String) {
    val file = File(audioFilePath)
    if (file.exists()) {
        file.delete()
    }
}

private fun uploadAudioToFirebase(
    context: Context,
    audioFilePath: String,
    onSuccess: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val audioFileName = "voice_notes/${timeStamp}_${UUID.randomUUID()}.m4a"
            val audioRef = storageRef.child(audioFileName)

            // Create file from path
            val audioFile = File(audioFilePath)
            if (audioFile.exists()) {
                // Upload the audio file
                audioRef.putFile(Uri.fromFile(audioFile)).await()
                val downloadUrl = audioRef.downloadUrl.await()

                CoroutineScope(Dispatchers.Main).launch {
                    onSuccess(downloadUrl.toString())
                }
            } else {
                Log.e("AudioUpload", "Audio file does not exist: $audioFilePath")
            }
        } catch (e: Exception) {
            Log.e("AudioUpload", "Error uploading audio: ${e.message}", e)
        }
    }
}

// New helper for upload with loading/error
private fun uploadImageToFirebaseWithLoading(
    context: Context,
    imageUri: Uri,
    currentImages: List<String>,
    onImagesChanged: (List<String>) -> Unit,
    onError: (Exception) -> Unit,
    retakeIndex: Int? = null
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "customer_images/${timeStamp}_${UUID.randomUUID()}.jpg"
            val imageRef = storageRef.child(imageFileName)
            // Upload the image
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            val newImages = currentImages.toMutableList()
            if (retakeIndex != null && retakeIndex in newImages.indices) {
                newImages[retakeIndex] = downloadUrl.toString()
            } else {
                newImages.add(downloadUrl.toString())
            }
            CoroutineScope(Dispatchers.Main).launch {
                onImagesChanged(newImages)
            }
        } catch (e: Exception) {
            Log.e("ImageUpload", "Error uploading image: "+e.message, e)
            CoroutineScope(Dispatchers.Main).launch {
                onError(e)
            }
        }
    }
}

// Compress image file to JPEG with given quality, return new file
private fun compressImageFile(context: Context, imageUri: Uri, quality: Int = 75): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        val compressedFile = File.createTempFile("COMPRESSED_", ".jpg", context.cacheDir)
        val out = FileOutputStream(compressedFile)
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.flush()
        out.close()
        compressedFile
    } catch (e: Exception) {
        Log.e("ImageCompress", "Compression failed: ${e.message}", e)
        null
    }
}

// Helper: upload and replace image at index (with compression)
private fun uploadImageToFirebaseReplace(
    context: Context,
    imageUri: Uri,
    index: Int,
    images: SnapshotStateList<CustomerImage>
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            Log.d("ImageUpload", "Starting upload for image at index $index")
            val compressedFile = compressImageFile(context, imageUri)
            val fileToUpload = compressedFile ?: File(imageUri.path ?: "")
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "customer_images/${timeStamp}_${UUID.randomUUID()}.jpg"
            val imageRef = storageRef.child(imageFileName)
            Log.d("ImageUpload", "Uploading to Firebase: $imageFileName")
            imageRef.putFile(Uri.fromFile(fileToUpload)).await()
            val downloadUrl = imageRef.downloadUrl.await()
            Log.d("ImageUpload", "Upload successful, download URL: $downloadUrl")
            withContext(Dispatchers.Main) {
                images[index] = CustomerImage(localUri = null, remoteUrl = downloadUrl.toString(), isUploading = false)
                Log.d("ImageUpload", "Updated image at index $index with remote URL")
            }
            // Clean up temp file
            compressedFile?.delete()
        } catch (e: Exception) {
            Log.e("ImageUpload", "Error uploading image: "+e.message, e)
            withContext(Dispatchers.Main) {
                images[index] = images[index].copy(isUploading = false)
            }
        }
    }
}
// Helper: upload and append image (with compression)
private fun uploadImageToFirebaseAppend(
    context: Context,
    imageUri: Uri,
    images: SnapshotStateList<CustomerImage>
) {
    val index = images.lastIndex
    uploadImageToFirebaseReplace(context, imageUri, index, images)
}

