package com.androchef.cameraxfacedetection

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androchef.cameraxfacedetection.listener.BitmapListener
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.activity_gallery_image.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface.SUCCESS
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class CameraActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    lateinit var faceDir: File
    private var mAbsoluteFaceSize = 0.0
    private var takePhoto = false

    private var mFaceDetector: CascadeClassifier? = null

    private lateinit var imageMat: Mat
    private lateinit var grayMat: Mat
    private var isFront:Boolean =true
    private var mCameraId = 1

    private lateinit var cameraBridgeViewBase: CameraBridgeViewBase
    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.i(
                        TAG,
                        "OpenCV loaded successfully"
                    )
                    loadFaceLib()

                    cameraBridgeViewBase.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
       // window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        cameraBridgeViewBase = findViewById(R.id.javaCam)
        cameraBridgeViewBase.visibility = CameraBridgeViewBase.VISIBLE
        cameraBridgeViewBase.setCameraIndex(mCameraId)
        cameraBridgeViewBase.setCvCameraViewListener(this)
        cameraBridgeViewBase.setCameraPermissionGranted()
       // orientation()
        btnCameraFlip.setOnClickListener {
            if (isFront) {
                mCameraId = 0
                cameraBridgeViewBase.disableView()
                cameraBridgeViewBase.setCameraIndex(mCameraId)
                cameraBridgeViewBase.enableView()
            } else {
                mCameraId = 1
                cameraBridgeViewBase.disableView()
                cameraBridgeViewBase.setCameraIndex(1)
                cameraBridgeViewBase.enableView()
            }
            isFront = !isFront

        }

        btnCapture.setOnClickListener {
            takePhoto = true
        }

        /*      buttonSave.setOnClickListener {
                  if (tempBitmap != null) {
                      val saveUri = getImageUri(tempBitmap!!)
                      val intent = Intent()
                      intent.putExtra(IMAGE_URI_SAVED, saveUri.toString())
                      setResult(RESULT_OK, intent)
                      finish()
                  } else {
                      Toast.makeText(this, "Face possibly not found", Toast.LENGTH_LONG).show()
                  }
              }*/
    }

    /*
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.androchef.cameraxfacedetection.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, CompareImageActivity.CAMERA_IMAGE)
                }
            }
        }
    }

    lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", *//* prefix *//*
            ".jpg", *//* suffix *//*
            storageDir *//* directory *//*
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            //imageUri = data?.data
            Log.e(TAG, currentPhotoPath)
           // val realPath = imageUri?.let { getRealPathFromUri(it) }
            val src = Imgcodecs.imread(currentPhotoPath)
            Log.e(TAG, src.toString())
            tempBitmap = openCvDetection.detectFaceOpenCV(src)
            imageView.setImageBitmap(tempBitmap)
        }
    }*/

    override fun onDestroy() {
        super.onDestroy()

        cameraBridgeViewBase.let { cameraBridgeViewBase.disableView() }
        if (faceDir.exists()) faceDir.delete()
    }
    override fun onPause() {
        super.onPause()
        cameraBridgeViewBase.disableView()
    }

    override fun onResume() {
        super.onResume()
        if(!OpenCVLoader.initDebug()){
            Toast.makeText(this, "There is an issue with opencv", Toast.LENGTH_LONG).show()
        }
        else{
            mLoaderCallback.onManagerConnected(SUCCESS)
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        imageMat = Mat(width, height, CvType.CV_8UC4)
        grayMat = Mat(width, height, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() {
        imageMat.release()
        grayMat.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        imageMat = inputFrame!!.rgba()
        grayMat = inputFrame.gray()

        //Computing absolute face size

        //Computing absolute face size
        if (mAbsoluteFaceSize == 0.0) {
            val height: Int = grayMat.rows()
            val mRelativeFaceSize = 0.2f
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize).toDouble()
            }
        }

        val faces = MatOfRect()

        //Using detection classifier

        //Using detection classifier
        if (mFaceDetector != null) {
            mFaceDetector?.detectMultiScale(
                grayMat, faces, 1.1, 5, 2,
                Size(mAbsoluteFaceSize, mAbsoluteFaceSize), Size()
            )
        } else {
            Log.e(TAG, "Detection is not selected!")
        }

        //Drawing rectangle around detected face

        //Drawing rectangle around detected face
        val facesArray = faces.toArray()
        for (i in facesArray.indices) {
            Imgproc.rectangle(
                imageMat,
                facesArray[i].tl(),
                facesArray[i].br(),
                Scalar(0.0, 255.0, 0.0, 255.0),
                3
            )
        }

        //If one face is detected and Capture is pressed, method capturePhoto and alertRemainingPhotos are executed

        //If one face is detected and Capture is pressed, method capturePhoto and alertRemainingPhotos are executed
        if (facesArray.size == 1) {
            if (takePhoto) {
                capturePhoto(imageMat)
               // alertRemainingPhotos()
            }
        }
        return imageMat
    }

    //Method for capturing photos
    private fun capturePhoto(rgbaMat: Mat) {
        try {
          mFaceDetector?.let {takePhoto( rgbaMat.clone(), it) }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        takePhoto = false
    }


    @Throws(Exception::class)
    fun takePhoto(rgbaMat: Mat?, faceDetector: CascadeClassifier) {
        /*  var savePhoto = File("")
        val facePicsPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
           "face_Pics"
        )
        if (facePicsPath.exists() && !facePicsPath.isDirectory) facePicsPath.delete()
        if (!facePicsPath.exists()) facePicsPath.mkdirs()*/
        val grayMat = Mat()
        //Converting RGBA to GRAY
        Imgproc.cvtColor(rgbaMat, grayMat, Imgproc.COLOR_RGBA2GRAY)
        val detectedFaces = MatOfRect()
        faceDetector.detectMultiScale(grayMat, detectedFaces)
        val detectedFacesArray = detectedFaces.toArray()
        Log.e(TAG, detectedFacesArray.size.toString())
        var rect: Rect
        var capturedFace = Mat()
        for (face in detectedFacesArray) {
            rect = face
            capturedFace = Mat(grayMat, face)
            //Resizing to 92x112
            /*   Imgproc.resize(
                  capturedFace, capturedFace, Size(
                      IMG_WIDTH.toDouble(),
                      IMG_HEIGHT.toDouble()
                  )
              )
              //Histogram equalizing
               Log.e(TAG, "=> $capturedFace")

                Imgproc.equalizeHist(capturedFace, capturedFace)
               savePhoto = File(facePicsPath, String.format("%d.png", SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(
                   Date()
               )))
               savePhoto.createNewFile()
               //Saving photos to directory FacePics
              // Imgcodecs.imwrite(savePhoto.absolutePath, capturedFace)

               val src = Imgcodecs.imread(savePhoto.absolutePath)
              // Log.e(GalleryImageActivity.TAG, realPath.toString())
              val tempBitmap = openCvDetection.detectFaceOpenCV(src)
              // imageView.setImageBitmap(tempBitmap)

              cameraListener?.cameraCaptureResult(savedUri)
               Log.i(TAG, "PIC PATH: " + savePhoto.absolutePath)*/

        }
       // val bitmap = convertMatToBitMap(capturedFace)
        //if (bitmap != null) {
            val intent = Intent()

            intent.putExtra(CAMERA_BITMAP, capturedFace.nativeObjAddr)
            setResult(RESULT_OK, intent)
            finish()
           // cameraListener?.cameraCaptureBitmapResult(bitmap)
      //  }
    }
/*
    @Throws(IOException::class)
    fun savebitmap(bmp: Bitmap): File {
        val bytes = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes)
        val f = File(
            Environment.getExternalStorageDirectory()
                .toString() + File.separator + SimpleDateFormat(
               FILENAME_FORMAT, Locale.ENGLISH
            ).format(System.currentTimeMillis()) + ".jpg"
        )
        f.createNewFile()
        val fo = FileOutputStream(f)
        fo.write(bytes.toByteArray())
        fo.close()
        return f
    }
    private fun convertMatToBitMap(input: Mat): Bitmap? {
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
    }*/
    fun loadFaceLib() {
        try {
            val modelInputStream =
                resources.openRawResource(
                    R.raw.haarcascade_frontalface_alt2
                )

            // create a temp directory
            faceDir = getDir(FACE_DIR, Context.MODE_PRIVATE)

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

            mFaceDetector = CascadeClassifier(faceModel.absolutePath)
        } catch (e: IOException) {
            Log.e("Error ", "loading cascade face model...$e")
        }
    }
    companion object {
        const val IMAGE_URI_SAVED = "image_frm_camera"
        const val CAMERA_BITMAP = "camera_bitmap"
        private const val TAG = "CAMERAA_IMAGE"
        private const val FACE_DIR = "facelib"
        private const val FACE_MODEL = "haarcascade_frontalface_alt2.xml"
        private const val byteSize = 4096 // buffer size
        private const val IMG_WIDTH = 92
        private const val IMG_HEIGHT = 112
        var cameraListener: BitmapListener?= null


    }


}