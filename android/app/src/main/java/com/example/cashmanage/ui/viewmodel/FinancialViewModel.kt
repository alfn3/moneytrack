package com.example.cashmanage.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cashmanage.data.db.*
import com.example.cashmanage.data.repository.FinancialRepository
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val prefs = application.getSharedPreferences("cashmanage_prefs", Context.MODE_PRIVATE)
    private val aiLearningManager = com.example.cashmanage.ai.AILearningManager(application)

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    var hasShownStartupSync = false

    fun setDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean("dark_mode", isDark).apply()
        _isDarkMode.value = isDark
    }

    val transactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val savingGoals: StateFlow<List<SavingGoalEntity>> = repository.getAllSavingGoals()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = repository.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addTransaction(accountId: Int, categoryId: Int, amount: Double, type: String, notes: String?) {
        viewModelScope.launch {
            try {
                var validAccountId = accountId
                if (accounts.value.isNotEmpty() && accounts.value.none { it.id == validAccountId }) {
                    validAccountId = accounts.value.first().id
                }

                var validCategoryId = categoryId
                if (categories.value.isNotEmpty() && categories.value.none { it.id == validCategoryId }) {
                    validCategoryId = categories.value.first().id
                }

                repository.addTransaction(
                    TransactionEntity(
                        accountId = validAccountId,
                        categoryId = validCategoryId,
                        amount = amount,
                        type = type,
                        notes = notes,
                        date = System.currentTimeMillis()
                    )
                )
                
                com.example.cashmanage.util.BudgetAlertHelper.checkBudgetLimit(getApplication(), amount, categoryId, type)
                enqueueSync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            try {
                // Cari transaksi lama untuk direkam ke AI Learning jika kategori atau rekening berubah
                val oldTx = transactions.value.find { it.id == transaction.id }
                if (oldTx != null && (oldTx.categoryId != transaction.categoryId || oldTx.accountId != transaction.accountId)) {
                    aiLearningManager.recordCorrection(transaction.notes, oldTx.categoryId, transaction.categoryId, oldTx.accountId, transaction.accountId)
                }
                
                repository.updateTransaction(transaction)
                enqueueSync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction)
                enqueueSync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addSavingGoal(name: String, targetAmount: Double, assetGroup: String) {
        viewModelScope.launch {
            repository.addSavingGoal(
                SavingGoalEntity(
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = 0.0,
                    targetDate = null,
                    assetGroup = assetGroup
                )
            )
            enqueueSync()
        }
    }

    // --- Budgets CRUD ---
    fun addBudgetEntry(categoryId: Int, limitAmount: Double, month: Int, year: Int) {
        viewModelScope.launch {
            val existing = budgets.value.find { it.categoryId == categoryId && it.month == month && it.year == year }
            if (existing != null) {
                repository.updateBudget(existing.copy(limitAmount = limitAmount))
            } else {
                repository.addBudget(
                    BudgetEntity(
                        categoryId = categoryId,
                        limitAmount = limitAmount,
                        month = month,
                        year = year
                    )
                )
            }
            enqueueSync()
        }
    }

    fun updateBudgetEntry(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.updateBudget(budget)
            enqueueSync()
        }
    }

    fun deleteBudgetEntry(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            enqueueSync()
        }
    }

    // --- Accounts CRUD ---
    fun addAccount(name: String, balance: Double) {
        viewModelScope.launch {
            repository.addAccount(
                AccountEntity(
                    userId = "default", // or fetch from auth
                    name = name,
                    balance = balance
                )
            )
            enqueueSync()
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
            enqueueSync()
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            enqueueSync()
        }
    }

    // --- Categories CRUD ---
    fun addCategory(name: String, type: String, icon: String? = null) {
        viewModelScope.launch {
            repository.addCategory(
                CategoryEntity(
                    name = name,
                    type = type,
                    icon = icon
                )
            )
            enqueueSync()
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
            enqueueSync()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            enqueueSync()
        }
    }

    private fun enqueueSync() {
        com.example.cashmanage.util.UIUtils.showCustomToast(getApplication(), "Data lokal berhasil disimpan")
        androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "SyncTransactionsWork", 
            androidx.work.ExistingWorkPolicy.REPLACE, 
            androidx.work.OneTimeWorkRequestBuilder<com.example.cashmanage.worker.SyncWorker>().build()
        )
    }
    fun pullDataFromSpreadsheet(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val authManager = com.example.cashmanage.auth.GoogleAuthManager(getApplication())
                val account = authManager.getLastSignedInAccount()
                if (account == null) {
                    onResult(false, "Anda belum login ke akun Google.")
                    return@launch
                }
                
                val spreadsheetId = prefs.getString("spreadsheet_id", null)
                if (spreadsheetId == null) {
                    onResult(false, "Spreadsheet belum pernah disinkronkan.")
                    return@launch
                }

                val sheetsService = com.example.cashmanage.data.api.GoogleSheetsService(getApplication(), account)
                val pulledData = sheetsService.pullAllData(spreadsheetId)
                if (pulledData == null) {
                    onResult(false, "Gagal mengambil data dari Google Sheets.")
                    return@launch
                }

                val db = AppDatabase.getDatabase(getApplication())
                
                // Clear synced data, keeping offline unsynced data safe
                db.transactionDao().deleteSyncedTransactions()
                db.budgetDao().deleteSyncedBudgets()
                db.savingGoalDao().deleteSyncedSavingGoals()
                // We do not delete categories and accounts to preserve their IDs for offline data.

                // Insert Categories & Accounts, and map names to IDs
                val catMap = mutableMapOf<String, Int>()
                pulledData.categories.forEach { cat ->
                    val existing = db.categoryDao().getCategoryByName(cat.name)
                    val id = if (existing != null) {
                        existing.id
                    } else {
                        db.categoryDao().insertCategory(cat).toInt()
                    }
                    catMap[cat.name] = id
                }

                val accMap = mutableMapOf<String, Int>()
                pulledData.accounts.forEach { acc ->
                    val existing = db.accountDao().getAccountByName(acc.name)
                    val id = if (existing != null) {
                        existing.id
                    } else {
                        db.accountDao().insertAccount(acc).toInt()
                    }
                    accMap[acc.name] = id
                }

                // Insert Saving Goals
                pulledData.savingGoals.forEach { goal ->
                    db.savingGoalDao().insertSavingGoal(goal)
                }

                // Insert Budgets
                val budgetsToInsert = pulledData.rawBudgets.mapNotNull { raw ->
                    val catId = catMap[raw.categoryName] ?: return@mapNotNull null
                    BudgetEntity(
                        categoryId = catId,
                        limitAmount = raw.limitAmount,
                        month = raw.month,
                        year = raw.year,
                        isSynced = true
                    )
                }
                db.budgetDao().insertBudgets(budgetsToInsert)

                // Insert Transactions
                pulledData.rawTransactions.forEach { raw ->
                    val catId = catMap[raw.categoryName] ?: catMap.values.firstOrNull() ?: 1
                    val accId = accMap[raw.accountName] ?: accMap.values.firstOrNull() ?: 1
                    db.transactionDao().insertTransaction(
                        TransactionEntity(
                            accountId = accId,
                            categoryId = catId,
                            amount = raw.amount,
                            date = raw.date,
                            notes = raw.notes,
                            type = raw.type,
                            isSynced = true
                        )
                    )
                }
                
                onResult(true, "Data berhasil ditarik dari Spreadsheet.")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Terjadi kesalahan: ${e.message}")
            }
        }
    }
}
