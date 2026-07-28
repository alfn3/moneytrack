package com.example.cashmanage.ai

import android.content.Context
import android.content.SharedPreferences


class AILearningManager(
    context: Context
) {

    private val prefs =
        context.getSharedPreferences(
            "ai_learning",
            Context.MODE_PRIVATE
        )


    fun recordCorrection(
        notes:String,
        oldCategoryId:Int,
        newCategoryId:Int,
        oldAccountId:Int,
        newAccountId:Int
    ){

        if(notes.isBlank()) return


        if(
            oldCategoryId == newCategoryId &&
            oldAccountId == newAccountId
        ){
            return
        }


        val rules =
            prefs.getStringSet(
                "rules",
                emptySet()
            )?.toMutableSet()
                ?: mutableSetOf()


        val rule =
            """
            Keyword: $notes
            Category: $newCategoryId
            Account: $newAccountId
            """.trimIndent()


        rules.add(rule)


        val limited =
            rules
                .takeLast(AIConfig.MAX_LEARNING_RULES)
                .toSet()


        prefs.edit()
            .putStringSet(
                "rules",
                limited
            )
            .apply()
    }



    fun getLearningContext():String{


        val rules =
            prefs.getStringSet(
                "rules",
                emptySet()
            )


        if(rules.isNullOrEmpty())
            return ""


        return """
        
        Preferensi pengguna sebelumnya:
        
        ${rules.joinToString("\n")}
        
        Gunakan hanya jika sesuai konteks.
        
        """.trimIndent()
    }

}