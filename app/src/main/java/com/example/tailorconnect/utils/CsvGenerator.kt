package com.example.tailorconnect.utils

import android.content.Context
import com.example.tailorconnect.data.model.Measurement
import java.text.SimpleDateFormat
import java.util.*

class CsvGenerator(private val context: Context) {
    fun generateCsvContent(measurement: Measurement): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = sdf.format(Date(measurement.timestamp))
        
        val stringBuilder = StringBuilder()
        
        // Add header
        stringBuilder.append("Customer Name,Date,Garment Type\n")
        
        // Add customer info
        stringBuilder.append("${measurement.customerName},$date,${measurement.dimensions["Garment Type"]}\n\n")
        
        // Add measurements header
        stringBuilder.append("Measurement,Value\n")
        
        // Add measurements
        measurement.dimensions.forEach { (key, value) ->
            if (key != "Garment Type") { // Skip garment type as it's already included above
                stringBuilder.append("$key,$value\n")
            }
        }
        
        return stringBuilder.toString()
    }
} 