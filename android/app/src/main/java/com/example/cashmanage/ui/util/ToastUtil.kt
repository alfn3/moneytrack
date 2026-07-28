package com.example.cashmanage.ui.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import com.example.cashmanage.R

enum class ToastType(@DrawableRes val iconRes: Int, val backgroundRes: Int) {
    SUCCESS(R.drawable.ic_toast_icon, R.drawable.toast_background),
    ERROR(R.drawable.ic_toast_icon, R.drawable.toast_background),
    INFO(R.drawable.ic_toast_icon, R.drawable.toast_background)
}

fun Context.showCustomToast(message: String, type: ToastType = ToastType.INFO) {
    val inflater = LayoutInflater.from(this)
    val layout: View = inflater.inflate(R.layout.custom_toast, null)
    val text = layout.findViewById<TextView>(R.id.toast_text)
    val icon = layout.findViewById<ImageView>(R.id.toast_icon)
    text.text = message
    icon.setImageResource(type.iconRes)
    // background already set in layout xml via toast_background drawable
    val toast = Toast(this)
    toast.duration = Toast.LENGTH_SHORT
    toast.view = layout
    toast.show()
}
