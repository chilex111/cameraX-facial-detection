package com.androchef.cameraxfacedetection

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.SparseArray
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.androchef.cameraxfacedetection.utils.BitmapUtils.Companion.detectFace
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import kotlinx.android.synthetic.main.activity_gallery_image.*
import java.io.ByteArrayOutputStream
import java.io.File


class GalleryImageActivity : AppCompatActivity() {
    private var  bitmapSource: Bitmap? = null
    private var imageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_image)
        openGallery()
        buttonSave.setOnClickListener {
            if (bitmapSource != null) {
                val saveUri = getImageUri(bitmapSource!!)
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

            imageView.setImageURI(imageUri)
            val faceDetected = imageUri?.let { detectFace(it) }
            imageView.setImageDrawable(BitmapDrawable(resources, faceDetected))
        }
    }



    companion object {
        private const val PICK_IMAGE = 1
        const val GALLERY_URI = "image_frm_gallery"
    }
}