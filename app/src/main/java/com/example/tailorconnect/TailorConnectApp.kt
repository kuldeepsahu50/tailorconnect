package com.example.tailorconnect

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class TailorConnectApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Enable Firebase offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
} 