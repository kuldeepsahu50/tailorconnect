package com.example.tailorconnect.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tailorconnect.data.repository.AppRepository
import com.example.tailorconnect.viewmodel.SignupViewModel
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(navController: NavController, repository: AppRepository) {
    val viewModel = SignupViewModel(repository)
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Tailor") }
    var uniqueCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            RadioButton(selected = role == "Admin", onClick = { role = "Admin" })
            Text("Admin", modifier = Modifier.padding(start = 4.dp, end = 16.dp))
            RadioButton(selected = role == "Tailor", onClick = { role = "Tailor" })
            Text("Tailor")
        }

        if (role == "Admin") {
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = uniqueCode,
                onValueChange = { uniqueCode = it },
                label = { Text("Admin Code") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(onClick = {
            scope.launch {
                try {
                    viewModel.signup(
                        name = name,
                        email = email,
                        phone = phone,
                        username = username,
                        password = password,
                        role = role,
                        uniqueCode = if (role == "Admin") uniqueCode else null
                    )
                    navController.navigate("login")
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Signup failed"
                }
            }
        }) {
            Text("Sign Up")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { navController.navigate("login") }) {
            Text("Already have an account? Login")
        }
    }
}