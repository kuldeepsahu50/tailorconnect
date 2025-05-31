package com.example.tailorconnect.data.model

object MeasurementFields {
    val measurementFields: Map<String, Map<String, List<String>>> = mapOf(
        "Male" to mapOf(
            "Shirt" to listOf("Shirt Length", "Chest", "Lower Chest", "Stomach", "Cross Chest", "Hip", "Shoulder", "Sleeves", "Sleeve Cuff", "Cross Back", "Back Length", "Neck", "Message", "Comment"),
            "Trouser" to listOf("Trouser Length", "Waist", "Hip", "Thigh", "Knee", "Bottom", "Crotch Half", "Crotch Full", "Inseam Length", "Message", "Comment"),
            "Coat" to listOf("Coat Length", "Chest", "Lower Chest", "Stomach", "Cross Chest", "Hip", "Shoulder", "Sleeves", "Sleeve Bottom", "Back Length", "Neck", "Message", "Comment"),
            "Kurta" to listOf("Kurta Length", "Chest", "Lower Chest", "Stomach", "Cross Chest", "Hip", "Shoulder", "Sleeves", "Sleeve Bottom", "Back Length", "Neck", "Message", "Comment"),
            "Others" to listOf("Length", "Chest", "Lower Chest", "Stomach", "Cross Chest", "Hip", "Shoulder", "Sleeves", "Sleeve Bottom", "Back Length", "Neck", "Message", "Comment")
        ),
        "Female" to mapOf(
            "Blouse" to listOf("Length", "Sleeve Length", "Shoulder", "Upper Bust", "Lower Bust", "Bust", "Waist", "Dart Point", "Neck Line"),
            "Lehenga" to listOf("Lehenga Length", "Waist", "Bottom"),
            "Saree" to listOf("Length"),
            "Shirt" to listOf("Shirt Length", "Upper Bust", "Lower Bust", "Stomach", "Cross Chest", "Hip", "Shoulder", "Sleeves", "Sleeve Cuff", "Cross Back", "Back Length", "Neck", "Message", "Comment"),
            "Trouser" to listOf("Trouser Length", "Waist", "Hip", "Thigh", "Knee", "Bottom", "Crotch Half", "Crotch Full", "Inseam Length", "Message", "Comment"),
            "Coat" to listOf("Coat Length", "Chest", "Lower Chest", "Stomach", "Cross Chest", "Hip", "Shoulder", "Sleeves", "Sleeve Bottom", "Back Length", "Neck", "Message", "Comment"),
            "Kurta" to listOf("Kurta Length", "Chest", "Lower Chest", "Stomach", "Cross Chest", "Hip", "Shoulder", "Sleeves", "Sleeve Bottom", "Back Length", "Neck", "Message", "Comment"),
            "Pant" to listOf("Pant Trouser Length", "Waist", "Hip", "Thigh", "Knee", "Bottom", "Crotch Half", "Crotch Full", "Inseam Length", "Message", "Comment"),
            "Others" to listOf("Length", "Sleeve Length", "Shoulder", "Upper Bust", "Lower Bust", "Bust", "Waist", "Dart Point", "Neck Length")
        )
    )

    fun getGarmentTypes(gender: String): List<String> {
        return measurementFields[gender]?.keys?.toList() ?: emptyList()
    }

    fun getMeasurementFields(gender: String, garmentType: String): List<String> {
        return measurementFields[gender]?.get(garmentType) ?: emptyList()
    }
} 