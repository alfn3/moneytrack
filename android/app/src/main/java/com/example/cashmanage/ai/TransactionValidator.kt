package com.example.cashmanage.ai


import com.example.cashmanage.data.model.TransactionDraft


object TransactionValidator {


    fun validate(
        data:TransactionDraft
    ):Boolean{


        if(data.amount<=0)
            return false


        if(data.categoryId<=0)
            return false


        if(data.accountId<=0)
            return false


        return true
    }

}