package com.example.tailorconnect.data.model

data class Measurement(
    val id: String = "",
    val customerName: String = "",
    val tailorId: String = "",
    val adminId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val bodyTypeImageId: Int? = null,
    val dimensions: Map<String, String> = mapOf(
        "chest" to "",
        "waist" to "",
        "hip" to "",
        "shoulder" to "",
        "sleeve" to "",
        "length" to ""
    )
) {
    // Empty constructor for Firebase
    constructor() : this(
        id = "",
        customerName = "",
        tailorId = "",
        adminId = "",
        timestamp = System.currentTimeMillis(),
        bodyTypeImageId = null,
        dimensions = mapOf()
    )
} 