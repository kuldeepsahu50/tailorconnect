package com.example.tailorconnect.data.repository

import com.example.tailorconnect.data.model.Measurement
import com.example.tailorconnect.data.model.Tailor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class AppRepository {
    private val db = FirebaseFirestore.getInstance()
    private val measurementsCollection = db.collection("measurements")
    private val tailorsCollection = db.collection("tailors")

    suspend fun addMeasurement(measurement: Measurement) {
        measurementsCollection.add(measurement).await()
    }

    suspend fun editMeasurement(measurement: Measurement) {
        measurementsCollection.document(measurement.id)
            .set(measurement)
            .await()
    }

    suspend fun deleteMeasurement(measurementId: String) {
        measurementsCollection.document(measurementId).delete().await()
    }

    fun getMeasurementsForTailor(tailorId: String): Flow<List<Measurement>> = flow {
        val snapshot = measurementsCollection
            .whereEqualTo("tailorId", tailorId)
            .get()
            .await()
        
        val measurements = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Measurement::class.java)?.copy(id = doc.id)
        }
        emit(measurements)
    }.flowOn(Dispatchers.IO)

    suspend fun getAllMeasurements(): List<Measurement> {
        val snapshot = measurementsCollection.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Measurement::class.java)?.copy(id = doc.id)
        }
    }

    fun getTailors(): Flow<List<Tailor>> = flow {
        val snapshot = tailorsCollection.get().await()
        val tailors = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Tailor::class.java)?.copy(id = doc.id)
        }
        emit(tailors)
    }.flowOn(Dispatchers.IO)

    suspend fun addTailor(tailor: Tailor) {
        tailorsCollection.add(tailor).await()
    }

    suspend fun updateTailor(tailor: Tailor) {
        tailorsCollection.document(tailor.id).set(tailor).await()
    }

    suspend fun deleteTailor(tailorId: String) {
        tailorsCollection.document(tailorId).delete().await()
    }
} 