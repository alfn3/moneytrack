package com.example.cashmanage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashmanage.ui.viewmodel.FinancialViewModel
import com.example.cashmanage.data.db.SavingGoalEntity
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingGoalsScreen(
    viewModel: FinancialViewModel = viewModel(),
    onBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val goals by viewModel.savingGoals.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }
    var newGoalName by remember { mutableStateOf("") }
    var newGoalTarget by remember { mutableStateOf("") }

    val totalLiquid = accounts.sumOf { it.balance }
    val totalNonLiquid = goals.sumOf { it.currentAmount }
    val totalAssets = totalLiquid + totalNonLiquid
    
    val numberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tracker Aset") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Aset Non-Likuid")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Kekayaan Bersih", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Rp ${numberFormat.format(totalAssets)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                Text("Aset Likuid (Saldo Rekening)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(accounts) { account ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("Rp ${numberFormat.format(account.balance)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Aset Non-Likuid (Investasi & Target)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(goals) { goal ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Rp ${numberFormat.format(goal.currentAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Target: Rp ${numberFormat.format(goal.targetAmount)}", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Tambah Target Aset Baru") },
                text = {
                    Column {
                        OutlinedTextField(value = newGoalName, onValueChange = { newGoalName = it }, label = { Text("Nama Aset/Target (misal: Reksadana)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = newGoalTarget, onValueChange = { newGoalTarget = it }, label = { Text("Target Jumlah (Rp)") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val target = newGoalTarget.toDoubleOrNull()
                        if (target != null && newGoalName.isNotBlank()) {
                            viewModel.addSavingGoal(newGoalName, target, "NON_LIQUID")
                        }
                        showDialog = false
                    }) { Text("Simpan") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}
