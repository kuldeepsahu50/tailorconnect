package com.example.tailorconnect.data.model.repository

import android.app.Activity
import android.util.Log
import com.example.tailorconnect.data.model.Measurement
import com.example.tailorconnect.data.model.User
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import java.util.concurrent.TimeUnit

class AppRepository {
    private val database = Firebase.database.reference
    private val auth = FirebaseAuth.getInstance()
    private var activity: Activity? = null
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var pendingUserInfo: Map<String, String>? = null

    fun setActivity(activity: Activity) {
        this.activity = activity
    }

    fun getActivity(): Activity {
        return activity ?: throw IllegalStateException("Activity not set")
    }

    fun getStoredVerificationId(): String? = storedVerificationId

    // Add cleanup method to prevent memory leaks
    fun cleanup() {
        try {
            storedVerificationId = null
            resendToken = null
            pendingUserInfo = null
            // Don't sign out here as it might affect other parts of the app
        } catch (e: Exception) {
            Log.e("AppRepository", "Error during cleanup: ${e.message}", e)
        }
    }

    suspend fun createTailor(name: String): User {
        try {
            // Create a new user in Firebase Auth with a random email and password
            val email = "${name.replace(" ", "").lowercase()}${System.currentTimeMillis()}@tailorconnect.com"
            val password = "tailor${System.currentTimeMillis()}"
            
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to create user")

            // Create user in Realtime Database
            val newUser = User(
                id = firebaseUser.uid,
                role = "Tailor",
                name = name,
                phone = "",
                email = email,
                username = "",
                password = password,
                uniqueCode = ""
            )
            
            database.child("users").child(firebaseUser.uid).setValue(newUser).await()
            return newUser
        } catch (e: Exception) {
            throw Exception("Failed to create tailor account: ${e.message}")
        }
    }

