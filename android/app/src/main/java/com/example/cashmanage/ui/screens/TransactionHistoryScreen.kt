package com.example.cashmanage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashmanage.data.db.TransactionEntity
import com.example.cashmanage.ui.viewmodel.FinancialViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.text.DateFormatSymbols
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onBack: () -> Unit,
    viewModel: FinancialViewModel = viewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var selectedMonth by remember { mutableStateOf<Int?>(null) } // 0 = Jan, 11 = Dec
    var selectedAccountId by remember { mutableStateOf<Int?>(null) }
    var selectedType by remember { mutableStateOf<String?>(null) } // "INCOME" or "EXPENSE"

    var showDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    val catMap = remember(categories) { categories.associateBy { it.id } }
    val accMap = remember(accounts) { accounts.associateBy { it.id } }

    val filteredTransactions = remember(transactions, selectedMonth, selectedAccountId, selectedType) {
        transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
            val matchMonth = selectedMonth == null || cal.get(Calendar.MONTH) == selectedMonth
            val matchAccount = selectedAccountId == null || tx.accountId == selectedAccountId
            val matchType = selectedType == null || tx.type == selectedType
            matchMonth && matchAccount && matchType
        }.sortedByDescending { it.date }
    }

    if (showDialog) {
        TransactionDialog(
            transaction = transactionToEdit,
            categories = categories,
            accounts = accounts,
            onDismiss = { 
                showDialog = false
                transactionToEdit = null
            },
            onSave = { accId, catId, amt, type, note ->
                if (transactionToEdit == null) {
                    viewModel.addTransaction(accId, catId, amt, type, note)
                } else {
                    viewModel.updateTransaction(
                        transactionToEdit!!.copy(
                            accountId = accId,
                            categoryId = catId,
                            amount = amt,
                            type = type,
                            notes = note
                        )
                    )
                }
                showDialog = false
                transactionToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                transactionToEdit = null
                showDialog = true 
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Transaksi")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filters Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var expandedMonth by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedMonth,
                    onExpandedChange = { expandedMonth = !expandedMonth },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedMonth?.let { DateFormatSymbols(Locale("id", "ID")).months[it] } ?: "Semua Bulan",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMonth) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedMonth, onDismissRequest = { expandedMonth = false }) {
                        DropdownMenuItem(
                            text = { Text("Semua Bulan") },
                            onClick = { selectedMonth = null; expandedMonth = false }
                        )
                        val months = DateFormatSymbols(Locale("id", "ID")).months
                        for (index in 0 until 12) {
                            DropdownMenuItem(
                                text = { Text(months[index]) },
                                onClick = { selectedMonth = index; expandedMonth = false }
                            )
                        }
                    }
                }

                var expandedAcc by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedAcc,
                    onExpandedChange = { expandedAcc = !expandedAcc },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedAccountId?.let { accMap[it]?.name } ?: "Semua Rek",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAcc) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedAcc, onDismissRequest = { expandedAcc = false }) {
                        DropdownMenuItem(
                            text = { Text("Semua Rekening") },
                            onClick = { selectedAccountId = null; expandedAcc = false }
                        )
                        for (acc in accounts) {
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = { selectedAccountId = acc.id; expandedAcc = false }
                            )
                        }
                    }
                }
            }

            // Type Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text("Semua") }
                )
                FilterChip(
                    selected = selectedType == "EXPENSE",
                    onClick = { selectedType = "EXPENSE" },
                    label = { Text("Pengeluaran") }
                )
                FilterChip(
                    selected = selectedType == "INCOME",
                    onClick = { selectedType = "INCOME" },
                    label = { Text("Pemasukan") }
                )
            }

            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Belum ada transaksi", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        TransactionItem(
                            transaction = tx,
                            categoryName = catMap[tx.categoryId]?.name ?: "Lainnya",
                            accountName = accMap[tx.accountId]?.name ?: "Tunai",
                            onEdit = {
                                transactionToEdit = tx
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionEntity, categoryName: String, accountName: String, onEdit: () -> Unit) {
    val isIncome = transaction.type == "INCOME"
    val color = if (isIncome) Color(0xFF4CAF50) else Color(0xFFE53935)
    val sign = if (isIncome) "+" else "-"
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    val numberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!transaction.notes.isNullOrBlank()) {
                    Text(transaction.notes, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text(accountName, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(sdf.format(Date(transaction.date)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$sign Rp ${numberFormat.format(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(
    transaction: TransactionEntity?,
    categories: List<com.example.cashmanage.data.db.CategoryEntity>,
    accounts: List<com.example.cashmanage.data.db.AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (accountId: Int, categoryId: Int, amount: Double, type: String, note: String) -> Unit
) {
    var type by remember { mutableStateOf(transaction?.type ?: "EXPENSE") }
    var amount by remember { mutableStateOf(transaction?.amount?.toLong()?.toString() ?: "") }
    var note by remember { mutableStateOf(transaction?.notes ?: "") }
    
    var selectedCat by remember { mutableStateOf(categories.find { it.id == transaction?.categoryId } ?: categories.firstOrNull()) }
    var selectedAcc by remember { mutableStateOf(accounts.find { it.id == transaction?.accountId } ?: accounts.firstOrNull()) }

    var expandedCat by remember { mutableStateOf(false) }
    var expandedAcc by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transaction == null) "Tambah Transaksi" else "Edit Transaksi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Type Switcher
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("Pengeluaran") }
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text("Pemasukan") }
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) amount = it },
                    label = { Text("Jumlah (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category
                ExposedDropdownMenuBox(
                    expanded = expandedCat,
                    onExpandedChange = { expandedCat = !expandedCat },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCat?.name ?: "Pilih Kategori",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                        categories.filter { if (type == "INCOME") it.type == "INCOME" else it.type != "INCOME" }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = { selectedCat = cat; expandedCat = false }
                            )
                        }
                    }
                }

                // Account
                ExposedDropdownMenuBox(
                    expanded = expandedAcc,
                    onExpandedChange = { expandedAcc = !expandedAcc },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedAcc?.name ?: "Pilih Rekening",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rekening") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAcc) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedAcc, onDismissRequest = { expandedAcc = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = { selectedAcc = acc; expandedAcc = false }
                            )
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Keterangan (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && selectedCat != null && selectedAcc != null) {
                        onSave(selectedAcc!!.id, selectedCat!!.id, amt, type, note)
                    }
                },
                enabled = amount.isNotBlank() && selectedCat != null && selectedAcc != null
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
