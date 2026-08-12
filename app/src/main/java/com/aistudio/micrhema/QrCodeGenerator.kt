package com.aistudio.micrhema

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.util.EnumMap

private const val QR_CODE_SIZE = 700

fun generateQrCode(content: String): ImageBitmap? {
    if (!content.startsWith("https://")) {
        return null
    }

    return try {
        val hints = EnumMap<EncodeHintType, Any>(
            EncodeHintType::class.java
        ).apply {
            put(EncodeHintType.MARGIN, 2)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }

        val bitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            QR_CODE_SIZE,
            QR_CODE_SIZE,
            hints
        )

        val bitmap = Bitmap.createBitmap(
            bitMatrix.width,
            bitMatrix.height,
            Bitmap.Config.ARGB_8888
        )

        for (x in 0 until bitMatrix.width) {
            for (y in 0 until bitMatrix.height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) {
                        android.graphics.Color.BLACK
                    } else {
                        android.graphics.Color.WHITE
                    }
                )
            }
        }

        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
