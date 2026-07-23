package com.example.cashmanage.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashmanage.ui.viewmodel.FinancialViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinancialViewModel = viewModel(),
    onNavigateToOcr: () -> Unit,
    onNavigateToChat: () -> Unit,
    onOpenDrawer: () -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsState()

    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense
    
    var transactionToEdit by remember { mutableStateOf<com.example.cashmanage.data.db.TransactionEntity?>(null) }
    
    if (transactionToEdit != null) {
        EditTransactionDialog(
            transaction = transactionToEdit!!,
            onDismiss = { transactionToEdit = null },
            onSave = { updatedTx ->
                viewModel.updateTransaction(updatedTx)
                transactionToEdit = null
            }
        )
    }

    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatRp.maximumFractionDigits = 0

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text("Cash Manage", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) 
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(androidx.compose.material.icons.Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                // Balance Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total Saldo", 
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date()), 
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                formatRp.format(balance).replace("Rp", "Rp "), 
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IncomeExpenseItem(
                                    title = "Pemasukan",
                                    amount = formatRp.format(totalIncome).replace("Rp", "Rp "),
                                    icon = Icons.Filled.ArrowDownward,
                                    iconColor = Color(0xFF4CAF50)
                                )
                                IncomeExpenseItem(
                                    title = "Pengeluaran",
                                    amount = formatRp.format(totalExpense).replace("Rp", "Rp "),
                                    icon = Icons.Filled.ArrowUpward,
                                    iconColor = Color(0xFFF44336)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                // Smart Actions
                Text("Aksi Pintar (AI)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = "Scan Struk",
                        subtitle = "Otomatis input",
                        icon = Icons.Filled.CameraAlt,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = onNavigateToOcr
                    )
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = "Chat AI",
                        subtitle = "Tanya & input suara",
                        icon = Icons.Filled.Chat,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = onNavigateToChat
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                // Chart
                Text("Analisis Arus Kas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                ) {
                    AndroidView(
                        factory = { context ->
                            PieChart(context).apply {
                                description.isEnabled = false
                                setUsePercentValues(true)
                                isDrawHoleEnabled = true
                                setHoleColor(AndroidColor.TRANSPARENT)
                                setTransparentCircleAlpha(0)
                                holeRadius = 58f
                                transparentCircleRadius = 61f
                                setDrawCenterText(true)
                                centerText = "Cash Flow"
                                setCenterTextSize(16f)
                                setCenterTextColor(AndroidColor.GRAY)
                                legend.isEnabled = true
                                animateY(1400, Easing.EaseInOutQuad)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        update = { chart ->
                            if (totalIncome > 0 || totalExpense > 0) {
                                val entries = listOf(
                                    PieEntry(totalIncome.toFloat(), "Income"),
                                    PieEntry(totalExpense.toFloat(), "Expense")
                                )
                                val dataSet = PieDataSet(entries, "").apply {
                                    colors = listOf(
                                        AndroidColor.rgb(76, 175, 80), // Green
                                        AndroidColor.rgb(244, 67, 54)  // Red
                                    )
                                    sliceSpace = 3f
                                    selectionShift = 5f
                                    valueTextSize = 12f
                                    valueTextColor = AndroidColor.WHITE
                                }
                                chart.data = PieData(dataSet)
                                chart.invalidate()
                            } else {
                                chart.clear()
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Transaksi Terakhir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (transactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada transaksi", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(transactions.take(10)) { tx ->
                    val isIncome = tx.type == "INCOME"
                    val txDateStr = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(tx.date))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIncome) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.notes ?: "Transaksi", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text("Kat: ${tx.categoryId} • $txDateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                (if (isIncome) "+" else "-") + formatRp.format(tx.amount).replace("Rp", "Rp "),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                        onClick = {
                                            expanded = false
                                            transactionToEdit = tx
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Hapus") },
                                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            expanded = false
                                            viewModel.deleteTransaction(tx)
                                        }
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

@Composable
fun IncomeExpenseItem(title: String, amount: String, icon: ImageVector, iconColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            Text(amount, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActionCard(modifier: Modifier, title: String, subtitle: String, icon: ImageVector, containerColor: Color, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick).height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EditTransactionDialog(
    transaction: com.example.cashmanage.data.db.TransactionEntity,
    onDismiss: () -> Unit,
    onSave: (com.example.cashmanage.data.db.TransactionEntity) -> Unit
) {
    var amountText by remember { mutableStateOf(transaction.amount.toLong().toString()) }
    var notesText by remember { mutableStateOf(transaction.notes ?: "") }
    var type by remember { mutableStateOf(transaction.type) }
    var categoryId by remember { mutableStateOf(transaction.categoryId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaksi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Nominal") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Catatan") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text("Pemasukan") }
                    )
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("Pengeluaran") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: transaction.amount
                    onSave(transaction.copy(amount = amount, notes = notesText, type = type, categoryId = categoryId))
                }
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
