package com.example.cashmanage.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashmanage.ui.viewmodel.FinancialViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinancialViewModel = viewModel(),
    onNavigateToOcr: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onOpenDrawer: () -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
    val userName = account?.givenName ?: account?.displayName ?: "Pengguna"

    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatRp.maximumFractionDigits = 0

    val cardColor = Color(0xFF1E3A34)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                // Header Profil
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Halo,", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    IconButton(onClick = { /* Handle Notifications */ }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color(0xFF333333))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Main Balance Card
                val monthYearFormat = java.text.SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                val currentMonthYear = monthYearFormat.format(java.util.Date())

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            "Sisa $currentMonthYear",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            formatRp.format(balance).replace("Rp", "Rp "),
                            color = Color.White,
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

            item {
                Spacer(modifier = Modifier.height(32.dp))
                // Pengeluaran Per Kategori (Progress Bars)
                Text("Pengeluaran Kategori", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(16.dp))

                val expensesByCategory = transactions
                    .filter { it.type == "EXPENSE" }
                    .groupBy { it.categoryId }
                    .mapValues { it.value.sumOf { tx -> tx.amount } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(3)

                if (expensesByCategory.isEmpty()) {
                    Text("Belum ada data pengeluaran.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    val maxExpense = expensesByCategory.maxOf { it.second }.toFloat()
                    expensesByCategory.forEach { (catId, amount) ->
                        val catName = categories.find { it.id == catId }?.name ?: "Kategori $catId"
                        val progress = if (maxExpense > 0) (amount.toFloat() / totalExpense.toFloat()) else 0f
                        CategoryProgressItem(
                            categoryName = catName,
                            amount = formatRp.format(amount).replace("Rp", "Rp "),
                            progress = progress
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                // Transaksi Bulan Ini
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transaksi Bulan Ini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onNavigateToTransactions) {
                        Text("Lihat Semua", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (transactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada transaksi", color = Color.Gray)
                    }
                }
            } else {
                items(transactions.take(5)) { tx ->
                    val isIncome = tx.type == "INCOME"
                    val txDateStr = java.text.SimpleDateFormat("dd MMM", Locale("id", "ID")).format(java.util.Date(tx.date))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
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
                            Text(tx.notes ?: "Transaksi", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            val catName = categories.find { it.id == tx.categoryId }?.name ?: "Kat: ${tx.categoryId}"
                            Text("$catName • $txDateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            (if (isIncome) "+" else "-") + formatRp.format(tx.amount).replace("Rp", "Rp "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
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
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            Text(amount, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
fun CategoryProgressItem(categoryName: String, amount: String, progress: Float) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000))
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF1E3A34),
            trackColor = Color(0xFFE0E0E0),
        )
    }
}
