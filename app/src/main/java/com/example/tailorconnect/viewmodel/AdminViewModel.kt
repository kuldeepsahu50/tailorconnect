package com.example.tailorconnect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tailorconnect.data.model.Measurement
import com.example.tailorconnect.data.model.User
import com.example.tailorconnect.data.model.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel(private val repository: AppRepository) : ViewModel() {
    suspend fun getAllMeasurements() = repository.getAllMeasurements()
    
    suspend fun getAllTailors(): List<User> {
        return repository.getAllUsers().filter { it.role == "Tailor" }
    }

    fun addMeasurement(measurement: Measurement) {
        viewModelScope.launch {
            repository.addMeasurement(measurement)
        }
    }
    fun editMeasurement(measurement: Measurement) {
        viewModelScope.launch {
            repository.editMeasurement(measurement)
        }
    }
    fun deleteMeasurement(measurementId: String) {
        viewModelScope.launch {
            repository.deleteMeasurement(measurementId)
        }
    }

    suspend fun isCustomerNameUsed(customerName: String): Boolean {
        return repository.isCustomerNameUsed(customerName)
    }
}