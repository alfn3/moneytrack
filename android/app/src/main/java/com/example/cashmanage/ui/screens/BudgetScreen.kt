package com.example.cashmanage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashmanage.ui.viewmodel.FinancialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: FinancialViewModel = viewModel(),
    onBack: () -> Unit
) {
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anggaran Bulanan") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(budgets) { budget ->
                    // Simplified logic: calculating expenses for this category
                    val spent = transactions
                        .filter { it.categoryId == budget.categoryId && it.type == "EXPENSE" }
                        .sumOf { it.amount }
                    val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat() else 0f
                    
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Kategori ID: ${budget.categoryId}", style = MaterialTheme.typography.titleMedium)
                            Text("Terpakai: Rp $spent / Batas: Rp ${budget.limitAmount}")
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                color = if (progress >= 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            if (progress >= 0.9f) {
                                Text("Peringatan: Anggaran hampir habis!", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
