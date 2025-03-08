package com.example.show_your_body

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.appinmotion.gpuimage.YuvToRgbConverter

@ExperimentalGetImage
fun ImageProxy.toBitmap(context: Context): Bitmap? {
    val mediaImage = this.image ?: return null
    val converter = YuvToRgbConverter(context)
    val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    converter.yuvToRgb(mediaImage, outBitmap)
    return outBitmap
}