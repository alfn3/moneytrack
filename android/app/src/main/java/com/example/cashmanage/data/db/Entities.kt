package com.example.cashmanage.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val name: String,
    val balance: Double
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // INCOME or EXPENSE
    val icon: String?
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"]),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"])
    ],
    indices = [Index("accountId"), Index("categoryId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val categoryId: Int,
    val amount: Double,
    val date: Long,
    val notes: String?,
    val type: String, // INCOME or EXPENSE
    val isSynced: Boolean = false
)

@Entity(tableName = "saving_goals")
data class SavingGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: Long?,
    val isSynced: Boolean = false
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val limitAmount: Double,
    val period: String, // MONTHLY, WEEKLY
    val isSynced: Boolean = false
)

// Spreadsheet Engine Entities
@Entity(tableName = "spreadsheets")
data class SpreadsheetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long,
    val isSynced: Boolean = false
)

@Entity(
    tableName = "sheets",
    foreignKeys = [ForeignKey(entity = SpreadsheetEntity::class, parentColumns = ["id"], childColumns = ["spreadsheetId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("spreadsheetId")]
)
data class SheetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val spreadsheetId: Int,
    val name: String
)

@Entity(
    tableName = "cells",
    foreignKeys = [ForeignKey(entity = SheetEntity::class, parentColumns = ["id"], childColumns = ["sheetId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sheetId")]
)
data class CellEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sheetId: Int,
    val row: Int,
    val col: Int,
    val rawValue: String,
    val computedValue: String?
)

// AI & History Entities
@Entity(tableName = "ai_history")
data class AIHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val response: String,
    val timestamp: Long
)

@Entity(tableName = "ocr_history")
data class OCRHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imagePath: String,
    val extractedText: String,
    val parsedAmount: Double?,
    val timestamp: Long
)
