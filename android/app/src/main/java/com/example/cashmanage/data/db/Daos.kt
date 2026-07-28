package com.example.cashmanage.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUser(uid: String): UserEntity?
}

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Query("SELECT * FROM accounts WHERE userId = :userId")
    fun getAccounts(userId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
    suspend fun getAccountByName(name: String): AccountEntity?

    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsList(): List<AccountEntity>

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT * FROM categories WHERE type = :type")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions ORDER BY date ASC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE isSynced = 1")
    suspend fun deleteSyncedTransactions()

    @Update
    suspend fun updateTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
}

@Dao
interface SavingGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingGoal(goal: SavingGoalEntity): Long

    @Query("SELECT * FROM saving_goals")
    fun getAllSavingGoals(): Flow<List<SavingGoalEntity>>
    
    @Query("SELECT * FROM saving_goals")
    suspend fun getAllSavingGoalsList(): List<SavingGoalEntity>

    @Query("DELETE FROM saving_goals WHERE isSynced = 1")
    suspend fun deleteSyncedSavingGoals()
}

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets")
    suspend fun clearAllBudgets()

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    
    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsList(): List<BudgetEntity>

    @Query("DELETE FROM budgets WHERE isSynced = 1")
    suspend fun deleteSyncedBudgets()
}

@Dao
interface SpreadsheetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpreadsheet(spreadsheet: SpreadsheetEntity): Long

    @Query("SELECT * FROM spreadsheets")
    fun getAllSpreadsheets(): Flow<List<SpreadsheetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSheet(sheet: SheetEntity): Long

    @Query("SELECT * FROM sheets WHERE spreadsheetId = :spreadsheetId")
    fun getSheetsBySpreadsheet(spreadsheetId: Int): Flow<List<SheetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCell(cell: CellEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCells(cells: List<CellEntity>)

    @Query("SELECT * FROM cells WHERE sheetId = :sheetId")
    fun getCellsBySheet(sheetId: Int): Flow<List<CellEntity>>
}

@Dao
interface AIHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: AIHistoryEntity): Long

    @Query("SELECT * FROM ai_history ORDER BY timestamp ASC")
    suspend fun getAllHistoryList(): List<AIHistoryEntity>
}

@Dao
interface AILearningRuleDao{


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(
        rule:AILearningRuleEntity
    )


    @Update
    suspend fun update(
        rule:AILearningRuleEntity
    )


    @Query("""
SELECT *
FROM ai_learning_rules
WHERE keyword=:keyword
LIMIT 1
""")
    suspend fun getRule(
        keyword:String
    ):AILearningRuleEntity?



    @Query("""
SELECT *
FROM ai_learning_rules
ORDER BY
confidence DESC,
frequency DESC,
lastUsed DESC
LIMIT 30
""")
    suspend fun getTopRules():
            List<AILearningRuleEntity>


}