    suspend fun sendVerificationCode(phoneNumber: String, name: String, email: String): String = suspendCoroutine { continuation ->
        try {
            Log.d("AppRepository", "Sending verification code to: $phoneNumber")
            
            // Store user info for later use
            pendingUserInfo = mapOf(
                "name" to name,
                "email" to email
            )

            // Validate phone number format
            val formattedPhoneNumber = if (phoneNumber.startsWith("+91")) {
                phoneNumber
            } else {
                "+91$phoneNumber"
            }
            
            Log.d("AppRepository", "Formatted phone number: $formattedPhoneNumber")

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedPhoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS) // Increased timeout for better compatibility
                .setActivity(getActivity())
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        Log.d("AppRepository", "Auto-verification completed")
                        try {
                            // Try to sign in immediately if auto-verification succeeds
                            auth.signInWithCredential(credential)
                                .addOnSuccessListener {
                                    Log.d("AppRepository", "Auto-verification sign-in successful")
                                    storedVerificationId = "auto_verified"
                                    continuation.resume("auto_verified")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("AppRepository", "Auto-verification sign-in failed", exception)
                                    storedVerificationId = null
                                    pendingUserInfo = null
                                    continuation.resumeWithException(exception)
                                }
                        } catch (e: Exception) {
                            Log.e("AppRepository", "Auto-verification error", e)
                            continuation.resumeWithException(e)
                        }
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.e("AppRepository", "Verification failed", e)
                        storedVerificationId = null
                        pendingUserInfo = null
                        continuation.resumeWithException(e)
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        Log.d("AppRepository", "Verification code sent successfully")
                        storedVerificationId = verificationId
                        resendToken = token
                        continuation.resume(verificationId)
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            Log.e("AppRepository", "Error in sendVerificationCode", e)
            storedVerificationId = null
            pendingUserInfo = null
            continuation.resumeWithException(e)
        }
    }

    suspend fun verifyCode(code: String, role: String): User? {
        try {
            Log.d("AppRepository", "Starting verification process for role: $role")
            val verificationId = storedVerificationId ?: throw Exception("Verification ID is missing. Please request a new code.")
            
            if (code.isBlank() && verificationId != "auto_verified") {
                throw Exception("Verification code cannot be empty")
            }

            // Check if auto-verification was successful
            if (verificationId == "auto_verified") {
                Log.d("AppRepository", "Auto-verification detected")
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    Log.d("AppRepository", "Current user found: ${currentUser.uid}")
                    return try {
                        // Check if user exists in database
                        val userSnapshot = database.child("users")
                            .child(currentUser.uid)
                            .get()
                            .await()

                        val user = userSnapshot.getValue(User::class.java)

                        if (user == null) {
                            Log.d("AppRepository", "Creating new user for auto-verified account")
                            // Create new user if doesn't exist
                            val userInfo = pendingUserInfo ?: throw Exception("User information is missing")
                            val newUser = User(
                                id = currentUser.uid,
                                role = role,
                                phone = currentUser.phoneNumber ?: "",
                                name = userInfo["name"] ?: "",
                                email = userInfo["email"] ?: "",
                                username = "",
                                password = "",
                                uniqueCode = ""
                            )
                            database.child("users").child(currentUser.uid).setValue(newUser).await()
                            pendingUserInfo = null
                            Log.d("AppRepository", "New user created successfully")
                            newUser
                        } else {
                            // Verify role matches
                            if (user.role != role) {
                                Log.e("AppRepository", "Role mismatch: expected $role but got ${user.role}")
                                auth.signOut()
                                throw Exception("Invalid role selected")
                            }
                            Log.d("AppRepository", "Existing user verified successfully")
                            user
                        }
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Error processing auto-verified user", e)
                        throw e
                    }
                } else {
                    Log.e("AppRepository", "Auto-verification detected but no current user found")
                    throw Exception("Auto-verification failed. Please try manual verification.")
                }
            }

            Log.d("AppRepository", "Manual verification with code")
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Authentication failed")

            Log.d("AppRepository", "Firebase authentication successful for user: ${firebaseUser.uid}")
            
            return try {
                // Check if user exists in database
                val userSnapshot = database.child("users")
                    .child(firebaseUser.uid)
                    .get()
                    .await()

                val user = userSnapshot.getValue(User::class.java)

                if (user == null) {
                    Log.d("AppRepository", "Creating new user for manually verified account")
                    // Create new user if doesn't exist
                    val userInfo = pendingUserInfo ?: throw Exception("User information is missing")
                    val newUser = User(
                        id = firebaseUser.uid,
                        role = role,
                        phone = firebaseUser.phoneNumber ?: "",
                        name = userInfo["name"] ?: "",
                        email = userInfo["email"] ?: "",
                        username = "",
                        password = "",
                        uniqueCode = ""
                    )
                    database.child("users").child(firebaseUser.uid).setValue(newUser).await()
                    pendingUserInfo = null
                    Log.d("AppRepository", "New user created successfully")
                    newUser
                } else {
                    // Verify role matches
                    if (user.role != role) {
                        Log.e("AppRepository", "Role mismatch: expected $role but got ${user.role}")
                        auth.signOut()
                        throw Exception("Invalid role selected")
                    }
                    Log.d("AppRepository", "Existing user verified successfully")
                    user
                }
            } catch (e: Exception) {
                Log.e("AppRepository", "Error processing manually verified user", e)
                throw e
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Verification failed: ${e.message}")
            // Clean up on error
            storedVerificationId = null
            pendingUserInfo = null
            throw Exception("Verification failed: ${e.message ?: "Unknown error"}")
        }
    }

    // Fetch the valid admin code from Firebase
    suspend fun getValidAdminCode(): String = suspendCoroutine { continuation ->
        database.child("admin_codes").child("valid_code")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val code = snapshot.getValue(String::class.java) ?: ""
                    continuation.resume(code)
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
    }

    suspend fun signup(user: User, enteredCode: String? = null) {
        try {
            // If user is an admin, validate the code
            if (user.role == "Admin") {
                val validCode = getValidAdminCode()
                if (enteredCode != validCode) {
                    throw IllegalArgumentException("Invalid admin code")
                }
            }

            // Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(user.email, user.password).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to create user")

            // Create user in Realtime Database
            val newUser = user.copy(id = firebaseUser.uid)
            database.child("users").child(firebaseUser.uid).setValue(newUser).await()
        } catch (e: Exception) {
            throw Exception("Signup failed: ${e.message}")
        }
    }

    suspend fun login(username: String, password: String, enteredCode: String? = null): User? {
        try {
            // First, find the user by username to get their email
            val userSnapshot = database.child("users")
                .orderByChild("username")
                .equalTo(username)
                .get()
                .await()

            val user = userSnapshot.children
                .mapNotNull { it.getValue(User::class.java) }
                .firstOrNull()

            if (user == null) {
                return null
            }

            // Sign in with Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(user.email, password).await()
            val firebaseUser = authResult.user ?: return null

            // If user is admin, validate the code
            if (user.role == "Admin" && enteredCode != null) {
                val validCode = getValidAdminCode()
                if (enteredCode != validCode) {
                    return null
                }
            }

            return user
        } catch (e: Exception) {
            throw Exception("Login failed: ${e.message}")
        }
    }

    suspend fun getAllMeasurements(): List<Measurement> = suspendCoroutine { continuation ->
        Log.d("AppRepository", "Getting all measurements")
        database.child("measurements").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val measurements = mutableListOf<Measurement>()
                    for (measurementSnapshot in snapshot.children) {
                        try {
                            val map = measurementSnapshot.value as? Map<*, *>
                            if (map != null) {
                                val customerImageUrlsRaw = map["customerImageUrls"]
                                val customerImageUrls: List<String> = when (customerImageUrlsRaw) {
                                    is List<*> -> customerImageUrlsRaw.filterIsInstance<String>()
                                    is String -> if (customerImageUrlsRaw.isNotEmpty()) listOf(customerImageUrlsRaw) else emptyList()
                                    else -> emptyList()
                                }
                                val dimensionsRaw = map["dimensions"]
                                val dimensions: Map<String, String> = when (dimensionsRaw) {
                                    is Map<*, *> -> dimensionsRaw.mapNotNull { (k, v) ->
                                        if (k is String && v is String) k to v else null
                                    }.toMap()
                                    else -> emptyMap()
                                }
                                val measurement = Measurement(
                                    id = map["id"] as? String ?: "",
                                    customerName = map["customerName"] as? String ?: "",
                                    tailorId = map["tailorId"] as? String ?: "",
                                    adminId = map["adminId"] as? String ?: "",
                                    timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                    bodyTypeImageId = (map["bodyTypeImageId"] as? Number)?.toInt(),
                                    customerImageUrls = customerImageUrls,
                                    audioFileUrl = map["audioFileUrl"] as? String,
                                    dimensions = dimensions
                                )
                                measurements.add(measurement)
                            }
                        } catch (e: Exception) {
                            Log.e("AppRepository", "Error parsing measurement: ${e.message}")
                        }
                    }
                    continuation.resume(measurements)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }

    suspend fun getMeasurementsForTailor(tailorId: String): List<Measurement> = suspendCoroutine { continuation ->
        Log.d("AppRepository", "Getting measurements for tailor: $tailorId")
        database.child("measurements")
            .orderByChild("tailorId")
            .equalTo(tailorId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        Log.d("AppRepository", "Data snapshot received for tailor measurements")
                        Log.d("AppRepository", "Snapshot exists: ${snapshot.exists()}")
                        Log.d("AppRepository", "Snapshot children count: ${snapshot.childrenCount}")
                        
                        val measurements = mutableListOf<Measurement>()
                        
                        for (measurementSnapshot in snapshot.children) {
                            try {
                                Log.d("AppRepository", "Processing measurement: ${measurementSnapshot.key}")
                                val measurement = measurementSnapshot.getValue(Measurement::class.java)
                                if (measurement != null) {
                                    measurements.add(measurement)
                                    Log.d("AppRepository", "Added measurement: id=${measurement.id}, " +
                                        "customer=${measurement.customerName}, " +
                                        "tailorId=${measurement.tailorId}, " +
                                        "adminId=${measurement.adminId}")
                                } else {
                                    Log.e("AppRepository", "Failed to parse measurement: ${measurementSnapshot.key}")
                                }
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error parsing measurement: ${e.message}")
                            }
                        }
                        
                        Log.d("AppRepository", "Found ${measurements.size} measurements for tailor")
                        if (measurements.isEmpty()) {
                            Log.d("AppRepository", "No measurements found for tailorId: $tailorId")
                        }
                        continuation.resume(measurements)
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Error processing measurements", e)
                        continuation.resumeWithException(e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppRepository", "Database error: ${error.message}")
                    continuation.resumeWithException(error.toException())
                }
            })
    }

    suspend fun addMeasurement(measurement: Measurement) {
        try {
            Log.d("AppRepository", "Adding new measurement for tailor: ${measurement.tailorId}")
            val key = database.child("measurements").push().key ?: throw Exception("Failed to generate key")
            
            // Validate and clean data before saving
            val validatedMeasurement = measurement.copy(
                id = key,
                timestamp = System.currentTimeMillis(),
                customerImageUrls = measurement.customerImageUrls.filter { it.isNotEmpty() },
                dimensions = measurement.dimensions.filterValues { it.isNotEmpty() }
            )
            
            Log.d("AppRepository", "New measurement details: " +
                "id=${validatedMeasurement.id}, " +
                "customer=${validatedMeasurement.customerName}, " +
                "tailorId=${validatedMeasurement.tailorId}, " +
                "adminId=${validatedMeasurement.adminId}, " +
                "imageUrls=${validatedMeasurement.customerImageUrls.size}")
            
            database.child("measurements").child(key).setValue(validatedMeasurement).await()
            Log.d("AppRepository", "Successfully added measurement: ${validatedMeasurement.id}")
        } catch (e: Exception) {
            Log.e("AppRepository", "Error adding measurement", e)
            throw e
        }
    }

    suspend fun editMeasurement(measurement: Measurement) {
        database.child("measurements").child(measurement.id).setValue(measurement).await()
    }

    suspend fun deleteMeasurement(measurementId: String) {
        database.child("measurements").child(measurementId).removeValue().await()
    }

    suspend fun updateUser(user: User) {
        database.child("users").child(user.id).setValue(user).await()
    }

    suspend fun getUser(userId: String): User? = suspendCoroutine { continuation ->
        database.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    continuation.resume(user)
                }
                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun getAdminSubmittedMeasurements(tailorId: String): List<Measurement> = suspendCoroutine { continuation ->
        Log.d("AppRepository", "Getting admin submitted measurements for tailor: $tailorId")
        database.child("measurements")
            .orderByChild("tailorId")
            .equalTo(tailorId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        Log.d("AppRepository", "Data snapshot received for admin measurements")
                        val measurements = mutableListOf<Measurement>()
                        
                        for (measurementSnapshot in snapshot.children) {
                            try {
                                val measurement = measurementSnapshot.getValue(Measurement::class.java)
                                if (measurement != null && measurement.adminId.isNotEmpty()) {
                                    measurements.add(measurement)
                                    Log.d("AppRepository", "Added admin measurement: ${measurement.id}")
                                }
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error parsing measurement: ${e.message}")
                            }
                        }
                        
                        Log.d("AppRepository", "Found ${measurements.size} admin measurements")
                        continuation.resume(measurements)
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Error processing admin measurements", e)
                        continuation.resumeWithException(e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppRepository", "Database error: ${error.message}")
                    continuation.resumeWithException(error.toException())
                }
            })
    }

    suspend fun getAllUsers(): List<User> = suspendCoroutine { continuation ->
        Log.d("AppRepository", "Getting all users")
        database.child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        Log.d("AppRepository", "Data snapshot received for users")
                        val users = snapshot.children.mapNotNull { 
                            try {
                                it.getValue(User::class.java)
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error parsing user: ${e.message}")
                                null
                            }
                        }
                        Log.d("AppRepository", "Found ${users.size} users")
                        continuation.resume(users)
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Error processing users", e)
                        continuation.resumeWithException(e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppRepository", "Database error getting users: ${error.message}")
                    continuation.resumeWithException(error.toException())
                }
            })
    }

    suspend fun getAdminSubmittedMeasurements(): List<Measurement> = suspendCoroutine { continuation ->
        Log.d("AppRepository", "Getting all admin submitted measurements")
        database.child("measurements")
            .orderByChild("adminId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        Log.d("AppRepository", "Data snapshot received for admin measurements")
                        val measurements = mutableListOf<Measurement>()
                        
                        for (measurementSnapshot in snapshot.children) {
                            try {
                                val measurement = measurementSnapshot.getValue(Measurement::class.java)
                                if (measurement != null && measurement.adminId.isNotEmpty()) {
                                    measurements.add(measurement)
                                    Log.d("AppRepository", "Added admin measurement: id=${measurement.id}, customer=${measurement.customerName}")
                                }
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Error parsing measurement: ${e.message}")
                            }
                        }
                        
                        Log.d("AppRepository", "Found ${measurements.size} admin measurements")
                        continuation.resume(measurements)
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Error processing admin measurements", e)
                        continuation.resumeWithException(e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppRepository", "Database error: ${error.message}")
                    continuation.resumeWithException(error.toException())
                }
            })
    }
} 