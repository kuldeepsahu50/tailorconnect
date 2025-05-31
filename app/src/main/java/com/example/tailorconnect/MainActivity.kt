package com.example.tailorconnect

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tailorconnect.data.model.repository.AppRepository
import com.example.tailorconnect.ui.screen.AdminDashboardScreen
import com.example.tailorconnect.ui.screen.LoginScreen
import com.example.tailorconnect.ui.screen.SignupScreen
import com.example.tailorconnect.ui.screen.TailorDashboardScreen
import com.example.tailorconnect.util.FirebaseTestUtil
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : ComponentActivity() {
    private val TAG = "TailorConnect"
    private lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            FirebaseApp.initializeApp(this)
            testFirebaseConnection()
            Log.i(TAG, FirebaseTestUtil.getFirebaseSetupInfo())
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error: ${e.message}")
        }

        repository = AppRepository()
        repository.setActivity(this)

        setContent {
            TailorConnectAppContent(repository)
        }
    }

    private fun testFirebaseConnection() {
        val database = FirebaseDatabase.getInstance()
        val connectedRef = database.getReference(".info/connected")

        connectedRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                Log.d(TAG, "Firebase connection: ${if (connected) "CONNECTED" else "DISCONNECTED"}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase connection error: ${error.message}")
            }
        })
    }
}

@Composable
fun TailorConnectAppContent(repository: AppRepository) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val isConnected = try {
                FirebaseTestUtil.testDatabaseConnection()
            } catch (e: Exception) {
                Log.e("TailorConnect", "Firebase connection test failed: ${e.message}")
                false
            }
            Log.d("TailorConnect", "Firebase connected: $isConnected")

            if (isConnected) {
                val writeAccessResult = try {
                    FirebaseTestUtil.testWriteAccess()
                } catch (e: Exception) {
                    "Write access failed: ${e.message}"
                }
                Log.d("TailorConnect", "Firebase write access: $writeAccessResult")
            }
        } catch (e: Exception) {
            Log.e("TailorConnect", "Firebase test error: ${e.message}")
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("signup") { SignupScreen(navController, repository) }
        composable("login") { LoginScreen(navController, repository) }

        composable(
            route = "admin_dashboard/{user_id}",
            arguments = listOf(
                navArgument("user_id") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("user_id") ?: ""
            Log.d("TailorConnect", "Navigating to admin_dashboard with userId: $userId")
            AdminDashboardScreen(repository, userId, navController)
        }

        composable(
            route = "tailor_dashboard/{tailorId}",
            arguments = listOf(
                navArgument("tailorId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tailorId = backStackEntry.arguments?.getString("tailorId") ?: ""
            TailorDashboardScreen(repository, tailorId, navController)
        }
    }
}