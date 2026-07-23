package com.example.cashmanage.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cashmanage.util.OcrManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast
import com.example.cashmanage.data.db.AppDatabase
import com.example.cashmanage.data.db.TransactionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val ocrManager = remember { OcrManager(context) }
    val scope = rememberCoroutineScope()
    
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var extractedText by remember { mutableStateOf("") }
    var parsedAmount by remember { mutableStateOf<Double?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            isProcessing = true
            scope.launch {
                extractedText = ocrManager.extractTextFromImage(it)
                parsedAmount = ocrManager.parseAmount(extractedText)
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Struk / Invoice") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Button(onClick = { launcher.launch("image/*") }) {
                Text("Pilih Gambar dari Galeri")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isProcessing) {
                CircularProgressIndicator()
                Text("Memproses gambar...")
            } else {
                if (extractedText.isNotEmpty()) {
                    Text("Teks Terdeteksi:", style = MaterialTheme.typography.titleMedium)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Text(extractedText, modifier = Modifier.padding(8.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (parsedAmount != null) {
                        Text(
                            "Total Terbaca: Rp $parsedAmount",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            if (parsedAmount != null) {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            val db = AppDatabase.getDatabase(context)
                                            db.transactionDao().insertTransaction(
                                                TransactionEntity(
                                                    accountId = 1,
                                                    categoryId = 1,
                                                    amount = parsedAmount!!,
                                                    date = System.currentTimeMillis(),
                                                    notes = "Dari hasil Scan Struk OCR",
                                                    type = "EXPENSE",
                                                    isSynced = false
                                                )
                                            )
                                        }
                                        Toast.makeText(context, "Transaksi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal menyimpan transaksi: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }) {
                            Text("Simpan sebagai Transaksi")
                        }
                    }
                }
            }
        }
    }
}
