package com.example.cashmanage.ai

import android.content.Context
import android.content.SharedPreferences

class AILearningManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_learning_prefs", Context.MODE_PRIVATE)
    
    fun recordCorrection(notes: String?, oldCategoryId: Int, newCategoryId: Int, oldAccountId: Int, newAccountId: Int) {
        if (notes.isNullOrBlank()) return
        
        val currentRules = prefs.getStringSet("learning_rules", mutableSetOf()) ?: mutableSetOf()
        
        var ruleText = "Jika catatan (notes) mengandung kata '$notes', maka"
        if (oldCategoryId != newCategoryId) {
            ruleText += " gunakan category_id $newCategoryId (Bukan $oldCategoryId)."
        }
        if (oldAccountId != newAccountId) {
            ruleText += " gunakan accountId $newAccountId (Bukan $oldAccountId)."
        }
        
        if (oldCategoryId != newCategoryId || oldAccountId != newAccountId) {
            currentRules.add(ruleText)
            prefs.edit().putStringSet("learning_rules", currentRules).apply()
        }
        prefs.edit().putStringSet("learning_rules", currentRules).apply()
    }
    
    fun getLearningRules(): String {
        val rules = prefs.getStringSet("learning_rules", emptySet()) ?: emptySet()
        if (rules.isEmpty()) return ""
        
        return "\n\nATURAN TAMBAHAN BERDASARKAN PREFERENSI PENGGUNA (PENTING):\n" + rules.joinToString("\n") { "- $it" }
    }
}
