package com.example.cashmanage.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cashmanage.data.db.*
import com.example.cashmanage.data.repository.FinancialRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinancialViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinancialRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinancialRepository(database)
    }

    val transactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val savingGoals: StateFlow<List<SavingGoalEntity>> = repository.getAllSavingGoals()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = repository.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addTransaction(accountId: Int, categoryId: Int, amount: Double, type: String, notes: String?) {
        viewModelScope.launch {
            try {
                repository.addTransaction(
                    TransactionEntity(
                        accountId = accountId,
                        categoryId = categoryId,
                        amount = amount,
                        type = type,
                        notes = notes,
                        date = System.currentTimeMillis()
                    )
                )
                androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "SyncTransactionsWork", 
                    androidx.work.ExistingWorkPolicy.REPLACE, 
                    androidx.work.OneTimeWorkRequestBuilder<com.example.cashmanage.worker.SyncWorker>().build()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            try {
                repository.updateTransaction(transaction)
                androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "SyncTransactionsWork", 
                    androidx.work.ExistingWorkPolicy.REPLACE, 
                    androidx.work.OneTimeWorkRequestBuilder<com.example.cashmanage.worker.SyncWorker>().build()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction)
                androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "SyncTransactionsWork", 
                    androidx.work.ExistingWorkPolicy.REPLACE, 
                    androidx.work.OneTimeWorkRequestBuilder<com.example.cashmanage.worker.SyncWorker>().build()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addSavingGoal(name: String, targetAmount: Double) {
        viewModelScope.launch {
            repository.addSavingGoal(
                SavingGoalEntity(
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = 0.0,
                    targetDate = null
                )
            )
        }
    }
}
