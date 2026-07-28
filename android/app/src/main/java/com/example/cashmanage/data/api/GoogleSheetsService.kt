package com.example.cashmanage.data.api

import android.content.Context
import com.example.cashmanage.data.db.TransactionEntity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.google.api.services.sheets.v4.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.client.http.InputStreamContent
import com.google.api.services.drive.model.File

class GoogleSheetsService(private val context: Context, private val account: GoogleSignInAccount) {

    private val credential = GoogleAccountCredential.usingOAuth2(
        context, listOf(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_FILE)
    ).apply {
        selectedAccount = account.account
    }

    private val transport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    private val sheetsService = Sheets.Builder(transport, jsonFactory, credential)
        .setApplicationName("JustSayIt. ")
        .build()

    private val driveService = Drive.Builder(transport, jsonFactory, credential)
        .setApplicationName("JustSayIt. ")
        .build()

    suspend fun createSpreadsheet(title: String): String = withContext(Dispatchers.IO) {
        val fileMetadata = File().apply {
            name = title
            mimeType = "application/vnd.google-apps.spreadsheet"
        }
        
        val inputStream = context.assets.open("template_sheet.xlsx")
        val mediaContent = InputStreamContent("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", inputStream)
        
        val file = driveService.files().create(fileMetadata, mediaContent)
            .setFields("id")
            .execute()
            
        file.id
    }

