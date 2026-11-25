package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.MoneyFlowRepository
import com.example.data.authStateFlow
import com.example.data.signOutUser
import com.example.ui.AuthScreen
import com.example.ui.MoneyFlowHomeScreen
import com.example.ui.MoneyFlowViewModel
import com.example.ui.theme.MoneyFlowTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            MoneyFlowTheme {
                MoneyFlowAppRoot()
            }
        }
    }
}

@Composable
private fun MoneyFlowAppRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener drives transition automatically */ })
    } else {
        val uid = user.uid
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val credentialManager = remember(context) { CredentialManager.create(context) }

        // Key composition and ViewModel by uid so sign-out or account switch cleanly tears down listeners
        key(uid) {
            val viewModel: MoneyFlowViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val projectId = runCatching {
                            FirebaseApp.getInstance().options.projectId
                        }.getOrNull().orEmpty().ifBlank { "Configured via google-services.json" }
                        val db = FirebaseFirestore.getInstance(databaseId)
                        MoneyFlowViewModel(
                            repository = MoneyFlowRepository(db = db, auth = auth),
                            currentUserId = uid,
                            userEmail = user.email,
                            userDisplayName = user.displayName,
                            firebaseProjectId = projectId,
                            firestoreDatabaseId = databaseId
                        )
                    }
                }
            )

            MoneyFlowHomeScreen(
                viewModel = viewModel,
                onSignOut = {
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}
