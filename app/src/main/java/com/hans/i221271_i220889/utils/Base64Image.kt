package com.hans.i221271_i220889.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object Base64Image {
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 70): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun uriToBase64(context: Context, uri: Uri, quality: Int = 70): String? {
        val src = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
            decoder.isMutableRequired = false
        }
        return bitmapToBase64(bitmap, quality)
    }

    fun base64ToBitmap(base64: String): Bitmap {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}


