package com.example.cashmanage.data.repository

import android.content.Context
import androidx.work.*
import com.example.cashmanage.data.db.AppDatabase
import com.example.cashmanage.data.db.TransactionEntity
import com.example.cashmanage.worker.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID


class AITransactionRepository(
    private val context: Context
) {


    private val database =
        AppDatabase.getDatabase(context)



    suspend fun insertTransaction(

        accountId:Int,

        categoryId:Int,

        amount:Double,

        type:String,

        notes:String

    ){


        withContext(Dispatchers.IO){


            val transaction =
                TransactionEntity(

                    id = 0,

                    accountId = accountId,

                    categoryId = categoryId,

                    amount = amount,

                    type = type,

                    notes = notes,

                    createdAt =
                    System.currentTimeMillis(),

                    syncStatus = false

                )



            database
                .transactionDao()
                .insert(transaction)



        }



        triggerSync()


    }





    private fun triggerSync(){


        val request =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(

                    Constraints.Builder()

                        .setRequiredNetworkType(
                            NetworkType.CONNECTED
                        )

                        .build()

                )

                .setId(
                    UUID.randomUUID()
                )

                .build()



        WorkManager
            .getInstance(context)
            .enqueue(request)


    }



}