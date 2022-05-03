package com.androchef.cameraxfacedetection

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.androchef.cameraxfacedetection.camerax.CameraManager.Companion.IMAGE_URI_SAVED
import kotlinx.android.synthetic.main.activity_compare_image.*


class CompareImageActivity : AppCompatActivity() {
    private var imagePicked = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compare_image)
        imageView1.layoutParams.height = 400
        imageView2.layoutParams.height = 400

        imageView1.setOnClickListener {
            imagePicked = PICK_IMAGE_1
            showMenu(imageView1, PICK_IMAGE_1)
        }
        imageView2.setOnClickListener {
            imagePicked = PICK_IMAGE_2
            showMenu(imageView2, PICK_IMAGE_2)
        }
    }

    private fun showMenu(imageView: ImageView?, i: Int) {
        val popupMenu = PopupMenu(this, imageView)
        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.gallery -> {
                    openGallery(i)
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


    private fun openGallery(id: Int) {
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, id)
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
                            /*try {
                                bitmap = if (Build.VERSION.SDK_INT < 28) {
                                    MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                                } else {
                                    val source =
                                        ImageDecoder.createSource(contentResolver, imageUri)
                                    ImageDecoder.decodeBitmap(source)
                                }
                                imageView1.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }*/
                        } else if (imagePicked == PICK_IMAGE_2) {
                            imageView2.setImageURI(imageUri)
                            /*       try {
                                       bitmap = if (Build.VERSION.SDK_INT < 28) {
                                           MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                                       } else {
                                           val source =
                                               ImageDecoder.createSource(contentResolver, imageUri)
                                           ImageDecoder.decodeBitmap(source)
                                       }
                                       imageView2.setImageBitmap(bitmap)
                                   } catch (e: Exception) {
                                       e.printStackTrace()
                                   }*/
                        }
                }
            } else {
                val imageUri = data?.data

                if (imagePicked != -1)
                    if (imagePicked == PICK_IMAGE_1) {
                        imageView1.setImageURI(imageUri)
                    } else {
                        imageView2.setImageURI(imageUri)
                    }
            }
        }
    }

    companion object {
        private const val PICK_IMAGE_1 = 1
        private const val PICK_IMAGE_2 = 2
        const val CAMERA_IMAGE = 3
    }
}