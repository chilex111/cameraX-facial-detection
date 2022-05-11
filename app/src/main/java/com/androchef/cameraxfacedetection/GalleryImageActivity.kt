package com.androchef.cameraxfacedetection

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.isNotEmpty
import com.androchef.cameraxfacedetection.utils.BitmapUtils.Companion.getImageUri
import com.androchef.cameraxfacedetection.utils.BitmapUtils.Companion.rotateImage
import com.androchef.cameraxfacedetection.utils.BitmapUtils.Companion.uriToBitmap
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import kotlinx.android.synthetic.main.activity_gallery_image.*


class GalleryImageActivity : AppCompatActivity() {
    private var tempBitmap: Bitmap? = null
    private var bitmapSource: Bitmap? = null
    private var imageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_image)
        openGallery()
        buttonSave.setOnClickListener {
            if (bitmapSource != null || imageUri != null) {
                val saveUri = getImageUri(bitmapSource!!, imageUri!!)
                val intent = Intent()
                intent.putExtra(GALLERY_URI, saveUri.toString())
                setResult(RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "Face possibly not found", Toast.LENGTH_LONG).show()
            }
        }
        buttonRotate.setOnClickListener {
            val bitmap = rotateImage(tempBitmap!!)
            detectFace(null, bitmap)
        }
    }

    private fun openGallery() {
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            imageUri = data?.data
            imageView.setImageURI(imageUri)
            imageUri?.let { detectFace(it, null) }

        }
    }

    private lateinit var thisFace: Face
/*
    private fun detectFace(imageUri: Uri *//*imageBitmap: Bitmap*//*) {
        val imageBitmap = uriToBitmap(imageUri)
        val bitmap = imageBitmap.copy(
            Bitmap.Config.ARGB_8888,
            true
        )//   `.resizeWithoutDistortion()// convert image from hardware bitmap to software bitmap :(
        val paint = Paint()
        paint.strokeWidth = 6f
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(tempBitmap!!)
        Log.e("TAG", tempBitmap.toString())
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val faceDetector: FaceDetector = FaceDetector.Builder(this).setTrackingEnabled(false)
            .build()
        if (!faceDetector.isOperational) {
            AlertDialog.Builder(this).setMessage("Could not set up Face Detector!").show()
            return
        }
        val frame: Frame = Frame.Builder().setBitmap(bitmap).build()
        val faces: SparseArray<Face> = faceDetector.detect(frame)
        Log.e("TAG", faces.toString())
        if(faces.isNotEmpty()) {
            for (i in 0 until faces.size()) {
                thisFace = faces.valueAt(i)
                val x1: Float = thisFace.position.x
                val y1: Float = thisFace.position.y
                val x2: Float = x1 + thisFace.width
                val y2: Float = y1 + thisFace.height
                canvas.drawRoundRect(RectF(x1, y1, x2, y2), 2f, 2f, paint)
            }

            val source = tempBitmap
            bitmapSource = when {
                thisFace.height.plus(thisFace.position.y) > source!!.height -> {
                    source

                }
                thisFace.width.plus(thisFace.position.x) > source.width -> {
                    source
                }

                else -> Bitmap.createBitmap(
                    source,
                    thisFace.position.x.toInt(),
                    thisFace.position.y.toInt(),
                    thisFace.width.toInt(),
                    thisFace.height.toInt()
                )
            }
            imageView.setImageDrawable(BitmapDrawable(resources, bitmapSource))
        }
        else{
           AlertDialog.Builder(this).setMessage("Could not detect Face, could be from rotation!\n " +
                    "Click on Rotate to set image properly")
                .setPositiveButton("OK") { _, _ ->
                    buttonRotate.visibility = View.VISIBLE
                    finish()
                }
               .show()
            return
        }
    }*/

    private fun detectFace(imageUri: Uri?, bm: Bitmap?) {
        val imageBitmap = if (imageUri != null) uriToBitmap(imageUri) else bm
        val bitmap = imageBitmap!!.copy(
            Bitmap.Config.ARGB_8888,
            true
        )//   `.resizeWithoutDistortion()// convert image from hardware bitmap to software bitmap :(
        val paint = Paint()
        paint.strokeWidth = 6f
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(tempBitmap!!)
        Log.e("TAG", tempBitmap.toString())
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val faceDetector: FaceDetector = FaceDetector.Builder(this).setTrackingEnabled(false)
            .build()
        if (!faceDetector.isOperational) {
            AlertDialog.Builder(this).setMessage("Could not set up Face Detector!").show()
            return
        }
        val frame: Frame = Frame.Builder().setBitmap(bitmap).build()
        val faces: SparseArray<Face> = faceDetector.detect(frame)
        Log.e("TAG", faces.toString())
        if (faces.isNotEmpty()) {
            for (i in 0 until faces.size()) {
                thisFace = faces.valueAt(i)
                val x1: Float = thisFace.position.x
                val y1: Float = thisFace.position.y
                val x2: Float = x1 + thisFace.width
                val y2: Float = y1 + thisFace.height
                canvas.drawRoundRect(RectF(x1, y1, x2, y2), 2f, 2f, paint)
            }

            val source = tempBitmap
            bitmapSource = when {
                thisFace.height.plus(thisFace.position.y) > source!!.height -> {
                    source

                }
                thisFace.width.plus(thisFace.position.x) > source.width -> {
                    source
                }

                else -> Bitmap.createBitmap(
                    source,
                    thisFace.position.x.toInt(),
                    thisFace.position.y.toInt(),
                    thisFace.width.toInt(),
                    thisFace.height.toInt()
                )
            }
            imageView.setImageDrawable(BitmapDrawable(resources, bitmapSource))
        } else {
            AlertDialog.Builder(this).setMessage(
                "Could not detect Face, could be from rotation!\n " +
                        "Click on Rotate to set image properly"
            )
                .setPositiveButton("OK") { dialogInterface, _ ->
                    buttonRotate.visibility = View.VISIBLE
                    dialogInterface.dismiss()
                }
                .show()
            return
        }
    }

    companion object {
        private const val PICK_IMAGE = 1
        const val GALLERY_URI = "image_frm_gallery"
    }
}