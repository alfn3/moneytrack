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
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT * FROM categories WHERE type = :type")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>
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
}

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>
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
