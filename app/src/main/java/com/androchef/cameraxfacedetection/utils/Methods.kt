package com.androchef.cameraxfacedetection.utils


import android.os.Environment
import android.util.Log
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


object Methods {
    const val TAG = "Methods"
    private const val FACE_PICS = "FacePics"
    private const val IMG_WIDTH = 92
    private const val IMG_HEIGHT = 112
    private val ROOT = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        FACE_PICS
    )

    //Method for deleting all data from FacePics
    fun reset() {
        val facePicsPath = File(ROOT.toString())
        if (facePicsPath.exists()) {
            val facePicsArray = facePicsPath.listFiles()
            for (facepicsTmp in facePicsArray) {
                facepicsTmp.delete()
            }
        }
    }
    //Method for capturing photos
    @Throws(Exception::class)
    fun takePhoto( rgbaMat: Mat?, faceDetector: CascadeClassifier):File {
        var savePhoto = File("")
        val facePicsPath = File(ROOT.toString())
        if (facePicsPath.exists() && !facePicsPath.isDirectory) facePicsPath.delete()
        if (!facePicsPath.exists()) facePicsPath.mkdirs()
        val grayMat = Mat()
        //Converting RGBA to GRAY
        Imgproc.cvtColor(rgbaMat, grayMat, Imgproc.COLOR_RGBA2GRAY)
        val detectedFaces = MatOfRect()
        faceDetector.detectMultiScale(grayMat, detectedFaces)
        val detectedFacesArray = detectedFaces.toArray()
        for (face in detectedFacesArray) {
            val capturedFace = Mat(grayMat, face)
            //Resizing to 92x112
            Imgproc.resize(
                capturedFace, capturedFace, Size(
                    IMG_WIDTH.toDouble(),
                    IMG_HEIGHT.toDouble()
                )
            )
            //Histogram equalizing

            Imgproc.equalizeHist(capturedFace, capturedFace)
                 savePhoto = File(facePicsPath, String.format("%d.png", SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(
                    Date()
                )))
                savePhoto.createNewFile()
                //Saving photos to directory FacePics
                Imgcodecs.imwrite(savePhoto.absolutePath, capturedFace)
                Log.i(TAG, "PIC PATH: " + savePhoto.absolutePath)

            }
        return savePhoto.absoluteFile
        }
    }
