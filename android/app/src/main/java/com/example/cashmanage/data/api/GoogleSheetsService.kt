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
        .setApplicationName("Cash Manage")
        .build()

    private val driveService = Drive.Builder(transport, jsonFactory, credential)
        .setApplicationName("Cash Manage")
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



    suspend fun overwriteTransactions(spreadsheetId: String, transactions: List<TransactionEntity>) = withContext(Dispatchers.IO) {
        // Clear existing data from row 3 downwards (assuming row 1 and 2 are headers)
        sheetsService.spreadsheets().values()
            .clear(spreadsheetId, "CATATAN PENGELUARAN!B3:F", ClearValuesRequest())
            .execute()

        if (transactions.isEmpty()) return@withContext

        val values = formatTransactions(transactions)
        val body = ValueRange().setValues(values)
        sheetsService.spreadsheets().values()
            .update(spreadsheetId, "CATATAN PENGELUARAN!B3", body)
            .setValueInputOption("USER_ENTERED")
            .execute()
    }

    private fun formatTransactions(transactions: List<TransactionEntity>): List<List<String>> {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        return transactions.map {
            val dateStr = sdf.format(Date(it.date))
            val categoryStr = when (it.categoryId) {
                1 -> "Makanan"
                2 -> "Transport"
                3 -> "Gaji"
                4 -> "Hiburan"
                else -> "Lainnya"
            }
            listOf(dateStr, categoryStr, it.notes ?: "", it.amount.toString(), it.type)
        }
    }
}
