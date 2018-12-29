package com.github.ipcjs.screenshottile

import android.graphics.Bitmap
import android.media.Image
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */

fun screenshot(context: Context) {
    TakeScreenshotActivity.start(context)
}


fun imageToBitmap(image: Image) : Bitmap {
    // Copy image content to new bitmap
    val w = image.width + (image.planes[0].rowStride - image.planes[0].pixelStride * image.width ) / image.planes[0].pixelStride
    val h = image.height
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(image.planes[0].buffer)
    return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
}

fun addImageToGallery(context: Context, filepath: String, title: String, description: String, mimeType: String="image/jpeg"): Uri {
    // Add image file and information to media store
    val values = ContentValues()
    values.put(Images.Media.TITLE, title)
    values.put(Images.Media.DESCRIPTION, description)
    values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis())
    values.put(Images.Media.MIME_TYPE, mimeType)
    values.put(MediaStore.MediaColumns.DATA, filepath)
    return context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values)!!
}

fun createImageFile(context: Context, filename: String, suffix:String = ".jpg"): File {
    // New image file in default "Picture" directory
    val storageDir: File = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
    return File.createTempFile(filename,suffix,  storageDir)
}

fun saveImageToFile(context: Context, image: Image, prefix: String, imageQuality: Int=100): File {
    // Save image to jpg file in default "Picture" storage with filename="{$prefix}yyyyMMdd_HHmmss"

    val date = Date()
    val timeStamp: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_").format(date)
    val filename = "$prefix$timeStamp"
    val imageFile = createImageFile(context, filename, ".jpg")

    // Save image
    val bitmap = imageToBitmap(image)
    image.close()
    val bytes =  ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, bytes)

    imageFile.createNewFile()
    val fileOutputStream = FileOutputStream(imageFile)
    fileOutputStream.write(bytes.toByteArray())
    fileOutputStream.close()

    // Add to g
    addImageToGallery(context, imageFile.absolutePath, context.getString(R.string.file_tile), context.getString(R.string.file_description, SimpleDateFormat(context.getString(R.string.file_description_simpledateformat)).format(date)))

    return imageFile
}