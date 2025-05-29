package com.example.tailorconnect.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tailorconnect.data.repository.AppRepository
import com.example.tailorconnect.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController, repository: AppRepository) {
    val viewModel = LoginViewModel(repository)
    var phoneNumber by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<String?>(null) }
    var verificationCode by remember { mutableStateOf("") }
    var isVerificationSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to TailorConnect",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Role Selection
        Text(
            text = "Select your role",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { selectedRole = "Admin" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedRole == "Admin") 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Admin")
            }

            Button(
                onClick = { selectedRole = "Tailor" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedRole == "Tailor") 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Tailor")
            }
        }

        if (selectedRole != null) {
            if (selectedRole == "Admin") {
                if (!isVerificationSent) {
                    // Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    // Phone Number Input
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+91XXXXXXXXXX") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (phoneNumber.isNotBlank() && name.isNotBlank() && email.isNotBlank()) {
                                scope.launch {
                                    try {
                                        isLoading = true
                                        errorMessage = ""
                                        viewModel.sendVerificationCode(phoneNumber, name, email)
                                        isVerificationSent = true
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to send verification code: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                errorMessage = "Please fill in all fields"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Send Verification Code")
                        }
                    }
                } else {
                    // Verification Code Input
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it },
                        label = { Text("Verification Code") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (verificationCode.isNotBlank()) {
                                scope.launch {
                                    try {
                                        isLoading = true
                                        errorMessage = ""
                                        val user = viewModel.verifyCode(verificationCode, selectedRole!!)
                                        if (user != null) {
                                            navController.navigate("admin_dashboard/${user.id}") {
                                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                            }
                                        } else {
                                            errorMessage = "Invalid verification code"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Verification failed: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                errorMessage = "Please enter the verification code"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Verify Code")
                        }
                    }

                    TextButton(
                        onClick = { 
                            isVerificationSent = false
                            errorMessage = ""
                            verificationCode = ""
                        }
                    ) {
                        Text("Change Phone Number")
                    }
                }
            } else {
                // Tailor Login - Only Name Required
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            scope.launch {
                                try {
                                    isLoading = true
                                    errorMessage = ""
                                    val user = viewModel.createTailor(name)
                                    navController.navigate("tailor_dashboard/${user.id}") {
                                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Failed to create tailor account: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        } else {
                            errorMessage = "Please enter your name"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Continue")
                    }
                }
            }
        }
    }
}