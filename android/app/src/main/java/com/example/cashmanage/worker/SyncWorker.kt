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
            val allBudgets = database.budgetDao().getAllBudgetsList()
            val allGoals = database.savingGoalDao().getAllSavingGoalsList()
            val allCategories = database.categoryDao().getAllCategoriesList()
            val allAccounts = database.accountDao().getAllAccountsList()
            
            // Check if user is signed in
            val authManager = com.example.cashmanage.auth.GoogleAuthManager(applicationContext)
            val account = authManager.getLastSignedInAccount()
            if (account == null) {
                // Not signed in, cannot sync to Google Sheets
                return Result.failure()
            }
            
            val sheetsService = com.example.cashmanage.data.api.GoogleSheetsService(applicationContext, account)
            
            if (spreadsheetId == null) {
                spreadsheetId = sheetsService.createSpreadsheet("JustSayIt. Transactions")
                prefs.edit().putString("spreadsheet_id", spreadsheetId).apply()
            }
            
            if (spreadsheetId != null) {
                // Sync all data to Google Sheets
                val finalBudgets = database.budgetDao().getAllBudgetsList()

                // 3. Sync all data to Google Sheets
                sheetsService.syncAllData(spreadsheetId, allTransactions, finalBudgets, allGoals, allCategories, allAccounts)
                
                // Mark all as synced (if needed)
                val updated = allTransactions.map { it.copy(isSynced = true) }
                transactionDao.updateTransactions(updated)
                
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    com.example.cashmanage.util.UIUtils.showCustomToast(applicationContext, "Sinkronisasi Spreadsheet berhasil")
                }
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
                .setSmallIcon(com.example.cashmanage.R.drawable.ic_justsayit_logo)
                .setColor(android.graphics.Color.parseColor("#FFBD59"))
                .build()
            notificationManager.notify(999, notification)
            return Result.retry()
        }
    }
}
