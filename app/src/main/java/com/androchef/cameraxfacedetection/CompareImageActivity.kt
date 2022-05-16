package com.androchef.cameraxfacedetection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.androchef.cameraxfacedetection.CameraActivity.Companion.CAMERA_BITMAP
import com.androchef.cameraxfacedetection.GalleryImageActivity.Companion.GALLERY_URI
import com.androchef.cameraxfacedetection.listener.BitmapListener
import com.androchef.cameraxfacedetection.models.FaceNetModel
import com.androchef.cameraxfacedetection.models.Models
import com.androchef.cameraxfacedetection.utils.FileReader
import com.androchef.cameraxfacedetection.utils.OpenCvDetection
import com.androchef.cameraxfacedetection.utils.RunModel
import kotlinx.android.synthetic.main.activity_compare_image.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat

class CompareImageActivity : AppCompatActivity(), BitmapListener {
    private var imagePicked = -1
    private var bitmap1: Bitmap? = null
    private var bitmap2: Bitmap? = null
    private lateinit var faceNetModel: FaceNetModel
    private lateinit var fileReader: FileReader
    private lateinit var frameAnalyser: RunModel

    lateinit var openCvDetection: OpenCvDetection
// <----------------------- User controls --------------------------->

    // Use the device's GPU to perform faster computations.
// Refer https://www.tensorflow.org/lite/performance/gpu
    private val useGpu = true

    // Use XNNPack to accelerate inference.
// Refer https://blog.tensorflow.org/2020/07/accelerating-tensorflow-lite-xnnpack-integration.html
    private val useXNNPack = true

    // You may the change the models here.
// Use the model configs in Models.kt
// Default is Models.FACENET ; Quantized models are faster
    private val modelInfo = Models.FACENET

    // <---------------------------------------------------------------->
    private val REQUEST_EXTERNAL_STORAGE: Int = 1
    private val PERMISSIONS_STORAGE = arrayOf<String>(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param //activity
     */
    private fun verifyStoragePermissions() {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verifyStoragePermissions()
        setContentView(R.layout.activity_compare_image)

            if (OpenCVLoader.initDebug()){
                Log.i(TAG, "opencv installed successfully")
            }else{
                Log.i(TAG, "opencv not installed")
            }
        openCvDetection = OpenCvDetection(this)
        openCvDetection.loadFaceLib()
        imageView1.layoutParams.height = 400
        imageView2.layoutParams.height = 400

        faceNetModel = FaceNetModel(this, modelInfo, useGpu, useXNNPack)
        frameAnalyser = RunModel(faceNetModel)
        fileReader = FileReader(faceNetModel)
        logTextView = textViewLog

        imageView1.setOnClickListener {
            imagePicked = PICK_IMAGE_1
            showMenu(imageView1)
        }
        imageView2.setOnClickListener {
            imagePicked = PICK_IMAGE_2
            showMenu(imageView2)
        }
        buttonMatch.setOnClickListener {

            CoroutineScope(Dispatchers.Default).launch {
                if (bitmap1 != null && bitmap2 != null)
                    frameAnalyser.runModel(bitmap1!!, bitmap2!!)
                else {
                    Toast.makeText(
                        this@CompareImageActivity,
                        "You need 2 images for similarity check",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showMenu(imageView: ImageView?) {
        val popupMenu = PopupMenu(this, imageView)
        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.gallery -> {
                   // openGallery(i)
                    val intent = Intent(this, GalleryImageActivity::class.java)
                    startActivityForResult(intent, GALLERY_IMAGE)
                    return@setOnMenuItemClickListener true
                }
                R.id.camera -> {
                    /*val intent = Intent(this, MainActivity::class.java)
                    startActivityForResult(intent, CAMERA_IMAGE)*/
                    // startFaceCaptureActivity(imageView)
                    val intent = Intent(this, CameraActivity::class.java)
                    startActivityForResult(intent, CAMERA_IMAGE)

                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.menuInflater.inflate(R.menu.menu, popupMenu.menu)
        popupMenu.show()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_IMAGE) {
                if (data != null && data.hasExtra(CAMERA_BITMAP)) {
                    val imageString = data.getLongExtra(CAMERA_BITMAP, 0)
                    /*val imageUri = Uri.parse(imageString)
                    val realPath = imageUri?.let { getRealPathFromUri(it) }
                    Log.e(TAG, realPath.toString())

                    val src = Imgcodecs.imread(realPath)
               // Log.e(TAG, src.toString())

                    val bitmap = openCvDetection.detectFaceOpenCV(src)*/

                    val tempImg = Mat(imageString)
                    val img = tempImg.clone()
                    val bitmap = openCvDetection.convertMatToBitMap(img)
                    if (imagePicked != -1)
                        if (imagePicked == PICK_IMAGE_1) {
                            bitmap1 = bitmap
                            imageView1.setImageBitmap(bitmap)

                        } else if (imagePicked == PICK_IMAGE_2) {
                            bitmap2 = bitmap
                            imageView2.setImageBitmap(bitmap)
                        }
                }
            } else {
                if (data != null && data.hasExtra(GALLERY_URI)) {
                    val imageString = data.getStringExtra(GALLERY_URI)
                    val imageUri = Uri.parse(imageString)

                    val bitmap = when {
                        Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
                            this.contentResolver,
                            imageUri
                        )
                        else -> {
                            val source = ImageDecoder.createSource(this.contentResolver, imageUri)
                            ImageDecoder.decodeBitmap(source)
                        }
                    }

                    if (imagePicked != -1)
                        if (imagePicked == PICK_IMAGE_1) {
                            bitmap1 = bitmap
                            imageView1.setImageURI(imageUri)
                        } else {
                            bitmap2 = bitmap

                            imageView2.setImageURI(imageUri)
                        }
                } else {
                    Log.e("COMPARE_ACTIVITY", "Eee No get anything")
                }
            }
            logTextView.text = getString(R.string.similarity_null)
        }
    }


    companion object {
        private const val PICK_IMAGE_1 = 1
        private const val PICK_IMAGE_2 = 2
        const val CAMERA_IMAGE = 3
        const val GALLERY_IMAGE = 4
        const val TAG = "COMPARE_ACTIVITY"
        lateinit var logTextView:TextView
        fun setMessage(message: String) {
            logTextView.text = message
        }
    }

    override fun cameraCaptureBitmapResult(value: Bitmap) {

    }
}