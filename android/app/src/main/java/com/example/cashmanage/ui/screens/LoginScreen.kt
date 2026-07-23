package com.example.cashmanage.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cashmanage.auth.GoogleAuthManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val authManager = remember { GoogleAuthManager(context) }
    
    LaunchedEffect(Unit) {
        if (authManager.getLastSignedInAccount() != null) {
            onLoginSuccess()
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val hasPermissions = GoogleSignIn.hasPermissions(
                    account,
                    com.google.android.gms.common.api.Scope(com.google.api.services.sheets.v4.SheetsScopes.SPREADSHEETS),
                    com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE)
                )
                if (hasPermissions) {
                    onLoginSuccess()
                } else {
                    errorMessage = "Izin Google Drive belum diberikan. Klik Reset Akun lalu Login lagi (centang kotak izin Google Drive)."
                }
            } catch (e: ApiException) {
                errorMessage = "Google Sign In Failed: ${e.statusCode}"
            }
        } else {
            errorMessage = "Sign In Cancelled"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AI Finance Bookkeeper", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                errorMessage = null
                launcher.launch(authManager.getSignInIntent())
            }
        ) {
            Text("Login with Google")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = {
                authManager.signOut {
                    authManager.revokeAccess {
                        errorMessage = "Akses telah di-reset. Silakan Login ulang."
                    }
                }
            }
        ) {
            Text("Reset Akun (Jika Error Sheets)", color = MaterialTheme.colorScheme.error)
        }
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
    }
}
