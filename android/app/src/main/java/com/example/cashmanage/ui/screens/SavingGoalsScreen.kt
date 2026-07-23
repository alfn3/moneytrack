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
fun SavingGoalsScreen(
    viewModel: FinancialViewModel = viewModel(),
    onBack: () -> Unit
) {
    val goals by viewModel.savingGoals.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var newGoalName by remember { mutableStateOf("") }
    var newGoalTarget by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Target Tabungan") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(goals) { goal ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(goal.name, style = MaterialTheme.typography.titleMedium)
                            Text("Terkumpul: Rp ${goal.currentAmount} / Rp ${goal.targetAmount}")
                            LinearProgressIndicator(
                                progress = { (goal.currentAmount / goal.targetAmount).toFloat() },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Tambah Target Tabungan") },
                text = {
                    Column {
                        TextField(value = newGoalName, onValueChange = { newGoalName = it }, label = { Text("Nama Target") })
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(value = newGoalTarget, onValueChange = { newGoalTarget = it }, label = { Text("Target Jumlah (Rp)") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val target = newGoalTarget.toDoubleOrNull()
                        if (target != null && newGoalName.isNotBlank()) {
                            viewModel.addSavingGoal(newGoalName, target)
                        }
                        showDialog = false
                    }) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}
