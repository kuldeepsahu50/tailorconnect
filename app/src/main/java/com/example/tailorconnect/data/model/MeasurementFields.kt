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

    fun getMeasurementFields(gender: String): List<String> {
        return when (gender) {
            "Male" -> listOf(
                // COAT
                "COAT LENGTH", "CHEST", "LOWER CHEST", "STOMACH", "CROSS CHEST", "HIP", "SHOULDER", "SLEEVES", "CROSS BACK", "BACK LENGTH", "NECK",
                // SHIRT
                "SHIRT LENGTH", "CHEST", "LOWER CHEST", "STOMACH", "CROSS CHEST", "HIP", "SHOULDER", "SLEEVES", "CROSS BACK", "BACK LENGTH", "NECK",
                // TROUSER
                "TROUSER LENGTH", "WAIST", "HIP", "THIGH", "KNEE", "BOTTOM", "CROTCH HALF", "CROTCH FULL", "IN SEEM LENGTH",
                // Misc
                "W.COATLENGTH", "KNEE LENGTH", "NEHRU JACKET LENGTH", "CALF", "CHURIDAR BOTTOM", "JJ SHOULDER", "TROUSER BACK POCKET", "SHIRT POCKET", "BISCEP", "ELBOW",
                // COMMENT
                "COMMENT"
            )
            else -> emptyList()
        }
    }

    fun getMeasurementFields(gender: String, garmentType: String): List<String> {
        return when (gender) {
            "Female" -> getFemaleMeasurementFields(garmentType)
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