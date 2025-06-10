package com.example.tailorconnect.ui.screen

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tailorconnect.R
import com.example.tailorconnect.data.model.repository.AppRepository
import com.example.tailorconnect.viewmodel.LoginViewModel
import kotlinx.coroutines.delay
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

    // Animation states
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "Alpha Animation"
    )
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1000),
        label = "Scale Animation"
    )
    val slideOffset = animateDpAsState(
        targetValue = if (startAnimation) 0.dp else 100.dp,
        animationSpec = tween(durationMillis = 1000),
        label = "Slide Animation"
    )

    // Start animations when the screen is first displayed
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
                .alpha(alphaAnim.value)
                .offset(y = slideOffset.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo and Welcome Text
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(scaleAnim.value)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Welcome to TailorConnect",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                textAlign = TextAlign.Center,
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
                RoleButton(
                    text = "Admin",
                    isSelected = selectedRole == "Admin",
                    onClick = { selectedRole = "Admin" }
                )

                RoleButton(
                    text = "Tailor",
                    isSelected = selectedRole == "Tailor",
                    onClick = { selectedRole = "Tailor" }
                )
            }

            AnimatedVisibility(
                visible = selectedRole != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    if (selectedRole == "Admin") {
                        if (!isVerificationSent) {
                            AdminLoginForm(
                                name = name,
                                onNameChange = { name = it },
                                email = email,
                                onEmailChange = { email = it },
                                phoneNumber = phoneNumber,
                                onPhoneNumberChange = { phoneNumber = it },
                                errorMessage = errorMessage,
                                isLoading = isLoading,
                                onSendVerification = {
                                    if (phoneNumber.isNotBlank() && name.isNotBlank()) {
                                        scope.launch {
                                            try {
                                                isLoading = true
                                                errorMessage = ""
                                                val result = viewModel.sendVerificationCode(phoneNumber, name, email)
                                                if (result == "auto_verified") {
                                                    // Auto-verification succeeded, try to verify immediately
                                                    val user = viewModel.verifyCode("", selectedRole!!)
                                                    if (user != null) {
                                                        navController.navigate("admin_dashboard/${user.id}") {
                                                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                                        }
                                                    }
                                                } else {
                                                    isVerificationSent = true
                                                }
                                            } catch (e: Exception) {
                                                errorMessage = "Failed to send verification code: ${e.message}"
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    } else {
                                        errorMessage = "Please enter name and phone number"
                                    }
                                }
                            )
                        } else {
                            VerificationCodeForm(
                                verificationCode = verificationCode,
                                onVerificationCodeChange = { verificationCode = it },
                                errorMessage = errorMessage,
                                isLoading = isLoading,
                                onVerify = {
                                    if (verificationCode.isNotBlank()) {
                                        scope.launch {
                                            try {
                                                isLoading = true
                                                errorMessage = ""
                                                val user = viewModel.verifyCode(verificationCode, selectedRole!!)
                                                if (user != null) {
                                                    Log.d("LoginScreen", "Verification successful for user: ${user.id}")
                                                    navController.navigate("admin_dashboard/${user.id}") {
                                                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                                    }
                                                } else {
                                                    Log.e("LoginScreen", "Verification failed: User is null")
                                                    errorMessage = "Invalid verification code"
                                                }
                                            } catch (e: Exception) {
                                                Log.e("LoginScreen", "Verification error: ${e.message}")
                                                errorMessage = "Verification failed: ${e.message}"
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    } else {
                                        errorMessage = "Please enter the verification code"
                                    }
                                },
                                onChangePhone = {
                                    isVerificationSent = false
                                    errorMessage = ""
                                    verificationCode = ""
                                }
                            )
                        }
                    } else {
                        TailorLoginForm(
                            name = name,
                            onNameChange = { name = it },
                            errorMessage = errorMessage,
                            isLoading = isLoading,
                            onLogin = {
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
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.secondary
        ),
        modifier = Modifier
            .width(120.dp)
            .height(48.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun AdminLoginForm(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    errorMessage: String,
    isLoading: Boolean,
    onSendVerification: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Email")
                    Text(
                        text = " (Optional)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            placeholder = { Text("Enter email (optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { newValue ->
                // Only allow digits
                val digitsOnly = newValue.filter { it.isDigit() }
                // Take only first 10 digits
                val truncatedDigits = digitsOnly.take(10)
                onPhoneNumberChange(truncatedDigits)
            },
            label = { Text("Phone Number") },
            placeholder = { Text("Enter 10 digit number") },
            prefix = { Text("+91 ") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
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
            onClick = onSendVerification,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && phoneNumber.length == 10 && name.isNotBlank()
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
    }
}

@Composable
private fun VerificationCodeForm(
    verificationCode: String,
    onVerificationCodeChange: (String) -> Unit,
    errorMessage: String,
    isLoading: Boolean,
    onVerify: () -> Unit,
    onChangePhone: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = verificationCode,
            onValueChange = onVerificationCodeChange,
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
            onClick = onVerify,
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
            onClick = onChangePhone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Change Phone Number")
        }
    }
}

@Composable
private fun TailorLoginForm(
    name: String,
    onNameChange: (String) -> Unit,
    errorMessage: String,
    isLoading: Boolean,
    onLogin: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
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
            onClick = onLogin,
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