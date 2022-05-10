package com.androchef.cameraxfacedetection

import android.app.Activity
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.androchef.cameraxfacedetection.GalleryImageActivity.Companion.GALLERY_URI
import com.androchef.cameraxfacedetection.camerax.CameraManager.Companion.IMAGE_URI_SAVED
import kotlinx.android.synthetic.main.activity_compare_image.*
import org.opencv.core.Scalar
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


class CompareImageActivity : AppCompatActivity() {
    private var imagePicked = -1

    private lateinit var tfliteModel: MappedByteBuffer
    private lateinit var interpreter: Interpreter
    private var tImage: TensorImage = TensorImage()
    private var tBuffer: TensorBuffer?= null

    private var MODEL_PATH = "MobileFaceNet.tflite"

    // Width of the image that our model expects
    var inputImageWidth = 112

    // Height of the image that our model expects
    var inputImageHeight = 112
    private val IMAGE_MEAN = 127.5f
    private val IMAGE_STD = 128f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compare_image)
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
            val res = getEmbedding(mBitmap!!)?.let { recognize(it) }
            val scalar = if (res == "unknown") {
                Scalar(255.0, 0.0, 0.0)
            } else Scalar(0.0, 255.0, 0.0)

        }
        initializeModel()
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

    private fun recognize( embedding1 : FloatArray, embedding2: FloatArray): String {
        return if (embedding1.isNotEmpty() || embedding2.isNotEmpty()) {
           // val similarities = ArrayList<Float>()

           val maxVal =  cosineSimilarity(embedding1, embedding2)

         //   val maxVal = similarities.maxOrNull()!!
            if (maxVal > 0.50) " ${(maxVal * 100).toString().take(2)}%"
            else "unknown:: ${(maxVal * 100).toString().take(2)}%"
        } else "unknown"
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
        return if (sumASq == 0.0 && sumBSq == 0.0) {
            2.0F
        } else (sumProduct / (Math.sqrt(sumASq) * Math.sqrt(sumBSq))).toFloat()
    }

    private fun showMenu(imageView: ImageView?) {
        val popupMenu = PopupMenu(this, imageView)
        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.gallery -> {
                    val intent = Intent(this, GalleryImageActivity::class.java)
                    galleryLauncher.launch(intent)
                    return@setOnMenuItemClickListener true
                }
                R.id.camera -> {
                    val intent = Intent(this, MainActivity::class.java)
                    cameraLauncher.launch(intent)
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.menuInflater.inflate(R.menu.menu, popupMenu.menu)
        popupMenu.show()
    }

    private var galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
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

    private var cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data != null && data.hasExtra(IMAGE_URI_SAVED)) {
                val imageString = data.getStringExtra(IMAGE_URI_SAVED)

                val imageUri = Uri.parse(imageString)

                if (imagePicked != -1)
                    if (imagePicked == PICK_IMAGE_1) {
                        imageView1.setImageURI(imageUri)

                    } else if (imagePicked == PICK_IMAGE_2) {
                        imageView2.setImageURI(imageUri)
                    }
            }
        }
    }

    companion object {
        private const val PICK_IMAGE_1 = 1
        private const val PICK_IMAGE_2 = 2
        const val TAG = "COMPARE_ACTIVITY"
    }
}