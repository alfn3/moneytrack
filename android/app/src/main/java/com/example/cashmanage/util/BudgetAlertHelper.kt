package com.example.cashmanage.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.cashmanage.R
import com.example.cashmanage.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object BudgetAlertHelper {
    private const val CHANNEL_ID = "budget_alerts"
    private const val ALERT_THRESHOLD = 0.9 // 90%

    fun checkBudgetLimit(context: Context, newAmount: Double, categoryId: Int, txType: String) {
        if (txType != "EXPENSE") return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val cal = Calendar.getInstance()
                val currentYear = cal.get(Calendar.YEAR)
                val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-12

                // Get budget for this category
                val budgets = db.budgetDao().getAllBudgetsList()
                val activeBudget = budgets.find { it.categoryId == categoryId && it.year == currentYear && it.month == currentMonth }

                if (activeBudget != null && activeBudget.limitAmount > 0) {
                    val allTx = db.transactionDao().getAllTransactionsList()
                    val spentThisMonth = allTx.filter {
                        if (it.type != "EXPENSE" || it.categoryId != categoryId) return@filter false
                        val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
                        txCal.get(Calendar.YEAR) == currentYear && (txCal.get(Calendar.MONTH) + 1) == currentMonth
                    }.sumOf { it.amount }

                    val progress = spentThisMonth / activeBudget.limitAmount
                    
                    if (progress >= ALERT_THRESHOLD) {
                        val category = db.categoryDao().getAllCategoriesList().find { it.id == categoryId }
                        val catName = category?.name ?: "Kategori"
                        
                        val formatRp = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID")).apply { maximumFractionDigits = 0 }
                        val spentFormatted = formatRp.format(spentThisMonth).replace("Rp", "Rp ")
                        val limitFormatted = formatRp.format(activeBudget.limitAmount).replace("Rp", "Rp ")
                        
                        showNotification(
                            context,
                            "Peringatan Anggaran Pintar",
                            "Pengeluaran $catName mencapai $spentFormatted dari batas $limitFormatted!",
                            categoryId
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat pengeluaran mendekati batas anggaran"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.cashmanage.R.drawable.ic_justsayit_logo)
            .setColor(android.graphics.Color.parseColor("#FFBD59"))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}
