package com.example.cashmanage.util

class FormulaEngine {
    fun evaluate(formula: String, contextCells: Map<String, Double>): Double? {
        if (!formula.startsWith("=")) return null
        
        val expr = formula.substring(1).trim().uppercase()
        
        // Handle basic SUM(A1:A5)
        if (expr.startsWith("SUM(") && expr.endsWith(")")) {
            val range = expr.removePrefix("SUM(").removeSuffix(")")
            val parts = range.split(":")
            if (parts.size == 2) {
                // Simplified evaluation for demonstration
                // A complete spreadsheet parser would iterate rows/cols
                val start = parts[0]
                val end = parts[1]
                var sum = 0.0
                contextCells.forEach { (ref, value) ->
                    // Extremely simplified check: just checks if ref is between start and end lexicographically
                    if (ref in start..end) {
                        sum += value
                    }
                }
                return sum
            }
        }
        
        // Handle basic arithmetic like =A1+B2
        val tokens = expr.split(Regex("(?<=[-+*/])|(?=[-+*/])"))
        if (tokens.size == 3) {
            val left = contextCells[tokens[0]] ?: tokens[0].toDoubleOrNull() ?: 0.0
            val right = contextCells[tokens[2]] ?: tokens[2].toDoubleOrNull() ?: 0.0
            return when (tokens[1]) {
                "+" -> left + right
                "-" -> left - right
                "*" -> left * right
                "/" -> if (right != 0.0) left / right else 0.0
                else -> null
            }
        }
        return null
    }
}
