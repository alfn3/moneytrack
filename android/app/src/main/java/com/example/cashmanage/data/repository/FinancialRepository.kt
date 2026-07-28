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

    suspend fun updateBudget(budget: BudgetEntity) {
        database.budgetDao().updateBudget(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        database.budgetDao().deleteBudget(budget)
    }
    
    suspend fun clearBudgets() {
        database.budgetDao().clearAllBudgets()
    }
    
    suspend fun insertBudgets(budgets: List<BudgetEntity>) {
        database.budgetDao().insertBudgets(budgets)
    }

    // --- Accounts ---
    fun getAllAccounts(): Flow<List<AccountEntity>> = database.accountDao().getAllAccounts()

    suspend fun addAccount(account: AccountEntity) {
        database.accountDao().insertAccount(account)
    }
    
    suspend fun updateAccount(account: AccountEntity) {
        database.accountDao().updateAccount(account)
    }
    
    suspend fun deleteAccount(account: AccountEntity) {
        database.accountDao().deleteAccount(account)
    }

    // --- Categories ---
    fun getAllCategories(): Flow<List<CategoryEntity>> = database.categoryDao().getAllCategories()

    suspend fun addCategory(category: CategoryEntity) {
        database.categoryDao().insertCategory(category)
    }
    
    suspend fun updateCategory(category: CategoryEntity) {
        database.categoryDao().updateCategory(category)
    }
    
    suspend fun deleteCategory(category: CategoryEntity) {
        database.categoryDao().deleteCategory(category)
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
