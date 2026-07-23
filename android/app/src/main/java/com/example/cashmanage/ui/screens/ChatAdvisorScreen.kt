package com.example.cashmanage.ui.screens

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cashmanage.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import java.io.InputStream
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashmanage.ui.viewmodel.FinancialViewModel
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.FunctionType
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.defineFunction
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import com.example.cashmanage.worker.SyncWorker
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAdvisorScreen(
    onBack: () -> Unit,
    viewModel: FinancialViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    
    // UI State for Chat
    data class ChatMessage(val text: String, val isUser: Boolean, val imageUri: Uri? = null, val bitmap: Bitmap? = null)
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var isTyping by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Removed GenerativeModel initialization from here to inside the click listener

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                selectedBitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Speech Recognizer
    var isListening by remember { mutableStateOf(false) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    
    // Permission launcher for Microphone
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            }
            speechRecognizer.startListening(intent)
            isListening = true
        } else {
            Toast.makeText(context, "Izin mikrofon diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                Toast.makeText(context, "Gagal mendengar, coba lagi.", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    input = matches[0] // Set transcribed text to input field
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose {
            speechRecognizer.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant") },
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = true
            ) {
                if (isTyping) {
                    item { Text("AI sedang berpikir...", modifier = Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(messages.reversed()) { msg ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                msg.bitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Attached Image",
                                        modifier = Modifier
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .padding(bottom = 8.dp)
                                    )
                                }
                                Text(msg.text)
                            }
                        }
                    }
                }
            }
            
            // Selected Image Preview
            selectedBitmap?.let { bmp ->
                Box(modifier = Modifier.padding(8.dp)) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Preview",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    IconButton(
                        onClick = { 
                            selectedImageUri = null
                            selectedBitmap = null 
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text("X", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(Icons.Filled.Image, contentDescription = "Pilih Gambar")
                }
                
                IconButton(
                    onClick = {
                        if (isListening) {
                            speechRecognizer.stopListening()
                            isListening = false
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.Mic, 
                        contentDescription = "Voice Note",
                        tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (isListening) "Mendengarkan..." else "Tanya ke AI...") },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                
                IconButton(
                    onClick = {
                        if (input.isNotBlank() || selectedBitmap != null) {
                            val userMsg = input
                            val userBmp = selectedBitmap
                            messages.add(ChatMessage(userMsg, true, selectedImageUri, userBmp))
                            input = ""
                            selectedImageUri = null
                            selectedBitmap = null
                            isTyping = true
                            
                            scope.launch {
                                try {
                                    val recordTransactionFunc = defineFunction(
                                        name = "record_transaction",
                                        description = "Record an income or expense transaction to the financial database.",
                                        parameters = listOf(
                                            Schema(
                                                name = "amount",
                                                description = "The nominal amount of the transaction",
                                                type = FunctionType.NUMBER
                                            ),
                                            Schema(
                                                name = "category_id",
                                                description = "The category ID (1: Food, 2: Transport, 3: Salary, 4: Entertainment, 5: Others)",
                                                type = FunctionType.INTEGER
                                            ),
                                            Schema(
                                                name = "type",
                                                description = "Either 'INCOME' or 'EXPENSE'",
                                                type = FunctionType.STRING
                                            ),
                                            Schema(
                                                name = "notes",
                                                description = "Notes or description of the transaction",
                                                type = FunctionType.STRING
                                            )
                                        )
                                    )

                                    val dynamicModel = GenerativeModel(
                                        modelName = "gemini-3.5-flash-lite",
                                        apiKey = BuildConfig.GEMINI_API_KEY,
                                        systemInstruction = content { text("Anda adalah asisten keuangan pribadi yang ahli. Tugas utama Anda adalah membantu pengguna mencatat keuangan (pemasukan dan pengeluaran), memberikan analisis tren keuangan, serta saran finansial yang cerdas dan praktis. Jawablah dengan ramah dan profesional. Tolak dengan sopan jika pengguna bertanya di luar topik keuangan atau manajemen kas. Jika pengguna menyebutkan pengeluaran atau pemasukan dengan nominal, panggillah fungsi record_transaction.") },
                                        tools = listOf(Tool(listOf(recordTransactionFunc)))
                                    )
                                    
                                    val response = if (userBmp != null) {
                                        dynamicModel.generateContent(content {
                                            image(userBmp)
                                            if (userMsg.isNotBlank()) text(userMsg) else text("Jelaskan gambar ini.")
                                        })
                                    } else {
                                        dynamicModel.generateContent(userMsg)
                                    }
                                    
                                    val functionCall = response.functionCall
                                    if (functionCall != null && functionCall.name == "record_transaction") {
                                        val amount = functionCall.args["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
                                        val categoryId = functionCall.args["category_id"]?.toString()?.toDoubleOrNull()?.toInt() ?: 1
                                        val type = functionCall.args["type"]?.toString()?.uppercase() ?: "EXPENSE"
                                        val notes = functionCall.args["notes"]?.toString() ?: ""
                                        
                                        viewModel.addTransaction(accountId = 1, categoryId, amount, type, notes)
                                        
                                        // Trigger auto sync
                                        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                                            "SyncTransactionsWork",
                                            androidx.work.ExistingWorkPolicy.REPLACE,
                                            androidx.work.OneTimeWorkRequestBuilder<SyncWorker>().build()
                                        )
                                            
                                        messages.add(ChatMessage("Berhasil mencatat transaksi $type sebesar Rp $amount ke database dan melakukan auto-sync ke Spreadsheet!", false))
                                    } else {
                                        messages.add(ChatMessage(response.text ?: "No response", false))
                                    }
                                } catch (e: Exception) {
                                    messages.add(ChatMessage("Error: ${e.message}", false))
                                } finally {
                                    isTyping = false
                                }
                            }
                        }
                    },
                    enabled = !isTyping && (input.isNotBlank() || selectedBitmap != null)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Kirim", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
