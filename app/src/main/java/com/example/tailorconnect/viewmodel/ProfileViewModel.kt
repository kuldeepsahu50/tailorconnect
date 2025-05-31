package com.example.tailorconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tailorconnect.data.model.User
import com.example.tailorconnect.data.model.repository.AppRepository
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: AppRepository) : ViewModel() {
    suspend fun getUser(userId: String) = repository.getUser(userId)
    fun updateProfile(user: User) {
        viewModelScope.launch {
            repository.updateUser(user)
        }
    }
}