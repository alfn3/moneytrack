package com.example.cashmanage.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cashmanage.util.OcrManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast
import com.example.cashmanage.ui.util.showCustomToast
import com.example.cashmanage.ui.util.ToastType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.core.content.FileProvider
import com.example.cashmanage.BuildConfig
import com.example.cashmanage.data.db.AppDatabase
import com.example.cashmanage.data.db.TransactionEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionDeclaration
import com.google.ai.client.generativeai.type.FunctionType
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.defineFunction
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val ocrManager = remember { OcrManager(context) }
    val scope = rememberCoroutineScope()
    
    var extractedText by remember { mutableStateOf("") }
    var processingStatus by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val tempUri = remember {
        val file = File(context.cacheDir, "camera_images/temp_ocr_image.jpg").apply {
            parentFile?.mkdirs()
        }
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            isProcessing = true
            processingStatus = "Mengekstrak teks dari struk..."
            scope.launch {
                val text = ocrManager.extractTextFromImage(tempUri)
                extractedText = text
                if (text.isNotBlank()) {
                    processingStatus = "Menganalisis teks dengan AI..."
                    processOcrWithGemini(context, text, onBack)
                } else {
                    isProcessing = false
                    context.showCustomToast("Tidak ada teks yang terdeteksi.", ToastType.INFO)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Struk / Invoice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Button(
                onClick = { cameraLauncher.launch(tempUri) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Text("Buka Kamera & Foto Struk")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (isProcessing) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(processingStatus)
            } else if (extractedText.isNotEmpty()) {
                Text("Teks Terdeteksi:", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(extractedText, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

private suspend fun processOcrWithGemini(context: android.content.Context, text: String, onBack: () -> Unit) {
    try {
        val recordTransactionFunc = defineFunction(
            name = "record_transaction",
            description = "Record an income or expense transaction to the financial database.",
            parameters = listOf(
                Schema(
                    name = "amount",
                    description = "The total nominal amount found in the receipt or invoice",
                    type = FunctionType.NUMBER
                ),
                Schema(
                    name = "category_id",
                    description = "The category ID (1: Food, 2: Transport, 3: Salary, 4: Entertainment, 5: Others). Guess based on the items in the receipt.",
                    type = FunctionType.INTEGER
                ),
                Schema(
                    name = "notes",
                    description = "Notes or description of the transaction based on the receipt items or merchant name",
                    type = FunctionType.STRING
                )
            )
        )

        val systemPrompt = "Anda adalah AI akuntan. Ekstrak data transaksi dari teks hasil OCR struk/invoice berikut. Temukan total tagihan (amount), tebak kategori yang paling relevan (category_id), dan buat catatan singkat (notes) seperti nama toko atau rincian item utama. Setelah itu panggil fungsi record_transaction."

        val dynamicModel = GenerativeModel(
            modelName = "gemini-3.5-flash-lite",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content { text(systemPrompt) },
            tools = listOf(Tool(listOf(recordTransactionFunc)))
        )

        val response = dynamicModel.generateContent(text)
        val functionCall = response.functionCall

        if (functionCall != null && functionCall.name == "record_transaction") {
            val amount = functionCall.args["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
            val categoryId = functionCall.args["category_id"]?.toString()?.toDoubleOrNull()?.toInt() ?: 1
            val notes = functionCall.args["notes"]?.toString() ?: "Hasil scan OCR"
            val accountId = 1 // default Rekening Utama
            val txType = "EXPENSE" // receipts are usually expenses

            withContext(Dispatchers.IO) {
                val database = AppDatabase.getDatabase(context)
                
                val accounts = database.accountDao().getAllAccountsList()
                val categories = database.categoryDao().getAllCategoriesList()
                
                var validAccountId = accountId
                if (accounts.isEmpty()) {
                    validAccountId = database.accountDao().insertAccount(
                        com.example.cashmanage.data.db.AccountEntity(name = "Rekening Utama", balance = 0.0, userId = "local")
                    ).toInt()
                } else if (accounts.none { it.id == validAccountId }) {
                    validAccountId = accounts.first().id
                }
                
                var validCategoryId = categoryId
                if (categories.isEmpty()) {
                    validCategoryId = database.categoryDao().insertCategory(
                        com.example.cashmanage.data.db.CategoryEntity(name = "Lainnya", type = "EXPENSE", icon = null)
                    ).toInt()
                } else if (categories.none { it.id == validCategoryId }) {
                    validCategoryId = categories.first().id
                }

                database.transactionDao().insertTransaction(
                    TransactionEntity(
                        accountId = validAccountId,
                        categoryId = validCategoryId,
                        amount = amount,
                        type = txType,
                        notes = notes,
                        date = System.currentTimeMillis()
                    )
                )
                
                com.example.cashmanage.util.BudgetAlertHelper.checkBudgetLimit(context, amount, categoryId, txType)

                // Save to AI History
                val formatRp = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).apply { maximumFractionDigits = 0 }
                val amountFormatted = formatRp.format(amount).replace("Rp", "Rp ")
                val responseMsg = "Berhasil memproses foto struk: Mencatat transaksi pengeluaran sebesar $amountFormatted dengan catatan '$notes'"
                database.aiHistoryDao().insertHistory(
                    com.example.cashmanage.data.db.AIHistoryEntity(
                        prompt = "[Menganalisis Foto Struk OCR]",
                        response = responseMsg,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            
            withContext(Dispatchers.Main) {
                val formatRp = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).apply { maximumFractionDigits = 0 }
                context.showCustomToast("Berhasil mencatat transaksi ${formatRp.format(amount)}: $notes", ToastType.SUCCESS)
                onBack()
            }
        } else {
            withContext(Dispatchers.Main) {
                context.showCustomToast("AI gagal mengekstrak data dari struk ini.", ToastType.ERROR)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            context.showCustomToast("Terjadi kesalahan: ${e.message}", ToastType.ERROR)
        }
    }
}
