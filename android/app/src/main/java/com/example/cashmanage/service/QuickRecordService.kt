package com.example.cashmanage.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
// Removed incorrect Compose and GoogleSignIn imports; not needed in Service
import androidx.core.app.NotificationCompat
import com.example.cashmanage.BuildConfig
import com.example.cashmanage.data.db.AppDatabase
import com.example.cashmanage.data.db.TransactionEntity
import com.example.cashmanage.ui.quickrecord.QuickRecordActivity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionType
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.defineFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.cashmanage.ui.util.showCustomToast
import com.example.cashmanage.ui.util.ToastType

class QuickRecordService : Service() {

    private val CHANNEL_ID = "quick_record_channel"
    private val NOTIFICATION_ID = 101

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "PROCESS_OCR") {
            val uriStr = intent.getStringExtra("photo_uri")
            if (uriStr != null) {
                processOcrInBackground(Uri.parse(uriStr))
            }
            return START_STICKY
        }

        val vnIntent = Intent(this, QuickRecordActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val vnPendingIntent = PendingIntent.getActivity(
            this, 1, vnIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val photoIntent = Intent(this, com.example.cashmanage.ui.quickrecord.OcrCameraActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val photoPendingIntent = PendingIntent.getActivity(
            this, 2, photoIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("justsayit - siap mendengarkan")
            .setContentText("bilang aja, transaksi harianmu akan kucatat rapi")
            .setSmallIcon(com.example.cashmanage.R.drawable.ic_justsayit_logo_small)
            .setColor(android.graphics.Color.parseColor("#FFBD59"))
            .setContentIntent(vnPendingIntent)
            .addAction(android.R.drawable.ic_btn_speak_now, "Voice Note", vnPendingIntent)
            .addAction(android.R.drawable.ic_menu_camera, "Foto Struk", photoPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Quick Record Service",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Persistent notification for quick voice recording"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun processOcrInBackground(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val aiLearningManager = com.example.cashmanage.ai.AILearningManager(applicationContext)
                val learningRules = aiLearningManager.getLearningRules()
                
                val systemPrompt = "Anda adalah asisten keuangan pribadi. Ekstrak total belanja (amount), ID kategori (category_id), tipe (type), dan catatan/nama barang (notes) dari foto struk belanja ini. $learningRules"
                
                val recordTransactionFunc = defineFunction(
                    name = "record_transaction",
                    description = "Mencatat transaksi dari struk",
                    parameters = listOf(
                        Schema(
                            name = "amount",
                            description = "Total jumlah transaksi",
                            type = FunctionType.NUMBER
                        ),
                        Schema(
                            name = "category_id",
                            description = "ID Kategori",
                            type = FunctionType.INTEGER
                        ),
                        Schema(
                            name = "type",
                            description = "Tipe transaksi: INCOME atau EXPENSE",
                            type = FunctionType.STRING
                        ),
                        Schema(
                            name = "notes",
                            description = "Catatan transaksi",
                            type = FunctionType.STRING
                        )
                    )
                )

                val dynamicModel = GenerativeModel(
                    modelName = "gemini-3.5-flash-lite",
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    systemInstruction = content { text(systemPrompt) },
                    tools = listOf(Tool(listOf(recordTransactionFunc)))
                )

                val inputContent = content {
                    image(bitmap)
                    text("Ekstrak data dari struk ini.")
                }

                val response = dynamicModel.generateContent(inputContent)
                val functionCall = response.functionCall

                if (functionCall != null && functionCall.name == "record_transaction") {
                    // Parse arguments
                    val amountStr = functionCall.args["amount"]?.toString()?.replace(Regex("[^0-9]"), "")
                    val amount = amountStr?.toDoubleOrNull() ?: 0.0
                    val categoryIdStr = functionCall.args["category_id"]?.toString()?.replace(Regex("[^0-9]"), "")
                    val categoryId = categoryIdStr?.toIntOrNull() ?: 1
                    val notes = functionCall.args["notes"]?.toString() ?: "Hasil scan OCR"
                    val accountId = 1
                    val txType = "EXPENSE"

                    val database = AppDatabase.getDatabase(applicationContext)
                    val accounts = database.accountDao().getAllAccountsList()
                    val categories = database.categoryDao().getAllCategoriesList()
                    
                    var validAccountId = accountId
                    if (accounts.isNotEmpty() && accounts.none { it.id == validAccountId }) {
                        validAccountId = accounts.first().id
                    }
                    
                    var validCategoryId = categoryId
                    if (categories.isNotEmpty() && categories.none { it.id == validCategoryId }) {
                        validCategoryId = categories.first().id
                    }

                    // Insert transaction
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

                    // Build formatted subtitle
                    val formatRp = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).apply { maximumFractionDigits = 0 }
                    val amountFormatted = formatRp.format(amount).replace("Rp", "Rp ")
                    val typeStr = if (txType == "INCOME") "pemasukan" else "pengeluaran"
                    val catName = categories.find { it.id == validCategoryId }?.name ?: "Lainnya"
                    val accName = accounts.find { it.id == validAccountId }?.name ?: "Tunai"
                    val subtitle = "$typeStr $catName $notes $amountFormatted via $accName"
                    
                    // Check budget limit
                    com.example.cashmanage.util.BudgetAlertHelper.checkBudgetLimit(applicationContext, amount, categoryId, txType)
                    
                    withContext(Dispatchers.Main) {
                        showOcrResultNotification("Berhasil Mencatat", subtitle)
                        showCustomToast("Transaksi berhasil dicatat", ToastType.SUCCESS)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showOcrResultNotification("Gagal", "AI gagal mengekstrak data dari struk.")
                        showCustomToast("AI gagal mengekstrak data dari struk.", ToastType.ERROR)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = e.message ?: ""
                // Check for quota related errors
                if (errorMsg.contains("quota", ignoreCase = true) || errorMsg.contains("ResourceExhausted", ignoreCase = true)) {
                    withContext(Dispatchers.Main) {
                        showOcrResultNotification(
                            "Quota Limit",
                            "Kuota harian tercapai. Silakan coba lagi nanti."
                        )
                        showCustomToast(
                            "Kuota harian tercapai",
                            ToastType.ERROR
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showOcrResultNotification(
                            "Error",
                            "Error memproses struk: $errorMsg"
                        )
                        showCustomToast(
                            "Error memproses struk: $errorMsg",
                            ToastType.ERROR
                        )
                    }
                }
            }

        }
    }
    private fun showOcrResultNotification(title: String, message: String) {
    val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val intent = android.content.Intent(this, com.example.cashmanage.MainActivity::class.java).apply {
        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE)
    val builder = androidx.core.app.NotificationCompat.Builder(this, "quick_record_channel")
        .setSmallIcon(com.example.cashmanage.R.drawable.ic_justsayit_logo_small)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setColor(android.graphics.Color.parseColor("#FFBD59"))
    notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
}



}
