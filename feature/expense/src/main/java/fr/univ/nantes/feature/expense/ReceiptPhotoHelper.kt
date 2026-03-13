package fr.univ.nantes.feature.expense

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility helpers for creating receipt files and checking basic photo quality.
 */
object ReceiptPhotoHelper {

    /** Create a unique file in app-specific Pictures/receipts directory. */
    @Throws(IOException::class)
    fun createReceiptFile(context: Context, groupId: Long? = null): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "RECEIPT_${groupId ?: "G"}_$timeStamp.jpg"
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        val receiptsDir = File(picturesDir, "receipts")
        if (!receiptsDir.exists()) receiptsDir.mkdirs()
        val file = File(receiptsDir, fileName)
        file.createNewFile()
        return file
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Heuristic quality check: requires minimum resolution and non-trivial file size.
     * width >= 800 && height >= 600 && file.length() >= 20_000
     */
    fun isPhotoQualityAcceptable(file: File): Boolean {
        if (!file.exists()) return false
        val length = file.length()
        if (length < 20_000) return false
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, options)
        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) return false
        return width >= 800 && height >= 600
    }
}
