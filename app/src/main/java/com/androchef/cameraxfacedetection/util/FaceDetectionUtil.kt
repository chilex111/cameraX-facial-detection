package com.androchef.cameraxfacedetection.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.SparseArray
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector

private lateinit var thisFace: Face

 fun Context.detectFace(imageUri: Uri): Bitmap? {
    val imageBitmap = if (Build.VERSION.SDK_INT < 28) {
        MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
    } else {
        val source =
            ImageDecoder.createSource(contentResolver, imageUri)
        ImageDecoder.decodeBitmap(source)
    }
    val bitmap = imageBitmap.copy(
        Bitmap.Config.ARGB_8888,
        true
    )//   `.resizeWithoutDistortion()// convert image from hardware bitmap to software bitmap :(
    val paint = Paint()
    paint.strokeWidth = 6f
    paint.color = Color.RED
    paint.style = Paint.Style.STROKE
    val tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
    val canvas = Canvas(tempBitmap!!)
    canvas.drawBitmap(bitmap, 0f, 0f, null)
    val faceDetector: FaceDetector = FaceDetector.Builder(this).setTrackingEnabled(false)
        .build()
    if (!faceDetector.isOperational) {
        AlertDialog.Builder(this).setMessage("Could not set up Face Detector!").show()
        return null
    }
    val frame: Frame = Frame.Builder().setBitmap(bitmap).build()
    val faces: SparseArray<Face> = faceDetector.detect(frame)
    for (i in 0 until faces.size()) {
        thisFace = faces.valueAt(i)
        val x1: Float = thisFace.position.x
        val y1: Float = thisFace.position.y
        val x2: Float = x1 + thisFace.width
        val y2: Float = y1 + thisFace.height
        canvas.drawRoundRect(RectF(x1, y1, x2, y2), 2f, 2f, paint)
    }

    return Bitmap.createBitmap(
         tempBitmap,
         thisFace.position.x.toInt(),
         thisFace.position.y.toInt(),
         thisFace.width.toInt(),
         thisFace.height.toInt()
     )
}