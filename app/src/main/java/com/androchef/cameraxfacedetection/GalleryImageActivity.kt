package com.androchef.cameraxfacedetection

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.SparseArray
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import kotlinx.android.synthetic.main.activity_gallery_image.*
import java.io.ByteArrayOutputStream
import java.io.File


class GalleryImageActivity : AppCompatActivity() {
    private var bitmapSource: Bitmap? = null
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
            imageUri?.let { detectFace(it) }
        }
    }

    private lateinit var thisFace: Face
    private fun detectFace(imageUri: Uri) {
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
            return
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
        Log.e(TAG, "${thisFace.height.plus(thisFace.position.y)} :: ${tempBitmap.height}")
        bitmapSource = when {
            thisFace.height.plus(thisFace.position.y) > tempBitmap.height -> {
                tempBitmap

            }
            thisFace.width.plus(thisFace.position.x) > tempBitmap.width -> {
                tempBitmap
            }

            else -> Bitmap.createBitmap(
                tempBitmap,
                thisFace.position.x.toInt(),
                thisFace.position.y.toInt(),
                thisFace.width.toInt(),
                thisFace.height.toInt()
            )
        }

        imageView.setImageDrawable(BitmapDrawable(resources, bitmapSource))
    }

    companion object {
        private const val PICK_IMAGE = 1
        const val GALLERY_URI = "image_frm_gallery"
        const val TAG = "Gallery_ACtivity"
    }
}