package com.androchef.cameraxfacedetection.listener

import android.net.Uri

interface ResultListener {
    fun cameraCaptureResult(value: Uri)
}