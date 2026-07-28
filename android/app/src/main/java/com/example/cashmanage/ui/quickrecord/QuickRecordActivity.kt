package com.example.cashmanage.ui.quickrecord

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import com.example.cashmanage.ui.util.ToastType
import com.example.cashmanage.ui.util.showCustomToast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cashmanage.BuildConfig
import com.example.cashmanage.R
import com.example.cashmanage.data.db.AppDatabase
import com.example.cashmanage.data.db.TransactionEntity
import com.example.cashmanage.util.BudgetAlertHelper
import com.example.cashmanage.worker.SyncWorker
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionType
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.defineFunction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QuickRecordActivity : ComponentActivity() {

    private val SPEECH_REQUEST_CODE = 100
    private var composeToastMessage = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // No compose UI, Activity is transparent
        
        // Start listening immediately
        showProcessingToast("Memproses...")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Sebutkan transaksi anda")
        }
        
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            this.showCustomToast("Voice recognition not available", ToastType.ERROR)
            finish()
        }
    }

    private fun showProcessingToast(msg: String) {
        this.showCustomToast(msg, ToastType.INFO)
    }
    
    private fun showVnResultNotification(title: String, message: String) {
        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val intent = android.content.Intent(this, com.example.cashmanage.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: android.app.PendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE)

        val builder = androidx.core.app.NotificationCompat.Builder(this, "quick_record_channel")
            .setSmallIcon(com.example.cashmanage.R.drawable.ic_justsayit_logo_small)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(android.graphics.Color.parseColor("#FFBD59"))

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = results?.get(0)
                if (!spokenText.isNullOrBlank()) {
                    showProcessingToast("Memproses: $spokenText")
                    processWithGemini(spokenText)
                } else {
                    finish()
                }
            } else {
                finish()
            }
        }
    }

    private fun processWithGemini(text: String) {
        lifecycleScope.launch {
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
                            name = "account_id",
                            description = "The account ID (1: Cash, 2: Bank, 3: E-Wallet)",
                            type = FunctionType.INTEGER
                        ),
                        Schema(
                            name = "type",
                            description = "Transaction type, must be exactly 'INCOME' or 'EXPENSE'",
                            type = FunctionType.STRING
                        ),
                        Schema(
                            name = "notes",
                            description = "Additional notes or context",
                            type = FunctionType.STRING
                        )
                    )
                )

                val aiLearningManager = com.example.cashmanage.ai.AILearningManager(applicationContext)
                val learningRules = aiLearningManager.getLearningRules()
                val systemPrompt = "Anda adalah asisten keuangan pribadi yang ramah, pintar, dan sangat teliti. Jika pengguna menyebutkan pengeluaran atau pemasukan dengan nominal, panggillah fungsi record_transaction.$learningRules"

                val dynamicModel = GenerativeModel(
                    modelName = "gemini-3.5-flash-lite",
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    systemInstruction = content { text(systemPrompt) },
                    tools = listOf(Tool(listOf(recordTransactionFunc)))
                )

                val response = dynamicModel.generateContent(text)
                val functionCall = response.functionCall

                if (functionCall != null && functionCall.name == "record_transaction") {
                    val amountStr = functionCall.args["amount"]?.toString()?.replace(Regex("[^0-9]"), "")
                    val amount = amountStr?.toDoubleOrNull() ?: 0.0
                    val categoryIdStr = functionCall.args["category_id"]?.toString()?.replace(Regex("[^0-9]"), "")
                    val categoryId = categoryIdStr?.toIntOrNull() ?: 1
                    val accountIdStr = functionCall.args["account_id"]?.toString()?.replace(Regex("[^0-9]"), "")
                    val accountId = accountIdStr?.toIntOrNull() ?: 1
                    val txType = functionCall.args["type"]?.toString()?.uppercase() ?: "EXPENSE"
                    val notes = functionCall.args["notes"]?.toString() ?: ""

                    val database = AppDatabase.getDatabase(applicationContext)
                    val categories = database.categoryDao().getAllCategoriesList()
                    val accounts = database.accountDao().getAllAccountsList()

                    var validAccountId = accountId
                    if (accounts.isNotEmpty() && accounts.none { it.id == validAccountId }) {
                        validAccountId = accounts.first().id
                    }

                    var validCategoryId = categoryId
                    if (categories.isNotEmpty() && categories.none { it.id == validCategoryId }) {
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

                    val catName = categories.find { it.id == validCategoryId }?.name ?: "Lainnya"
                    val accName = accounts.find { it.id == validAccountId }?.name ?: "Tunai"
                    
                    val formatRp = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).apply { maximumFractionDigits = 0 }
                    val amountFormatted = formatRp.format(amount).replace("Rp", "Rp ")
                    val typeStr = if (txType == "INCOME") "pemasukan" else "pengeluaran"
                    val flatResponse = "berhasil mencatat transaksi $typeStr $catName $notes sebesar $amountFormatted via $accName ke database"
                    WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                        "SyncTransactionsWork",
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequestBuilder<SyncWorker>().build()
                    )
                    
                    val history = com.example.cashmanage.data.db.AIHistoryEntity(
                        prompt = text,
                        response = flatResponse,
                        timestamp = System.currentTimeMillis()
                    )
                    database.aiHistoryDao().insertHistory(history)
                    
                    BudgetAlertHelper.checkBudgetLimit(applicationContext, amount, categoryId, txType)
                    showVnResultNotification("Berhasil Mencatat", "$typeStr $catName $notes $amountFormatted via $accName")
                } else {
                    showProcessingToast("Gagal menangkap nominal dari ucapan.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errMsg = e.message ?: "Unknown error"
                if (errMsg.contains("quota", ignoreCase = true) || errMsg.contains("ResourceExhausted", ignoreCase = true)) {
                    showProcessingToast("Kuota harian tercapai. Silakan coba lagi besok.")
                } else {
                    showProcessingToast("Error: $errMsg")
                }
            } finally {
                finish()
            }
        }
    }
}
