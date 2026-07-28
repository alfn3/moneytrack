package com.example.cashmanage.data.model

data class TransactionDraft(
    val amount: Double,
    val categoryId: Int,
    val accountId: Int,
    val type: String,
    val notes: String,
    val confidence: Double = 0.0
)