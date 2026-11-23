package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.SecurityImprovementRepository
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.configureAppCheck
import com.example.firebase.signOut
import com.example.model.AccountAccessState
import com.example.ui.HazardApp
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.HazardTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AppContent()
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var accessState by remember { mutableStateOf<AccountAccessState>(AccountAccessState.Checking) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    LaunchedEffect(user?.uid, user?.email) {
        if (user == null) {
            accessState = AccountAccessState.Checking
        } else {
            accessState = AccountAccessState.Checking
            val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
            val secRepo = SecurityImprovementRepository(db)
            val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            accessState = secRepo.verifyAndRecordSignIn(
                uid = user.uid,
                rawEmail = user.email,
                deviceLabel = deviceLabel
            )
        }
    }

    if (user == null) {
        HazardTheme(isWyattMode = false) {
            AuthScreen(
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        }
    } else {
        when (val state = accessState) {
            AccountAccessState.Checking -> {
                HazardTheme(isWyattMode = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Vérification de sécurité Google & App Check...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            is AccountAccessState.Denied -> {
                HazardTheme(isWyattMode = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier.widthIn(max = 480.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(22.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Accès refusé",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Accès refusé",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    if (state.email.isNotBlank()) {
                                        Text(
                                            text = "Compte : ${state.email}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Text(
                                        text = state.reason,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            signOut(credentialManager, scope) {
                                                currentUser = null
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("denied_sign_out_button")
                                    ) {
                                        Text("Changer de compte Google")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is AccountAccessState.Authorized -> {
                key(state.uid, state.lockedWyattMode) {
                    HazardApp(
                        currentUserId = state.uid,
                        isOwner = state.isOwner,
                        lockedWyattMode = state.lockedWyattMode,
                        onSignedOut = { currentUser = null }
                    )
                }
            }
        }
    }
}
