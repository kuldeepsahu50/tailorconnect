package com.example.tailorconnect.data.model

data class Tailor(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val experience: String = "",
    val specialization: String = "",
    val rating: Double = 0.0,
    val isAvailable: Boolean = true
) 