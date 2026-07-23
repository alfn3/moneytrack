package com.example.cashmanage.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cashmanage.util.FormulaEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetGridScreen(onBack: () -> Unit) {
    val formulaEngine = remember { FormulaEngine() }
    
    // In a real implementation, this would be backed by Room DB Cells
    var cells by remember { mutableStateOf(mapOf(
        "A1" to "1000",
        "A2" to "2000",
        "A3" to "=SUM(A1:A2)"
    )) }
    
    var computedCells by remember { mutableStateOf(mapOf<String, Double>()) }
    
    // Basic reactive re-evaluation
    LaunchedEffect(cells) {
        val newComputed = mutableMapOf<String, Double>()
        // First pass: numbers
        cells.forEach { (ref, value) ->
            if (!value.startsWith("=")) {
                value.toDoubleOrNull()?.let { newComputed[ref] = it }
            }
        }
        // Second pass: formulas
        cells.forEach { (ref, value) ->
            if (value.startsWith("=")) {
                val res = formulaEngine.evaluate(value, newComputed)
                if (res != null) {
                    newComputed[ref] = res
                }
            }
        }
        computedCells = newComputed
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Internal Spreadsheet") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn {
                items(10) { rowIndex ->
                    LazyRow {
                        items(5) { colIndex ->
                            val colChar = ('A' + colIndex).toString()
                            val ref = "$colChar${rowIndex + 1}"
                            val displayValue = computedCells[ref]?.toString() ?: cells[ref] ?: ""
                            
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(40.dp)
                                    .border(1.dp, Color.Gray)
                            ) {
                                TextField(
                                    value = displayValue,
                                    onValueChange = { newVal ->
                                        cells = cells.toMutableMap().apply { put(ref, newVal) }
                                    },
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxSize(),
                                    colors = TextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
