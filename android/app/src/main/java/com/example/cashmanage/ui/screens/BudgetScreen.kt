package com.example.cashmanage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashmanage.ui.viewmodel.FinancialViewModel
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: FinancialViewModel = viewModel(),
    onBack: () -> Unit
) {
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    
    val catMap = remember(categories) { categories.associateBy { it.id } }
    
    val cal = Calendar.getInstance()
    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) } // 0-11
    
    var showDialog by remember { mutableStateOf(false) }

    val filteredBudgets = remember(budgets, selectedYear, selectedMonth) {
        budgets.filter { it.year == selectedYear && it.month == selectedMonth + 1 }
    }

    var budgetToEdit by remember { mutableStateOf<com.example.cashmanage.data.db.BudgetEntity?>(null) }

    val numberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    if (showDialog || budgetToEdit != null) {
        BudgetDialog(
            categories = categories,
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            budgetToEdit = budgetToEdit,
            onDismiss = { 
                showDialog = false 
                budgetToEdit = null
            },
            onSave = { catId, limitAmount ->
                if (budgetToEdit != null) {
                    viewModel.updateBudgetEntry(budgetToEdit!!.copy(categoryId = catId, limitAmount = limitAmount))
                } else {
                    viewModel.addBudgetEntry(catId, limitAmount, selectedMonth + 1, selectedYear)
                }
                showDialog = false
                budgetToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anggaran Bulanan ($selectedYear)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Set Budget")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Month Selector
            ScrollableTabRow(
                selectedTabIndex = selectedMonth,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val shortMonths = DateFormatSymbols(Locale("id", "ID")).shortMonths
                for (index in 0 until 12) {
                    val monthName = shortMonths[index]
                    Tab(
                        selected = selectedMonth == index,
                        onClick = { selectedMonth = index },
                        text = { Text(monthName) }
                    )
                }
            }
            
            if (filteredBudgets.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Belum ada anggaran untuk bulan ini", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp).weight(1f)) {
                    items(filteredBudgets) { budget ->
                        val catName = catMap[budget.categoryId]?.name ?: "Kategori Hapus"
                        val spent = transactions
                            .filter { 
                                val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
                                it.categoryId == budget.categoryId && 
                                it.type == "EXPENSE" &&
                                txCal.get(Calendar.YEAR) == selectedYear &&
                                txCal.get(Calendar.MONTH) == selectedMonth
                            }
                            .sumOf { it.amount }
                            
                        val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat() else 0f
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(catName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    
                                    Box {
                                        var menuExpanded by remember { mutableStateOf(false) }
                                        IconButton(onClick = { menuExpanded = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                                        }
                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Edit") },
                                                onClick = {
                                                    budgetToEdit = budget
                                                    menuExpanded = false
                                                },
                                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Hapus", color = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    viewModel.deleteBudgetEntry(budget)
                                                    menuExpanded = false
                                                },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val percentage = (progress * 100).toInt()
                                    Text("Terpakai: Rp ${numberFormat.format(spent)} ($percentage%)", style = MaterialTheme.typography.bodyMedium)
                                    Text("Batas: Rp ${numberFormat.format(budget.limitAmount)}", style = MaterialTheme.typography.bodyMedium)
                                }
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    color = if (progress >= 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                                if (progress >= 0.9f) {
                                    Text(
                                        "Peringatan: Anggaran hampir habis!", 
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDialog(
    categories: List<com.example.cashmanage.data.db.CategoryEntity>,
    selectedMonth: Int,
    selectedYear: Int,
    budgetToEdit: com.example.cashmanage.data.db.BudgetEntity? = null,
    onDismiss: () -> Unit,
    onSave: (categoryId: Int, limitAmount: Double) -> Unit
) {
    val expenseCategories = categories.filter { it.type == "EXPENSE" }

    var amount by remember { mutableStateOf(budgetToEdit?.limitAmount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var selectedCat by remember { mutableStateOf(expenseCategories.find { it.id == budgetToEdit?.categoryId } ?: expenseCategories.firstOrNull()) }
    var expandedCat by remember { mutableStateOf(false) }

    val monthName = DateFormatSymbols(Locale("id", "ID")).months[selectedMonth]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budgetToEdit != null) "Edit Anggaran" else "Set Anggaran - $monthName $selectedYear") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        expenseCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = { selectedCat = cat; expandedCat = false }
                            )
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) amount = it },
                    label = { Text("Batas Anggaran (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && selectedCat != null) {
                        onSave(selectedCat!!.id, amt)
                    }
                },
                enabled = amount.isNotBlank() && selectedCat != null
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
