package com.example.tailorconnect.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tailorconnect.data.model.User
import com.example.tailorconnect.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import android.util.Log

@Composable
fun ProfileSection(viewModel: ProfileViewModel, userId: String, navController: NavController) {
    Log.d("ProfileSection", "Loading profile for userId: $userId")
    var user by remember { mutableStateOf<User?>(null) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        try {
            isLoading = true
            errorMessage = null
            if (userId.isBlank()) {
                errorMessage = "User ID not available. Please log in again."
                isLoading = false
                return@LaunchedEffect
            }

            user = viewModel.getUser(userId)
            user?.let {
                name = it.name
                email = it.email
                phone = it.phone
            } ?: run {
                // If user is null, check if it's a known default ID and retry
                if (userId == "admin_id" && viewModel.getUser("test_admin") != null) {
                    user = viewModel.getUser("test_admin")
                    user?.let {
                        name = it.name
                        email = it.email
                        phone = it.phone
                    }
                } else {
                    errorMessage = "Unable to load user profile"
                }
            }
        } catch (e: Exception) {
            errorMessage = "Error loading profile: ${e.message}"
            Log.e("ProfileSection", "Error: ${e.message}", e)
        } finally {
            isLoading = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Profile Information",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = {
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                ) {
                    Text("Log In Again")
                }
            } else if (user != null) {
                if (isEditing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        user?.let {
                                            val updatedUser = it.copy(
                                                name = name,
                                                email = email,
                                                phone = phone
                                            )
                                            viewModel.updateProfile(updatedUser)
                                            isEditing = false
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to update: ${e.message}"
                                    }
                                }
                            }
                        ) {
                            Text("Save Changes")
                        }
                        OutlinedButton(
                            onClick = { isEditing = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                } else {
                    ProfileInfoItem("Name", name)
                    ProfileInfoItem("Email", email)
                    ProfileInfoItem("Phone", phone)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { isEditing = true }
                        ) {
                            Text("Edit Profile")
                        }
                        OutlinedButton(
                            onClick = {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        ) {
                            Text("Logout")
                        }
                    }
                }
            } else {
                Text("No profile information available")
            }
        }
    }
}

@Composable
private fun ProfileInfoItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}