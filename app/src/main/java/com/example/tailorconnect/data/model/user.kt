package com.example.tailorconnect.data.model

data class User(
    val id: String = "",
    val role: String = "", // "Admin" or "Tailor"
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val username: String = "",
    val password: String = "",
    val uniqueCode: String = "" // Added for admin code
)