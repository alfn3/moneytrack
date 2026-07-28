package com.example.cashmanage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cashmanage.ui.util.showCustomToast
import com.example.cashmanage.ui.util.ToastType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashmanage.data.db.AccountEntity
import com.example.cashmanage.data.db.CategoryEntity
import com.example.cashmanage.ui.viewmodel.FinancialViewModel
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.cashmanage.theme.CardBackgroundGlass
import com.example.cashmanage.theme.CardBackgroundGlassDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: FinancialViewModel = viewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()



    var showAccountDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan (Master Data)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
            var showSyncDialog by remember { mutableStateOf(false) }
            var isSyncing by remember { mutableStateOf(false) }
            val context = androidx.compose.ui.platform.LocalContext.current
            var expandedSection by remember { mutableStateOf<String?>(null) }

            if (showSyncDialog) {
                AlertDialog(
                    onDismissRequest = { showSyncDialog = false },
                    title = { Text("Tarik Data Spreadsheet") },
                    text = { Text("Menarik data terbaru dari Google Spreadsheet. Data offline yang belum tersinkronisasi tidak akan hilang.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showSyncDialog = false
                            isSyncing = true
                            viewModel.pullDataFromSpreadsheet { success, message ->
                                isSyncing = false
                                context.showCustomToast(message ?: "Tidak ada pesan", ToastType.INFO)
                            }
                        }) { Text("Ya, Tarik Data") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSyncDialog = false }) { Text("Batal") }
                    }
                )
            }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
                        item {
                            ListItem(
                                headlineContent = { Text("Dark Mode", style = MaterialTheme.typography.titleMedium) },
                                trailingContent = {
                                    Switch(
                                        checked = isDarkMode,
                                        onCheckedChange = { viewModel.setDarkMode(it) }
                                    )
                                }
                            )
                            HorizontalDivider()
                        }
                

                item {
                    ListItem(
                        headlineContent = { Text("Sinkronisasi Manual", style = MaterialTheme.typography.titleMedium) },
                        supportingContent = { Text("Tarik data terbaru dari Spreadsheet") },
                        modifier = Modifier.clickable { if (!isSyncing) showSyncDialog = true },
                        trailingContent = {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    )
                    HorizontalDivider()
                }

                item {
                    ListItem(
                        headlineContent = { Text("Kelola Rekening", style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.clickable { expandedSection = if (expandedSection == "REKENING") null else "REKENING" },
                        trailingContent = {
                            Icon(
                                if (expandedSection == "REKENING") androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    )
                }
                
                if (expandedSection == "REKENING") {
                    item {
                        Button(
                            onClick = {
                                editingAccount = null
                                showAccountDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tambah Rekening Baru")
                        }
                    }
                    items(accounts) { account ->
                        AccountItem(
                            account = account,
                            onEdit = {
                                editingAccount = account
                                showAccountDialog = true
                            },
                            onDelete = { viewModel.deleteAccount(account) }
                        )
                    }
                }

                item { HorizontalDivider() }

                item {
                    ListItem(
                        headlineContent = { Text("Kelola Kategori", style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.clickable { expandedSection = if (expandedSection == "KATEGORI") null else "KATEGORI" },
                        trailingContent = {
                            Icon(
                                if (expandedSection == "KATEGORI") androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    )
                }

                if (expandedSection == "KATEGORI") {
                    item {
                        Button(
                            onClick = {
                                editingCategory = null
                                showCategoryDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tambah Kategori Baru")
                        }
                    }
                    items(categories) { category ->
                        CategoryItem(
                            category = category,
                            onEdit = {
                                editingCategory = category
                                showCategoryDialog = true
                            },
                            onDelete = { viewModel.deleteCategory(category) }
                        )
                    }
                }
                
                item { HorizontalDivider() }
            }

        if (showAccountDialog) {
            AccountDialog(
                account = editingAccount,
                onDismiss = { showAccountDialog = false },
                onSave = { name, balance ->
                    if (editingAccount == null) {
                        viewModel.addAccount(name, balance)
                    } else {
                        viewModel.updateAccount(editingAccount!!.copy(name = name, balance = balance))
                    }
                    showAccountDialog = false
                }
            )
        }

        if (showCategoryDialog) {
            CategoryDialog(
                category = editingCategory,
                onDismiss = { showCategoryDialog = false },
                onSave = { name, type ->
                    if (editingCategory == null) {
                        viewModel.addCategory(name, type)
                    } else {
                        viewModel.updateCategory(editingCategory!!.copy(name = name, type = type))
                    }
                    showCategoryDialog = false
                }
            )
        }
    }
}

@Composable
fun AccountItem(account: AccountEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) CardBackgroundGlassDark else CardBackgroundGlass)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun CategoryItem(category: CategoryEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) CardBackgroundGlassDark else CardBackgroundGlass)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(category.name, style = MaterialTheme.typography.titleMedium)
                Text(if (category.type == "INCOME") "Pemasukan" else "Pengeluaran", style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDialog(account: AccountEntity?, onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    val balance = account?.balance ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "Tambah Rekening" else "Edit Rekening") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Rekening") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(name, balance)
                }
            }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDialog(category: CategoryEntity?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var isIncome by remember { mutableStateOf(category?.type == "INCOME") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Tambah Kategori" else "Edit Kategori") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kategori") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isIncome, onCheckedChange = { isIncome = it })
                    Text("Ini adalah kategori Pemasukan")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(name, if (isIncome) "INCOME" else "EXPENSE")
                }
            }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
