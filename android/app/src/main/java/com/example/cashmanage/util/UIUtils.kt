package com.example.cashmanage.util

import android.content.Context
import android.media.MediaPlayer
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.example.cashmanage.R

object UIUtils {
    
    fun showCustomToast(context: Context, message: String) {
        try {
            val inflater = LayoutInflater.from(context)
            val layout = inflater.inflate(R.layout.custom_toast, null)
            val text = layout.findViewById<TextView>(R.id.toast_text)
            text.text = message
            
            val toast = Toast(context.applicationContext)
            toast.duration = Toast.LENGTH_LONG
            toast.view = layout
            toast.show()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to standard toast if custom toast fails
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
