package com.androchef.cameraxfacedetection

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androchef.cameraxfacedetection.utils.OpenCvDetection
import com.androchef.cameraxfacedetection.utils.getRealPathFromUri
import kotlinx.android.synthetic.main.activity_gallery_image.*
import org.opencv.imgcodecs.Imgcodecs
import java.io.ByteArrayOutputStream
import java.io.File


class GalleryImageActivity : AppCompatActivity() {
    private var tempBitmap: Bitmap? = null
    private var imageUri: Uri? = null

    lateinit var openCvDetection: OpenCvDetection
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_image)

        openCvDetection = OpenCvDetection(this)
        openCvDetection.loadFaceLib()
        openGallery()
        buttonSave.setOnClickListener {
            if (tempBitmap != null) {
                val saveUri = getImageUri(tempBitmap!!)
                val intent = Intent()
                intent.putExtra(GALLERY_URI, saveUri.toString())
                setResult(RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "Face possibly not found", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openGallery() {
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, PICK_IMAGE)
    }

    private fun getImageUri(inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            contentResolver,
            inImage,
            File(imageUri?.path.toString()).name,
            null
        )
        return Uri.parse(path)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            imageUri = data?.data

            val realPath = imageUri?.let { getRealPathFromUri(it) }
            val src = Imgcodecs.imread(realPath)
            Log.e(TAG, realPath.toString())
            tempBitmap = openCvDetection.detectFaceOpenCV(src)
            imageView.setImageBitmap(tempBitmap)
        }
    }

    companion object {
        private const val PICK_IMAGE = 1
        const val GALLERY_URI = "image_frm_gallery"

        private const val TAG = "GALEERY_IMAGE"
    }
}