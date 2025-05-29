package com.example.tailorconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tailorconnect.data.model.Measurement
import com.example.tailorconnect.data.repository.AppRepository
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TailorViewModel(private val repository: AppRepository) : ViewModel() {
    private val _measurements = MutableStateFlow<List<Measurement>>(emptyList())
    val measurements: StateFlow<List<Measurement>> = _measurements

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadMeasurements(tailorId: String) {
        viewModelScope.launch {
            try {
                Log.d("TailorViewModel", "Starting to load admin measurements")
                _isLoading.value = true
                _error.value = null

                // Get all admin submitted measurements
                val adminMeasurements = repository.getAdminSubmittedMeasurements()
                Log.d("TailorViewModel", "Repository returned ${adminMeasurements.size} admin measurements")

                if (adminMeasurements.isEmpty()) {
                    Log.d("TailorViewModel", "No admin measurements found")
                } else {
                    adminMeasurements.forEach { measurement ->
                        Log.d("TailorViewModel", "Admin measurement details: " +
                            "id=${measurement.id}, " +
                            "customer=${measurement.customerName}, " +
                            "adminId=${measurement.adminId}, " +
                            "timestamp=${measurement.timestamp}")
                    }
                }

                // Sort measurements by timestamp (newest first)
                val sortedMeasurements = adminMeasurements.sortedByDescending { it.timestamp }
                Log.d("TailorViewModel", "Setting ${sortedMeasurements.size} sorted measurements to state")
                _measurements.value = sortedMeasurements

            } catch (e: Exception) {
                Log.e("TailorViewModel", "Error loading measurements", e)
                _error.value = "Failed to load measurements: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d("TailorViewModel", "Finished loading measurements")
            }
        }
    }

    fun refreshMeasurements(tailorId: String) {
        Log.d("TailorViewModel", "Refreshing admin measurements")
        loadMeasurements(tailorId)
    }
}