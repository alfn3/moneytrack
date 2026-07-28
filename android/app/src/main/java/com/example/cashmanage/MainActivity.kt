package com.example.cashmanage

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.cashmanage.theme.CashManageTheme
import com.example.cashmanage.ui.screens.*
import com.example.cashmanage.service.QuickRecordService
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.cashmanage.theme.BottomNavActive
import com.example.cashmanage.theme.BottomNavInactive
import com.example.cashmanage.ui.util.showCustomToast
import com.example.cashmanage.ui.util.ToastType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startQuickRecordService()
        }
    }

    private val intentActionState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startQuickRecordService()
        }
        
        intentActionState.value = intent.action

        enableEdgeToEdge()
        setContent {
            val viewModel: com.example.cashmanage.ui.viewmodel.FinancialViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            CashManageTheme(darkTheme = isDarkMode) { 
                MainAppScreen(viewModel, intentActionState.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentActionState.value = intent.action
    }

    private fun startQuickRecordService() {
        val serviceIntent = Intent(this, QuickRecordService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: com.example.cashmanage.ui.viewmodel.FinancialViewModel = viewModel(),
    intentAction: String? = null
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    
    val prefs = context.getSharedPreferences("cashmanage_prefs", Context.MODE_PRIVATE)
    val spreadsheetId = prefs.getString("spreadsheet_id", "") ?: ""

    val openUrl = { url: String ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    var showInitialSyncPopup by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val hasShownInitialSync = prefs.getBoolean("has_shown_initial_sync", false)
        if (!hasShownInitialSync && spreadsheetId.isNotEmpty() && intentAction != "OPEN_OCR" && intentAction != "OPEN_VN") {
            showInitialSyncPopup = true
        }
    }

    LaunchedEffect(intentAction) {
        if (intentAction == "OPEN_OCR") {
            navController.navigate("ocr")
        }
    }

    if (showInitialSyncPopup) {
        AlertDialog(
            onDismissRequest = {
                showInitialSyncPopup = false
                prefs.edit().putBoolean("has_shown_initial_sync", true).apply()
            },
            title = { Text("Sinkronisasi Otomatis") },
            text = { Text("Apakah Anda ingin menarik data terbaru dari Spreadsheet sekarang?") },
            confirmButton = {
                TextButton(onClick = {
                    isSyncing = true
                    viewModel.pullDataFromSpreadsheet { success, message ->
                        isSyncing = false
                        showInitialSyncPopup = false
                        prefs.edit().putBoolean("has_shown_initial_sync", true).apply()
                        context.showCustomToast(message ?: "Tidak ada pesan", ToastType.INFO)
                    }
                }, enabled = !isSyncing) { 
                    Text(if (isSyncing) "Tunggu..." else "Ya, Tarik Data") 
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showInitialSyncPopup = false
                    prefs.edit().putBoolean("has_shown_initial_sync", true).apply()
                }) { Text("Nanti Saja") }
            }
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Triple("Beranda", Icons.Default.Dashboard, "dashboard"),
        Triple("Riwayat", Icons.Default.List, "transactions"),
        Triple("Catat", Icons.Default.AddCircle, "chat"), 
        Triple("Rekap", Icons.Default.AccountBalanceWallet, "budget"),
        Triple("Profil", Icons.Default.Person, "profile")
    )

    Scaffold(
        bottomBar = {
            if (currentRoute != "login") {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    bottomNavItems.forEach { (label, icon, route) ->
                        val isSelected = currentRoute == route
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BottomNavActive,
                                unselectedIconColor = BottomNavInactive,
                                selectedTextColor = BottomNavActive,
                                unselectedTextColor = BottomNavInactive
                            ),
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo("dashboard") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = MaterialTheme.colorScheme.background) { 
            NavHost(
                navController = navController, 
                startDestination = "login",
                enterTransition = { fadeIn(tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)) },
                exitTransition = { fadeOut(tween(300)) + scaleOut(targetScale = 0.9f, animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)) },
                popExitTransition = { fadeOut(tween(300)) + scaleOut(targetScale = 0.9f, animationSpec = tween(300)) }
            ) {
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }
                composable("dashboard") { 
                    DashboardScreen(
                        onNavigateToOcr = { navController.navigate("ocr") },
                        onNavigateToChat = { navController.navigate("chat") },
                        onNavigateToTransactions = { navController.navigate("transactions") },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        viewModel = viewModel
                    ) 
                }
                composable("ocr") { OcrScanScreen(onBack = { navController.popBackStack() }) }
                composable("chat") { ChatAdvisorScreen(onBack = { navController.popBackStack() }) }
                composable("saving_goals") { SavingGoalsScreen(onBack = { navController.popBackStack() }, viewModel = viewModel) }
                composable("budget") { BudgetScreen(onBack = { navController.popBackStack() }, viewModel = viewModel) }
                composable("profile") { ProfileScreen(onBack = { navController.popBackStack() }, navController = navController) }
                composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }, viewModel = viewModel) }
                composable("help") { HelpScreen(onBack = { navController.popBackStack() }) }
                composable("transactions") { TransactionHistoryScreen(onBack = { navController.popBackStack() }, viewModel = viewModel) }
            }
        } 
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panduan Penggunaan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val guideItems = listOf(
            "1. DATA MASTER" to "a. Isi sheet 'REKENING' dengan daftar rekening/e-wallet yang Anda gunakan. Jangan lupa input saldo awalnya.\nb. Isi sheet 'KATEGORI TRANSAKSI' jika ada tambahan atau penyesuaian kategori pemasukan/pengeluaran.\nc. Isi sheet 'BUDGETING TAHUNAN' untuk merencanakan target limit pengeluaran per kategori setiap bulannya.",
            "2. MENCATAT TRANSAKSI" to "a. Gunakan aplikasi ini atau sheet 'TRANSAKSI' untuk mencatat setiap pemasukan, pengeluaran, atau transfer.\nb. Pilih Kategori dan Rekening dari dropdown.\nc. Isi nominal pada kolom Pemasukan atau Pengeluaran. Saldo akan terupdate otomatis.",
            "3. SAVING GOALS" to "a. Tulis target tabungan Anda di sheet 'SAVING GOALS' (misal: Beli Laptop Baru, Liburan).\nb. Tentukan target nominal dan batas waktunya.\nc. Tambahkan tabungan dengan mencatat kategori 'Saving Goals'.",
            "4. DASHBOARD" to "a. Pantau kondisi keuangan Anda di sheet 'DASHBOARD'.\nb. Anda bisa melihat total saldo saat ini, sisa budget, dan perbandingan pemasukan vs pengeluaran.\nc. Grafik dan tabel otomatis terupdate.",
            "TIPS TAMBAHAN" to "- Jangan mengubah rumus di kolom yang berwarna gelap pada sheet.\n- Lakukan pencatatan secara rutin.\n- Selalu cocokkan Total Saldo Aktual dengan saldo asli di rekening/e-wallet Anda."
        )

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            items(guideItems.size) { index ->
                val (title, description) = guideItems[index]
                var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                
                Card(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Icon(
                                imageVector = if (expanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                                contentDescription = "Expand/Collapse"
                            )
                        }
                        
                        if (expanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
