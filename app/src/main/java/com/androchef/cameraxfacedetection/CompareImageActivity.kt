package com.androchef.cameraxfacedetection

import android.Manifest
import android.app.Activity
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
import com.androchef.cameraxfacedetection.GalleryImageActivity.Companion.GALLERY_URI
import com.androchef.cameraxfacedetection.camerax.CameraManager.Companion.IMAGE_URI_SAVED
import com.androchef.cameraxfacedetection.models.FaceNetModel
import com.androchef.cameraxfacedetection.models.Models
import com.androchef.cameraxfacedetection.utils.BitmapUtils.Companion.detectFace
import com.androchef.cameraxfacedetection.utils.FileReader
import com.androchef.cameraxfacedetection.utils.RunModel
import kotlinx.android.synthetic.main.activity_compare_image.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class CompareImageActivity : AppCompatActivity() {
    private var imagePicked = -1
    private var bitmap1: Bitmap? = null
    private var bitmap2: Bitmap? = null
    val images = ArrayList<Pair<String, Bitmap>>()

    private lateinit var faceNetModel: FaceNetModel
    private lateinit var fileReader: FileReader
    private lateinit var frameAnalyser: RunModel

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
    fun verifyStoragePermissions() {
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
        setContentView(R.layout.activity_compare_image)
        verifyStoragePermissions()
        logTextView = textViewLog
        imageView1.layoutParams.height = 400
        imageView2.layoutParams.height = 400
        faceNetModel = FaceNetModel(this, modelInfo, useGpu, useXNNPack)
        frameAnalyser = RunModel(faceNetModel)
        fileReader = FileReader(faceNetModel)

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
                    val intent = Intent(this, MainActivity::class.java)
                    startActivityForResult(intent, CAMERA_IMAGE)
                    // startFaceCaptureActivity(imageView)
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.menuInflater.inflate(R.menu.menu, popupMenu.menu)
        popupMenu.show()
    }

/*
    private fun openGallery(id: Int) {
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, id)
    }*/

    private val fileReaderCallback = object : FileReader.ProcessCallback {
        override fun onProcessCompleted(
            data: ArrayList<Pair<String, FloatArray>>,
            numImagesWithNoFaces: Int
        ) {
            frameAnalyser.faceList = data
            Log.e(TAG, "Images parsed. Found $numImagesWithNoFaces images with no faces.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_IMAGE) {
                if (data != null && data.hasExtra(IMAGE_URI_SAVED)) {
                    val imageString = data.getStringExtra(IMAGE_URI_SAVED)
                    val imageUri = Uri.parse(imageString)

                  /*  val bitmap = when {
                        Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
                            this.contentResolver,
                            imageUri
                        )
                        else -> {
                            val source = ImageDecoder.createSource(this.contentResolver, imageUri)
                            ImageDecoder.decodeBitmap(source)
                        }
                    }*/
                    val bitmap = detectFace(imageUri)
                    if (imagePicked != -1)
                        if (imagePicked == PICK_IMAGE_1) {
                            bitmap1 = bitmap
                            imageView1.setImageBitmap(bitmap)
                          //  imageView1.setImageURI(imageUri)

                        } else if (imagePicked == PICK_IMAGE_2) {
                            bitmap2 = bitmap
                            imageView2.setImageBitmap(bitmap)

                           // imageView2.setImageURI(imageUri)
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
        const val TAG = "COMPARE_IMAGE_ACTIVITY"
        private const val PICK_IMAGE_1 = 1
        private const val PICK_IMAGE_2 = 2
        const val CAMERA_IMAGE = 3
        const val GALLERY_IMAGE = 4
        lateinit var logTextView: TextView


        fun setMessage(message: String) {
            logTextView.text = message
        }
    }
}