package com.example.tailorconnect.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Utility class for testing Firebase connectivity and rules
 */
object FirebaseTestUtil {
    private const val TAG = "FirebaseTestUtil"
    
    /**
     * Test Firebase database connection
     * @return true if connected, false otherwise
     */
    suspend fun testDatabaseConnection(): Boolean = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val database = FirebaseDatabase.getInstance()
            val connectedRef = database.getReference(".info/connected")
            
            // Flag to ensure we only resume the continuation once
            val hasResumed = AtomicBoolean(false)
            
            val listener = connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    Log.d(TAG, "Firebase connection: ${if (connected) "CONNECTED" else "DISCONNECTED"}")
                    
                    // Only resume if we haven't already
                    if (hasResumed.compareAndSet(false, true)) {
                        // If we're currently connected, resume with true
                        // Otherwise wait a bit to give it a chance to connect
                        if (connected) {
                            continuation.resume(true)
                            connectedRef.removeEventListener(this)
                        } else {
                            // If not connected, we'll wait for the next update
                            // but set a timeout to avoid waiting forever
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (hasResumed.compareAndSet(false, true)) {
                                    continuation.resume(false)
                                    connectedRef.removeEventListener(this)
                                }
                            }, 5000) // 5 second timeout
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase connection error: ${error.message}")
                    if (hasResumed.compareAndSet(false, true)) {
                        continuation.resume(false)
                        connectedRef.removeEventListener(this)
                    }
                }
            })
            
            continuation.invokeOnCancellation {
                connectedRef.removeEventListener(listener)
            }
        }
    }
    
    /**
     * Test write access to Firebase using a single value listener
     * @return A message describing the result
     */
    suspend fun testWriteAccess(): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                // First try to authenticate
                val auth = FirebaseAuth.getInstance()
                
                // If not logged in, try anonymous auth
                if (auth.currentUser == null) {
                    auth.signInAnonymously()
                        .addOnSuccessListener {
                            testDatabaseWrite(continuation)
                        }
                        .addOnFailureListener { e ->
                            if (!continuation.isCompleted) {
                                continuation.resumeWithException(Exception("Authentication failed: ${e.message}"))
                            }
                        }
                } else {
                    testDatabaseWrite(continuation)
                }
            } catch (e: Exception) {
                if (!continuation.isCompleted) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }
    
    private fun testDatabaseWrite(continuation: CancellableContinuation<String>) {
        if (continuation.isCompleted) return
        
        val database = FirebaseDatabase.getInstance().reference
        val testRef = database.child("test_write").push()
        
        testRef.setValue("test_value_${System.currentTimeMillis()}")
            .addOnSuccessListener {
                // Clean up after test
                testRef.removeValue()
                if (!continuation.isCompleted) {
                    continuation.resume("Write access test successful")
                }
            }
            .addOnFailureListener { e ->
                if (!continuation.isCompleted) {
                    continuation.resumeWithException(Exception("Write access denied: ${e.message}"))
                }
            }
    }
    
    /**
     * Provides detailed information about Firebase setup
     */
    fun getFirebaseSetupInfo(): String {
        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()
        
        return """
            Firebase Setup:
            - Authentication: ${if (auth.currentUser != null) "Signed in" else "Not signed in"}
            - User ID: ${auth.currentUser?.uid ?: "none"}
            - Database URL: ${database.reference.root}
            - Anonymous auth enabled: true
        """.trimIndent()
    }
} 