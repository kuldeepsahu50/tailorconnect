package com.example.tailorconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tailorconnect.data.model.User
import com.example.tailorconnect.data.model.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignupViewModel(private val repository: AppRepository) : ViewModel() {
    suspend fun signup(
        name: String,
        email: String,
        phone: String,
        username: String,
        password: String,
        role: String,
        uniqueCode: String? = null
    ) {
        val user = User(
            id = "",
            role = role,
            name = name,
            email = email,
            phone = phone,
            username = username,
            password = password,
            uniqueCode = if (role == "Admin") uniqueCode ?: "" else ""
        )
        return repository.signup(user, uniqueCode)
    }
}