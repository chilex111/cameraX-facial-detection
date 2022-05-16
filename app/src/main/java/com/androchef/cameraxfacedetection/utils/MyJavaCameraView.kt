package com.androchef.cameraxfacedetection.utils

import android.content.Context
import android.util.AttributeSet
import com.google.android.gms.vision.CameraSource
import org.opencv.android.JavaCameraView
import java.io.IOException

class MyJavaCameraView(context: Context?, attrs: AttributeSet?): JavaCameraView(context, attrs),CameraSource.PictureCallback {

    override fun onPictureTaken(p0: ByteArray?) {
        TODO("Not yet implemented")
    }
    fun takePicture( fileName:String){

    }
}
