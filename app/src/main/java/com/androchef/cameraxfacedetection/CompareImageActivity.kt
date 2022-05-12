package com.androchef.cameraxfacedetection

import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.androchef.cameraxfacedetection.GalleryImageActivity.Companion.GALLERY_URI
import com.androchef.cameraxfacedetection.camerax.CameraManager.Companion.IMAGE_URI_SAVED
import kotlinx.android.synthetic.main.activity_compare_image.*
import org.opencv.android.OpenCVLoader
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt


class CompareImageActivity : AppCompatActivity() {
    private var imagePicked = -1
    private var bitmap1: Bitmap? = null
    private var bitmap2: Bitmap? = null
    private lateinit var tfliteModel: MappedByteBuffer
    private lateinit var interpreter: Interpreter
    private var tImage: TensorImage = TensorImage()
    private var tBuffer: TensorBuffer? = null

    private var MODEL_PATH = "MobileFacenet.tflite"
// private var MODEL_PATH = "facenet.tflite"

    // Width of the image that our model expects
    var inputImageWidth = 112

    // Height of the image that our model expects
    var inputImageHeight = 112
    private val IMAGE_MEAN = 128.5f
    private val IMAGE_STD = 128f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compare_image)
        //static {
            if (OpenCVLoader.initDebug()){
                Log.i(TAG, "opencv installed successfully");
            }else{
                Log.i(TAG, "opencv not installed");
            }
      //  }
        initializeModel()

        imageView1.layoutParams.height = 400
        imageView2.layoutParams.height = 400

        imageView1.setOnClickListener {
            imagePicked = PICK_IMAGE_1
            showMenu(imageView1)
        }
        imageView2.setOnClickListener {
            imagePicked = PICK_IMAGE_2
            showMenu(imageView2)
        }
        buttonMatch.setOnClickListener {
            if (bitmap1 != null && bitmap2 != null) {
                val img1 = bitmap1!!.copy(Bitmap.Config.ARGB_8888, true)
                val img2 = bitmap2!!.copy(Bitmap.Config.ARGB_8888, true)
                Log.e(TAG, "converted")
                val embedding1 = getEmbedding(img1)
                val embedding2 = getEmbedding(img2)

                val res = recognize(embedding1, embedding2)
                textViewLog.text = res
                /*  val scalar = if (res == "unknown") {
                      Scalar(255.0, 0.0, 0.0)
                  } else Scalar(0.0, 255.0, 0.0)*/

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_IMAGE) {
                if (data != null && data.hasExtra(IMAGE_URI_SAVED)) {
                    val imageString = data.getStringExtra(IMAGE_URI_SAVED)

                    val imageUri = Uri.parse(imageString)
                    //  var bitmap: Bitmap? = null
                    // val contentResolver = contentResolver

                    if (imagePicked != -1)
                        if (imagePicked == PICK_IMAGE_1) {
                            imageView1.setImageURI(imageUri)

                        } else if (imagePicked == PICK_IMAGE_2) {
                            imageView2.setImageURI(imageUri)
                        }
                }
            } else {
                if (data != null && data.hasExtra(GALLERY_URI)) {
                    val imageString = data.getStringExtra(GALLERY_URI)
                    val imageUri = Uri.parse(imageString)
                    if (imagePicked != -1)
                        if (imagePicked == PICK_IMAGE_1) {
                            imageView1.setImageURI(imageUri)
                        } else {
                            imageView2.setImageURI(imageUri)
                        }
                }else{
                    Log.e("COMPARE_ACTIVITY", "Eee No get anything")
                }
            }
        }
    }

    private fun initializeModel() {
        try {
            tfliteModel = loadModelFile()

            val delegate = GpuDelegate(GpuDelegate.Options().setQuantizedModelsAllowed(true))
            val options = (Interpreter.Options()).addDelegate(delegate)

            @Suppress("DEPRECATION")
            interpreter = Interpreter(tfliteModel, options)

            val probabilityTensorIndex = 0
            val probabilityShape =
                interpreter.getOutputTensor(probabilityTensorIndex).shape() // {1, EMBEDDING_SIZE}
            val probabilityDataType = interpreter.getOutputTensor(probabilityTensorIndex).dataType()

            // Creates the input tensor
            tImage = TensorImage(DataType.FLOAT32)

            // Creates the output tensor and its processor
            tBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)

            Log.d(TAG, "Model loaded successful")
        } catch (e: IOException) {
            Log.e(TAG, "Error reading model", e)
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun recognize(embedding1: FloatArray?, embedding2: FloatArray?): String {
        if (embedding1 != null || embedding2 != null) {
            return if (embedding1!!.isNotEmpty() || embedding2!!.isNotEmpty()) {
                val maxVal = cosineSimilarity(embedding1, embedding2)
                Log.e(TAG, maxVal.toString())
                if (maxVal > 0.50) " ${(maxVal * 100).toString().take(2)}%"
                else "unknown:: ${(maxVal * 100)}%"
            } else "unknown"
        }
        return "unknown: 0%"
    }

    private fun getEmbedding(bitmap: Bitmap): FloatArray? {
        tImage = loadImage(bitmap)

        interpreter.run(tImage.buffer, tBuffer?.buffer?.rewind())

        return tBuffer?.floatArray
    }

    private fun loadImage(bitmap: Bitmap): TensorImage {
        // Loads bitmap into a TensorImage
        tImage.load(bitmap)

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(IMAGE_MEAN, IMAGE_STD))
            .build()
        return imageProcessor.process(tImage)
    }

    private fun cosineSimilarity(A: FloatArray?, B: FloatArray?): Float {
        if (A == null || B == null || A.isEmpty() || B.isEmpty() || A.size != B.size) {
            return 2.0F
        }

        var sumProduct = 0.0
        var sumASq = 0.0
        var sumBSq = 0.0
        for (i in A.indices) {
            sumProduct += (A[i] * B[i]).toDouble()
            sumASq += (A[i] * A[i]).toDouble()
            sumBSq += (B[i] * B[i]).toDouble()
        }
        val sumTotal = if (sumASq == 0.0 && sumBSq == 0.0) {
            2.0F
        } else (sumProduct / (sqrt(sumASq) * sqrt(sumBSq))).toFloat()
        Log.e(TAG, sumTotal.toString())
        return sumTotal
    }

    companion object {
        private const val PICK_IMAGE_1 = 1
        private const val PICK_IMAGE_2 = 2
        const val CAMERA_IMAGE = 3
        const val GALLERY_IMAGE = 4
        const val TAG = "COMPARE_ACTIVITY"
    }
}