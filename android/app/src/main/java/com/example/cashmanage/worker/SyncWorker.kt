package com.example.cashmanage.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cashmanage.data.db.AppDatabase

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val transactionDao = database.transactionDao()
        
        try {
            val prefs = applicationContext.getSharedPreferences("cashmanage_prefs", Context.MODE_PRIVATE)
            var spreadsheetId = prefs.getString("spreadsheet_id", null)
            
            val allTransactions = transactionDao.getAllTransactionsList()
            
            // Check if user is signed in
            val authManager = com.example.cashmanage.auth.GoogleAuthManager(applicationContext)
            val account = authManager.getLastSignedInAccount()
            if (account == null) {
                // Not signed in, cannot sync to Google Sheets
                return Result.failure()
            }
            
            val sheetsService = com.example.cashmanage.data.api.GoogleSheetsService(applicationContext, account)
            
            if (spreadsheetId == null) {
                spreadsheetId = sheetsService.createSpreadsheet("Cash Manage Transactions")
                prefs.edit().putString("spreadsheet_id", spreadsheetId).apply()
            }
            
            if (spreadsheetId != null) {
                // Overwrite all transactions to Google Sheets to sync edits and deletions
                sheetsService.overwriteTransactions(spreadsheetId, allTransactions)
                
                // Mark all as synced
                val updated = allTransactions.map { it.copy(isSynced = true) }
                transactionDao.updateTransactions(updated)
            }
            
            return Result.success()
        } catch (e: Throwable) {
            e.printStackTrace()
            // Munculkan notifikasi lokal
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channel = android.app.NotificationChannel("sync_error", "Sync Error", android.app.NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
            val notification = android.app.Notification.Builder(applicationContext, "sync_error")
                .setContentTitle("Gagal Sync ke Spreadsheet")
                .setContentText(e.toString() + " " + (e.message ?: "Unknown error"))
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .build()
            notificationManager.notify(999, notification)
            return Result.retry()
        }
    }
}
