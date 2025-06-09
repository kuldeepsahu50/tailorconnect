package com.example.tailorconnect.data.model

object MeasurementFields {
    private val maleGarmentTypes = listOf("Shirt", "T-Shirt", "Trouser", "Kurta", "Coat")
    private val femaleGarmentTypes = listOf("Blouse", "Lehenga", "Trouser", "Kurta", "Coat")

    fun getGarmentTypes(gender: String): List<String> {
        return when (gender) {
            "Male" -> maleGarmentTypes
            "Female" -> femaleGarmentTypes
            else -> emptyList()
        }
    }

    fun getMeasurementFields(gender: String, garmentType: String): List<String> {
        return when (gender) {
            "Male" -> getMaleMeasurementFields(garmentType)
            "Female" -> getFemaleMeasurementFields(garmentType)
            else -> emptyList()
        }
    }

    private fun getMaleMeasurementFields(garmentType: String): List<String> {
        return when (garmentType) {
            "Shirt", "T-Shirt" -> listOf(
                "Top Length", "Chest", "Lower Chest", "Stomach", "Cross Chest", 
                "Hip", "Shoulder", "Sleeves", "Sleeve Cuff", "Cross Back", 
                "Back Length", "Neck", "Message", "Comment"
            )
            "Trouser" -> listOf(
                "Bottom Length", "Waist", "Hip", "Thigh", "Knee", "Bottom", 
                "Crotch Half", "Crotch Full", "Inseam Length", "Message", "Comment"
            )
            "Kurta" -> listOf(
                "Kurta Length", "Chest", "Lower Chest", "Stomach", "Cross Chest", 
                "Hip", "Shoulder", "Sleeves", "Sleeve Bottom", "Back Length", 
                "Neck", "Trouser Length", "Waist", "Hip", "Thigh", "Knee", 
                "Bottom", "Crotch Half", "Crotch Full", "Inseam Length", 
                "Message", "Comment"
            )
            "Coat" -> listOf(
                "Coat Length", "Chest", "Lower Chest", "Stomach", "Cross Chest", 
                "Hip", "Shoulder", "Sleeves", "Sleeve Bottom", "Back Length", 
                "Neck", "Message", "Comment"
            )
            else -> emptyList()
        }
    }

    private fun getFemaleMeasurementFields(garmentType: String): List<String> {
        return when (garmentType) {
            "Blouse" -> listOf(
                "Blouse Length", "Sleeve Length", "Shoulder", "Upper Bust", 
                "Lower Bust", "Bust", "Waist", "Dart Point", "Neck Line", 
                "Message", "Comment"
            )
            "Lehenga" -> listOf(
                "Lehenga Length", "Waist", "Bottom", "Top-wear Length", 
                "Stomach", "Cross Chest", "Hip", "Sleeve Cuff", "Cross Back", 
                "Back Length", "Neck", "Message", "Comment"
            )
            "Trouser" -> listOf(
                "Trouser Length", "Waist", "Hip", "Thigh", "Knee", "Bottom", 
                "Crotch Half", "Crotch Full", "Inseam Length", "Message", "Comment"
            )
            "Kurta" -> listOf(
                "Kurta Length", "Chest", "Lower Chest", "Stomach", "Cross Chest", 
                "Hip", "Shoulder", "Sleeves", "Sleeve Bottom", "Back Length", 
                "Neck", "Trouser Length", "Waist", "Hip", "Thigh", "Knee", 
                "Bottom", "Crotch Half", "Crotch Full", "Inseam Length", 
                "Message", "Comment"
            )
            "Coat" -> listOf(
                "Coat Length", "Chest", "Lower Chest", "Stomach", "Cross Chest", 
                "Hip", "Shoulder", "Sleeves", "Sleeve Bottom", "Back Length", 
                "Neck", "Message", "Comment"
            )
            else -> emptyList()
        }
    }
} 