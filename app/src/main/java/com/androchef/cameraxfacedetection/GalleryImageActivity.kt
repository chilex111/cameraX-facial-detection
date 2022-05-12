package com.androchef.cameraxfacedetection

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_gallery_image.*
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class GalleryImageActivity : AppCompatActivity() {
    private var tempBitmap: Bitmap? = null
    private var imageUri: Uri? = null

    var faceDetector: CascadeClassifier? = null
    lateinit var faceDir: File
    var imageRatio = 0.0 // scale down ratio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_image)

//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        loadFaceLib()
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

            // imageView.setImageURI(imageUri)
            //imageUri?.let { detectFace(it) }
            val realPath = imageUri?.let { getRealPathFromUri(it) }
            val src = Imgcodecs.imread(realPath)
            Log.e(TAG, realPath.toString())
            detectFaceOpenCV(src)
        }
    }

    fun Context.getRealPathFromUri(uri: Uri): String {
        var picturePath = ""
        val filePathColumn = arrayOf(MediaStore.Files.FileColumns.DATA)
        val cursor: Cursor? = contentResolver.query(
            uri, filePathColumn,
            null, null, null
        )
        if (cursor != null) {
            cursor.moveToFirst()
            val columnIndex: Int = cursor.getColumnIndex(filePathColumn[0])
            picturePath = cursor.getString(columnIndex)
            Log.e("", "picturePath : $picturePath")
            cursor.close()
        }
        return picturePath
    }

    /*
        private fun detectFace(imageUri: Uri) {
            val imageBitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            } else {
                val source =
                    ImageDecoder.createSource(contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source)
            }
           val bitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)// convert image from hardware bitmap to software bitmap :(
            val paint = Paint()
            paint.strokeWidth = 6f
            paint.color = Color.RED
            paint.style = Paint.Style.STROKE
            tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
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
                val thisFace: Face = faces.valueAt(i)
                val x1: Float = thisFace.position.x
                val y1: Float = thisFace.position.y
                val x2: Float = x1 + thisFace.width
                val y2: Float = y1 + thisFace.height
                canvas.drawRoundRect(RectF(x1, y1, x2, y2), 2f, 2f, paint)
            }
            imageView.setImageDrawable(BitmapDrawable(resources, tempBitmap))
        }*/
    private fun detectFaceOpenCV(src: Mat) {
        // Detecting the face in the snap
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
        if (tempsBitmap != null) {
            tempBitmap = Bitmap.createBitmap(
                tempsBitmap,
                face.x,
                face.y,
                face.width,
                face.height
            )
            imageView.setImageBitmap(tempBitmap)
        }
    }

    private lateinit var face: Rect
    private fun loadFaceLib() {
        try {
            val modelInputStream =
                resources.openRawResource(
                    R.raw.haarcascade_frontalface_alt2
                )

            // create a temp directory
            faceDir = getDir(FACE_DIR, MODE_PRIVATE)

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
    }

    companion object {
        private const val PICK_IMAGE = 1
        const val GALLERY_URI = "image_frm_gallery"

        // Face model
        private const val FACE_DIR = "facelib"
        private const val FACE_MODEL = "haarcascade_frontalface_alt2.xml"
        private const val byteSize = 4096 // buffer size
        private const val TAG = "GALEERY_IMAGE"
    }
}