package com.example.cashmanage.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_learning_rules")
data class AILearningRuleEntity(

    @PrimaryKey(autoGenerate = true)
    val id:Int = 0,

    val keyword:String,

    val categoryId:Int,

    val accountId:Int,

    val frequency:Int = 1,

    val confidence:Float = 1f,

    val lastUsed:Long = System.currentTimeMillis()

)