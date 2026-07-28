package com.example.cashmanage.ai


import android.graphics.Bitmap
import com.example.cashmanage.BuildConfig
import com.example.cashmanage.data.model.TransactionDraft
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.*


class GeminiService {


    suspend fun parseTransaction(
        text:String,
        image:Bitmap?,
        learning:String
    ):TransactionDraft? {


        val function =
            defineFunction(
                name="record_transaction",
                description=
                "Extract financial transaction",
                parameters=listOf(

                    Schema(
                        name="amount",
                        description="Nominal",
                        type=FunctionType.NUMBER
                    ),

                    Schema(
                        name="category_id",
                        description="Category ID",
                        type=FunctionType.INTEGER
                    ),

                    Schema(
                        name="account_id",
                        description="Account ID",
                        type=FunctionType.INTEGER
                    ),

                    Schema(
                        name="type",
                        description="INCOME or EXPENSE",
                        type=FunctionType.STRING
                    ),

                    Schema(
                        name="notes",
                        description="Transaction note",
                        type=FunctionType.STRING
                    )

                )
            )


        val prompt =
            """
            Anda adalah AI pencatat transaksi.
            
            Kategori:
            1 Kebutuhan
            2 Makanan
            3 Transportasi
            4 Tagihan
            5 Edukasi
            6 Kesehatan
            7 Hiburan
            8 Belanja
            9 Investasi
            10 Gaji
            11 Pendapatan
            
            Account:
            1 Rekening Utama
            2 Tabungan
            3 Ewallet
            4 Tunai
            
            Jika rekening tidak disebut:
            gunakan 4
            
            $learning
            
            Input:
            $text
            
            """.trimIndent()



        val model =
            GenerativeModel(
                modelName =
                AIConfig.MODEL_NAME,
                apiKey =
                BuildConfig.GEMINI_API_KEY,
                systemInstruction =
                content {
                    text(prompt)
                },
                tools =
                listOf(
                    Tool(
                        listOf(function)
                    )
                )
            )


        val response =
            if(image!=null){

                model.generateContent(
                    content {

                        image(image)

                        text(prompt)

                    }
                )

            }else{

                model.generateContent(text)

            }



        val call =
            response.functionCall
                ?: return null



        if(call.name!="record_transaction")
            return null



        return TransactionDraft(

            amount =
            call.args["amount"]
                ?.toString()
                ?.toDoubleOrNull()
                ?:0.0,


            categoryId =
            call.args["category_id"]
                ?.toString()
                ?.toInt()
                ?:1,


            accountId =
            call.args["account_id"]
                ?.toString()
                ?.toInt()
                ?:AIConfig.DEFAULT_ACCOUNT_ID,


            type =
            call.args["type"]
                ?.toString()
                ?: "EXPENSE",


            notes =
            call.args["notes"]
                ?.toString()
                ?: ""

        )

    }

}