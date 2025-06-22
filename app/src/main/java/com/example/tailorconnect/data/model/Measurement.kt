package com.example.tailorconnect.data.model

data class Measurement(
    val id: String = "",
    val customerName: String = "",
    val tailorId: String = "",
    val adminId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val bodyTypeImageId: Int? = null,
    val customerImageUrls: List<String> = emptyList(),
    val audioFileUrl: String? = null,
    val dimensions: Map<String, String> = emptyMap()
) {
    constructor() : this("", "", "", "", System.currentTimeMillis(), null, emptyList(), null, emptyMap())
} 