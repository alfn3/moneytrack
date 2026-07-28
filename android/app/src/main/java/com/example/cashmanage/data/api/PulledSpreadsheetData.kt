package com.example.cashmanage.data.api

import com.example.cashmanage.data.db.AccountEntity
import com.example.cashmanage.data.db.CategoryEntity
import com.example.cashmanage.data.db.SavingGoalEntity

data class PulledSpreadsheetData(
    val categories: List<CategoryEntity>,
    val accounts: List<AccountEntity>,
    val savingGoals: List<SavingGoalEntity>,
    val rawBudgets: List<RawBudget>,
    val rawTransactions: List<RawTransaction>
)

data class RawBudget(
    val categoryName: String,
    val limitAmount: Double,
    val month: Int,
    val year: Int
)

data class RawTransaction(
    val date: Long,
    val type: String,
    val categoryName: String,
    val notes: String,
    val accountName: String,
    val amount: Double
)