    suspend fun pullAllData(spreadsheetId: String): PulledSpreadsheetData? = withContext(Dispatchers.IO) {
        try {
            val ranges = listOf(
                "SETTINGS!A4:B",
                "TRACKER ASET!B5:C7",
                "TRACKER ASET!B11:C15",
                "BUDGETING TAHUNAN!B4:M14",
                "CATATAN PENGELUARAN!A4:F"
            )
            val response = sheetsService.spreadsheets().values().batchGet(spreadsheetId).setRanges(ranges).execute()
            val valueRanges = response.valueRanges ?: return@withContext null

            // 1. Settings (Categories & Accounts)
            val categories = mutableListOf<com.example.cashmanage.data.db.CategoryEntity>()
            val accounts = mutableListOf<com.example.cashmanage.data.db.AccountEntity>()
            val settingsValues = valueRanges.getOrNull(0)?.getValues()
            if (settingsValues != null) {
                for (row in settingsValues) {
                    val catName = row.getOrNull(0)?.toString()?.trim()
                    if (!catName.isNullOrEmpty()) {
                        categories.add(com.example.cashmanage.data.db.CategoryEntity(name = catName, type = "EXPENSE", icon = null))
                    }
                    val accName = row.getOrNull(1)?.toString()?.trim()
                    if (!accName.isNullOrEmpty()) {
                        accounts.add(com.example.cashmanage.data.db.AccountEntity(userId = "default", name = accName, balance = 0.0))
                    }
                }
            }

            // 2. Saving Goals (Liquid & Non-Liquid)
            val savingGoals = mutableListOf<com.example.cashmanage.data.db.SavingGoalEntity>()
            val liquidValues = valueRanges.getOrNull(1)?.getValues()
            if (liquidValues != null) {
                for (row in liquidValues) {
                    val name = row.getOrNull(0)?.toString()?.trim()
                    val amountStr = row.getOrNull(1)?.toString()?.trim() ?: "0"
                    val amount = amountStr.replace("[^\\d]".toRegex(), "").toDoubleOrNull() ?: 0.0
                    if (!name.isNullOrEmpty()) {
                        savingGoals.add(com.example.cashmanage.data.db.SavingGoalEntity(name = name, targetAmount = 0.0, currentAmount = amount, targetDate = null, assetGroup = "LIQUID", isSynced = true))
                    }
                }
            }
            val nonLiquidValues = valueRanges.getOrNull(2)?.getValues()
            if (nonLiquidValues != null) {
                for (row in nonLiquidValues) {
                    val name = row.getOrNull(0)?.toString()?.trim()
                    val amountStr = row.getOrNull(1)?.toString()?.trim() ?: "0"
                    val amount = amountStr.replace("[^\\d]".toRegex(), "").toDoubleOrNull() ?: 0.0
                    if (!name.isNullOrEmpty()) {
                        savingGoals.add(com.example.cashmanage.data.db.SavingGoalEntity(name = name, targetAmount = 0.0, currentAmount = amount, targetDate = null, assetGroup = "NON_LIQUID", isSynced = true))
                    }
                }
            }

            // 3. Budgets
            val rawBudgets = mutableListOf<RawBudget>()
            val budgetValues = valueRanges.getOrNull(3)?.getValues()
            if (budgetValues != null) {
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                for (rowIdx in budgetValues.indices) {
                    val row = budgetValues[rowIdx]
                    val catName = categories.getOrNull(rowIdx)?.name ?: continue
                    for (colIdx in row.indices) {
                        val cellValueStr = row[colIdx].toString()
                        val cellValue = cellValueStr.replace("[^\\d]".toRegex(), "")
                        if (cellValue.isNotEmpty()) {
                            val limitAmount = cellValue.toDoubleOrNull()
                            if (limitAmount != null && limitAmount > 0) {
                                rawBudgets.add(RawBudget(categoryName = catName, limitAmount = limitAmount, month = colIdx + 1, year = currentYear))
                            }
                        }
                    }
                }
            }

            // 4. Transactions
            val rawTransactions = mutableListOf<RawTransaction>()
            val txValues = valueRanges.getOrNull(4)?.getValues()
            if (txValues != null) {
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                for (row in txValues) {
                    val dateStr = row.getOrNull(0)?.toString()?.trim() ?: continue
                    val typeStr = row.getOrNull(1)?.toString()?.trim() ?: "Pengeluaran"
                    val catName = row.getOrNull(2)?.toString()?.trim() ?: "Lainnya"
                    val notes = row.getOrNull(3)?.toString()?.trim() ?: ""
                    val accName = row.getOrNull(4)?.toString()?.trim() ?: "Tunai"
                    val amountStr = row.getOrNull(5)?.toString()?.trim() ?: "0"

                    val type = if (typeStr.equals("Pemasukan", ignoreCase = true)) "INCOME" else "EXPENSE"
                    val amount = amountStr.replace("[^\\d]".toRegex(), "").toDoubleOrNull() ?: 0.0
                    var dateLong = System.currentTimeMillis()
                    try {
                        val parsed = sdf.parse(dateStr)
                        if (parsed != null) dateLong = parsed.time
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    rawTransactions.add(
                        RawTransaction(
                            date = dateLong,
                            type = type,
                            categoryName = catName,
                            notes = notes,
                            accountName = accName,
                            amount = amount
                        )
                    )
                }
            }

            PulledSpreadsheetData(categories, accounts, savingGoals, rawBudgets, rawTransactions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun syncAllData(
        spreadsheetId: String,
        transactions: List<TransactionEntity>,
        budgets: List<com.example.cashmanage.data.db.BudgetEntity>,
        goals: List<com.example.cashmanage.data.db.SavingGoalEntity>,
        categories: List<com.example.cashmanage.data.db.CategoryEntity>,
        accounts: List<com.example.cashmanage.data.db.AccountEntity>
    ) = withContext(Dispatchers.IO) {
        val catMap = categories.associateBy { it.id }
        val accMap = accounts.associateBy { it.id }

        // 1. Transactions (Catatan Pengeluaran)
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        val formatRp = java.text.NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
        
        val txValues = transactions.map { tx ->
            val dateStr = sdf.format(Date(tx.date))
            val catName = catMap[tx.categoryId]?.name ?: "Lainnya"
            val accName = accMap[tx.accountId]?.name ?: "Tunai"
            val typeStr = if (tx.type == "INCOME") "Pemasukan" else "Pengeluaran"
            val amountStr = formatRp.format(tx.amount).replace("Rp", "Rp ")
            listOf(dateStr, typeStr, catName, tx.notes ?: "", accName, amountStr)
        }

        // 2. Budgeting Tahunan
        val budgetGrid = MutableList(11) { MutableList<Any>(12) { "" } }
        // Assumes categories 1 to 11 are perfectly ordered in rows 4 to 14
        budgets.forEach { b ->
            val rowIdx = b.categoryId - 1 // 0-indexed for 11 categories
            val colIdx = b.month - 1 // 0-indexed for Jan-Dec
            if (rowIdx in 0..10 && colIdx in 0..11) {
                budgetGrid[rowIdx][colIdx] = formatRp.format(b.limitAmount).replace("Rp", "Rp ")
            }
        }

        // 3. Tracker Aset
        val liquidGoals = goals.filter { it.assetGroup == "LIQUID" }.take(3)
        val nonLiquidGoals = goals.filter { it.assetGroup == "NON_LIQUID" }.take(5)
        
        val liquidValues = liquidGoals.map { listOf(it.name, formatRp.format(it.currentAmount).replace("Rp", "Rp ")) }
        val nonLiquidValues = nonLiquidGoals.map { listOf(it.name, formatRp.format(it.currentAmount).replace("Rp", "Rp ")) }

        // 4. Settings (Kategori & Rekening)
        val maxLen = maxOf(categories.size, accounts.size)
        val settingsValues = mutableListOf<List<Any>>()
        for (i in 0 until maxLen) {
            val catName = if (i < categories.size) categories[i].name else ""
            val accName = if (i < accounts.size) accounts[i].name else ""
            settingsValues.add(listOf(catName, accName))
        }

        // Batch Clear
        val clearBody = BatchClearValuesRequest().setRanges(listOf(
            "CATATAN PENGELUARAN!A4:F",
            "BUDGETING TAHUNAN!B4:M14",
            "TRACKER ASET!B5:C7",
            "TRACKER ASET!B11:C15",
            "SETTINGS!A4:B"
        ))
        sheetsService.spreadsheets().values().batchClear(spreadsheetId, clearBody).execute()

        // Batch Update
        val data = mutableListOf<ValueRange>()
        if (txValues.isNotEmpty()) {
            data.add(ValueRange().setRange("CATATAN PENGELUARAN!A4").setValues(txValues))
        }
        data.add(ValueRange().setRange("BUDGETING TAHUNAN!B4").setValues(budgetGrid.toList()))
        
        if (liquidValues.isNotEmpty()) {
            data.add(ValueRange().setRange("TRACKER ASET!B5").setValues(liquidValues))
        }
        if (nonLiquidValues.isNotEmpty()) {
            data.add(ValueRange().setRange("TRACKER ASET!B11").setValues(nonLiquidValues))
        }
        if (settingsValues.isNotEmpty()) {
            data.add(ValueRange().setRange("SETTINGS!A4").setValues(settingsValues))
        }

        if (data.isNotEmpty()) {
            val batchBody = BatchUpdateValuesRequest()
                .setValueInputOption("USER_ENTERED")
                .setData(data)
            sheetsService.spreadsheets().values().batchUpdate(spreadsheetId, batchBody).execute()
        }
    }
}
