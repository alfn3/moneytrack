package com.example.cashmanage.ui.quickrecord

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.example.cashmanage.service.QuickRecordService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OcrCameraActivity : ComponentActivity() {

    private var currentPhotoUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val photoUri = currentPhotoUri
            if (photoUri != null) {
                com.example.cashmanage.util.UIUtils.showCustomToast(this, "Memproses struk di latar belakang...")
                val serviceIntent = Intent(this, QuickRecordService::class.java).apply {
                    action = "PROCESS_OCR"
                    putExtra("photo_uri", photoUri.toString())
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        } else {
            com.example.cashmanage.util.UIUtils.showCustomToast(this, "Batal mengambil foto")
        }
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            val photoFile = createImageFile()
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                photoFile
            )
            takePictureLauncher.launch(currentPhotoUri!!)
        } catch (e: Exception) {
            e.printStackTrace()
            com.example.cashmanage.util.UIUtils.showCustomToast(this, "Gagal membuka kamera: ${e.message}")
            finish()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = externalCacheDir ?: cacheDir
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
}
