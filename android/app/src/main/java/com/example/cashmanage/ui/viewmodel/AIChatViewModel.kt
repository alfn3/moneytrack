package com.example.cashmanage.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cashmanage.ai.AILearningManager
import com.example.cashmanage.ai.GeminiService
import com.example.cashmanage.ai.TransactionValidator
import com.example.cashmanage.data.model.TransactionDraft
import com.example.cashmanage.data.repository.AITransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val bitmap: Bitmap? = null
)


data class AIChatState(

    val messages: List<ChatMessage> = emptyList(),

    val isLoading: Boolean = false,

    val pendingTransaction: TransactionDraft? = null,

    val error: String? = null

)



class AIChatViewModel(
    application: Application
) : AndroidViewModel(application) {



    private val context =
        application.applicationContext



    private val geminiService =
        GeminiService()



    private val learningManager =
        AILearningManager(
            context
        )



    private val transactionRepository =
        AITransactionRepository(
            context
        )




    private val _state =
        MutableStateFlow(
            AIChatState()
        )


    val state: StateFlow<AIChatState> =
        _state





    fun sendMessage(

        text: String,

        image: Bitmap? = null

    ) {



        val currentMessages =
            _state.value.messages
                .toMutableList()



        currentMessages.add(

            ChatMessage(

                text = text,

                isUser = true,

                bitmap = image

            )

        )



        _state.value =
            _state.value.copy(

                messages = currentMessages,

                isLoading = true,

                error = null

            )





        viewModelScope.launch {



            try {



                val draft =

                    geminiService
                        .parseTransaction(

                            text = text,

                            image = image,

                            learning =
                            learningManager
                                .getLearningContext()

                        )




                if(

                    draft != null &&

                    TransactionValidator
                        .validate(draft)

                ){



                    _state.value =
                        _state.value.copy(

                            pendingTransaction = draft,

                            isLoading = false

                        )



                    addAssistantMessage(

                        """
                        Saya menemukan transaksi:

                        Jenis:
                        ${draft.type}

                        Nominal:
                        Rp ${formatMoney(draft.amount)}

                        Catatan:
                        ${draft.notes}

                        Silakan konfirmasi sebelum menyimpan.
                        """.trimIndent()

                    )



                }else{



                    addAssistantMessage(

                        "Maaf, saya belum dapat memahami transaksi tersebut."

                    )

                }




            }catch(e:Exception){



                _state.value =
                    _state.value.copy(

                        error =
                        e.message,

                        isLoading = false

                    )


                addAssistantMessage(

                    "Terjadi kesalahan: ${e.message}"

                )


            }



        }



    }







    fun confirmTransaction(){



        val transaction =

            _state.value.pendingTransaction

                ?: return





        viewModelScope.launch {



            try {



                transactionRepository
                    .insertTransaction(


                        accountId =
                        transaction.accountId,


                        categoryId =
                        transaction.categoryId,


                        amount =
                        transaction.amount,


                        type =
                        transaction.type,


                        notes =
                        transaction.notes


                    )





                addAssistantMessage(

                    """
                    ✅ Transaksi berhasil disimpan

                    ${transaction.type}

                    Rp ${formatMoney(transaction.amount)}

                    ${transaction.notes}

                    Data akan disinkronkan otomatis.
                    """.trimIndent()

                )





                _state.value =
                    _state.value.copy(

                        pendingTransaction = null

                    )





            }catch(e:Exception){



                addAssistantMessage(

                    "Gagal menyimpan transaksi: ${e.message}"

                )


            }


        }


    }







    fun cancelTransaction(){



        _state.value =
            _state.value.copy(

                pendingTransaction = null

            )



        addAssistantMessage(

            "Transaksi dibatalkan."

        )


    }









    fun clearError(){


        _state.value =
            _state.value.copy(

                error = null

            )


    }









    private fun addAssistantMessage(

        message:String

    ){



        val list =

            _state.value.messages
                .toMutableList()



        list.add(

            ChatMessage(

                text = message,

                isUser = false

            )

        )



        _state.value =
            _state.value.copy(

                messages = list,

                isLoading = false

            )


    }







    private fun formatMoney(

        value:Double

    ):String{


        return String
            .format(
                "%,.0f",
                value
            )
            .replace(
                ",",
                "."
            )

    }



}