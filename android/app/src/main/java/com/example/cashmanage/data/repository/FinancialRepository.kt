package com.example.cashmanage.data.repository

import com.example.cashmanage.data.db.*
import kotlinx.coroutines.flow.Flow

class FinancialRepository(private val database: AppDatabase) {

    // --- Transactions ---
    fun getAllTransactions(): Flow<List<TransactionEntity>> = database.transactionDao().getAllTransactions()

    suspend fun addTransaction(transaction: TransactionEntity) {
        database.transactionDao().insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        database.transactionDao().updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.transactionDao().deleteTransaction(transaction)
    }

    // --- Saving Goals ---
    fun getAllSavingGoals(): Flow<List<SavingGoalEntity>> = database.savingGoalDao().getAllSavingGoals()

    suspend fun addSavingGoal(goal: SavingGoalEntity) {
        database.savingGoalDao().insertSavingGoal(goal)
    }

    // --- Budgets ---
    fun getAllBudgets(): Flow<List<BudgetEntity>> = database.budgetDao().getAllBudgets()

    suspend fun addBudget(budget: BudgetEntity) {
        database.budgetDao().insertBudget(budget)
    }

    // --- Spreadsheets ---
    fun getAllSpreadsheets(): Flow<List<SpreadsheetEntity>> = database.spreadsheetDao().getAllSpreadsheets()
    
    fun getSheets(spreadsheetId: Int): Flow<List<SheetEntity>> = database.spreadsheetDao().getSheetsBySpreadsheet(spreadsheetId)
    
    fun getCells(sheetId: Int): Flow<List<CellEntity>> = database.spreadsheetDao().getCellsBySheet(sheetId)

    suspend fun createSpreadsheet(name: String): Long {
        val id = database.spreadsheetDao().insertSpreadsheet(SpreadsheetEntity(name = name, createdAt = System.currentTimeMillis()))
        // Create default sheet
        database.spreadsheetDao().insertSheet(SheetEntity(spreadsheetId = id.toInt(), name = "Sheet1"))
        return id
    }
}
