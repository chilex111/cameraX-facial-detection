package com.androchef.cameraxfacedetection.listener

import android.graphics.Bitmap
import android.net.Uri

interface BitmapListener {
    fun cameraCaptureBitmapResult(value: Bitmap)
}