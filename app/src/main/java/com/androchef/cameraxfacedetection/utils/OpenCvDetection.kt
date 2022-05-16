package com.androchef.cameraxfacedetection.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.androchef.cameraxfacedetection.R
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class OpenCvDetection(private val context: Context) {


    var faceDetector: CascadeClassifier? = null
    lateinit var faceDir: File

    private lateinit var face: Rect

     fun detectFaceOpenCV(src: Mat):Bitmap? {
        // Detecting the face in the snap
         try {
             val faceDetections = MatOfRect()
             faceDetector?.detectMultiScale(src, faceDetections)
             println(
                 String.format(
                     "Detected %s faces",
                     faceDetections.toArray().size
                 )
             )
             // Drawing boxes
             for (rect in faceDetections.toArray()) {
                 face = rect
                 Imgproc.rectangle(
                     src,  // where to draw the box
                     Point(rect.x.toDouble(), rect.y.toDouble()),  // bottom left
                     Point(
                         rect.x + rect.width.toDouble(),
                         rect.y + rect.height.toDouble()
                     ),  // top right
                     Scalar(0.0, 0.0, 255.0),
                     1 // RGB colour
                 )
             }

             val tempsBitmap = convertMatToBitMap(src)
             if (tempsBitmap != null ) {
                 return Bitmap.createBitmap(
                         tempsBitmap,
                         face.x,
                         face.y,
                         face.width,
                         face.height)
             }
         }catch (e: Exception){
             Log.e("TRY_CATCH_ERROR", e.message.toString())
         }
         return null
    }
     fun convertMatToBitMap(input: Mat): Bitmap? {
        var bmp: Bitmap? = null
        val rgb = Mat()
        Imgproc.cvtColor(input, rgb, Imgproc.COLOR_BGR2RGB)
        try {
            bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(rgb, bmp)
        } catch (e: CvException) {
            Log.d("Exception", e.message!!)
        }
        return bmp
    }
     fun loadFaceLib() {
        try {
            val modelInputStream =
                context.resources.openRawResource(
                    R.raw.haarcascade_frontalface_alt2
                )

            // create a temp directory
            faceDir = context.getDir(FACE_DIR, Context.MODE_PRIVATE)

            // create a model file
            val faceModel = File(faceDir, FACE_MODEL)

            if (!faceModel.exists()) { // copy model
                // copy model to new face library
                val modelOutputStream = FileOutputStream(faceModel)

                val buffer = ByteArray(byteSize)
                var byteRead = modelInputStream.read(buffer)
                while (byteRead != -1) {
                    modelOutputStream.write(buffer, 0, byteRead)
                    byteRead = modelInputStream.read(buffer)
                }

                modelInputStream.close()
                modelOutputStream.close()
            }

            faceDetector = CascadeClassifier(faceModel.absolutePath)
        } catch (e: IOException) {
            Log.e("Error ", "loading cascade face model...$e")
        }
    }

    companion object {
        // Face model
        private const val FACE_DIR = "facelib"
        private const val FACE_MODEL = "haarcascade_frontalface_alt2.xml"
        private const val byteSize = 4096 // buffer size
        var tempBitmap: Bitmap? = null
    }
}