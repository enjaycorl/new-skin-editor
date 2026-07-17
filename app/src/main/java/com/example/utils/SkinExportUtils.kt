package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object SkinExportUtils {

    /**
     * Saves the 2D Minecraft skin PNG to the public Gallery.
     */
    fun saveSkinToGallery(
        context: Context,
        pixels: IntArray,
        format: String,
        skinName: String
    ): Uri? {
        val (w, h) = SkinTextureMapper.getDimensions(format)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)

        val filename = "${skinName.replace(" ", "_")}_skin.png"
        var outputStream: OutputStream? = null
        var uri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SkinCraftStudio")
                }
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = resolver.openOutputStream(uri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val skinDir = File(imagesDir, "SkinCraftStudio")
                if (!skinDir.exists()) skinDir.mkdirs()
                val imageFile = File(skinDir, filename)
                outputStream = FileOutputStream(imageFile)
                uri = Uri.fromFile(imageFile)
            }

            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            outputStream?.close()
            bitmap.recycle()
        }

        return uri
    }

    /**
     * Shares the skin PNG via Android Intent
     */
    fun shareSkin(context: Context, pixels: IntArray, format: String, skinName: String) {
        val (w, h) = SkinTextureMapper.getDimensions(format)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)

        try {
            val cachePath = File(context.cacheDir, "shared_skins")
            cachePath.mkdirs()
            val file = File(cachePath, "${skinName.replace(" ", "_")}_skin.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Minecraft Skin"))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bitmap.recycle()
        }
    }
}
