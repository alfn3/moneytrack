package com.example.cashmanage.ui.quickrecord

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cashmanage.worker.SyncWorker

class QuickRecordActivity : ComponentActivity() {

    private val SPEECH_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start listening immediately
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Sebutkan pengeluaran Anda...")
        }
        
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = results?.get(0)
                if (!spokenText.isNullOrBlank()) {
                    Toast.makeText(this, "Memproses: $spokenText", Toast.LENGTH_SHORT).show()
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
                    systemInstruction = content { text("Anda adalah asisten pencatatan keuangan. Jika pengguna menyebutkan pengeluaran atau pemasukan dengan nominal, panggillah fungsi record_transaction.") },
                    tools = listOf(Tool(listOf(recordTransactionFunc)))
                )

                val response = dynamicModel.generateContent(text)
                val functionCall = response.functionCall

                if (functionCall != null && functionCall.name == "record_transaction") {
                    val amount = functionCall.args["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
                    val categoryId = functionCall.args["category_id"]?.toString()?.toDoubleOrNull()?.toInt() ?: 1
                    val txType = functionCall.args["type"]?.toString()?.uppercase() ?: "EXPENSE"
                    val notes = functionCall.args["notes"]?.toString() ?: ""

                    val database = AppDatabase.getDatabase(applicationContext)
                    database.transactionDao().insertTransaction(
                        TransactionEntity(
                            accountId = 1,
                            categoryId = categoryId,
                            amount = amount,
                            type = txType,
                            notes = notes,
                            date = System.currentTimeMillis()
                        )
                    )

                    androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                        "SyncTransactionsWork",
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        androidx.work.OneTimeWorkRequestBuilder<SyncWorker>().build()
                    )
                    
                    Toast.makeText(this@QuickRecordActivity, "Berhasil mencatat Rp $amount!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@QuickRecordActivity, "Gagal menangkap nominal dari ucapan.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@QuickRecordActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                finish()
            }
        }
    }
}
