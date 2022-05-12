package com.androchef.cameraxfacedetection.utils

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log


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

 fun Context.uriToBitmap(imageUri: Uri): Bitmap {
    val bitmap = when {
        Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
            contentResolver,
            imageUri
        )
        else -> {
            val source = ImageDecoder.createSource(this.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source)
        }
    }
    return bitmap
}
