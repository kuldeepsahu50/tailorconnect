package com.example.tailorconnect.viewmodel

import androidx.lifecycle.ViewModel
import com.example.tailorconnect.data.model.User
import com.example.tailorconnect.data.repository.AppRepository
import com.google.firebase.auth.PhoneAuthProvider

class LoginViewModel(private val repository: AppRepository) : ViewModel() {
    suspend fun sendVerificationCode(phoneNumber: String, name: String, email: String): String {
        try {
            return repository.sendVerificationCode(phoneNumber, name, email)
        } catch (e: Exception) {
            throw Exception("Failed to send verification code: ${e.message}")
        }
    }

    suspend fun verifyCode(code: String, role: String): User? {
        try {
            return repository.verifyCode(code, role)
        } catch (e: Exception) {
            throw Exception("Verification failed: ${e.message}")
        }
    }

    suspend fun createTailor(name: String): User {
        try {
            return repository.createTailor(name)
        } catch (e: Exception) {
            throw Exception("Failed to create tailor account: ${e.message}")
        }
    }
}