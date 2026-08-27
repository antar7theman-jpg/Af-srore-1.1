package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.random.Random

object ImageStorageHelper {

    private const val DIRECTORY_NAME = "product_images"
    private const val MAX_DIMENSION = 1080
    private const val JPEG_QUALITY = 85

    fun getImagesDirectory(context: Context): File {
        val dir = File(context.filesDir, DIRECTORY_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Reads an image Uri from Gallery/PhotoPicker, resizes it if needed,
     * corrects orientation, and saves it into the internal app storage.
     * Returns the absolute path of the saved file.
     */
    fun saveImageFromUri(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Decode bounds to check dimension
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            } ?: return null

            // 2. Calculate sample size
            val maxDim = max(options.outWidth, options.outHeight)
            var sampleSize = 1
            if (maxDim > MAX_DIMENSION) {
                sampleSize = maxDim / MAX_DIMENSION
            }

            // 3. Decode actual bitmap with inSampleSize
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            var bitmap = contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } ?: return null

            if (bitmap == null) return null

            // 4. Correct orientation using ExifInterface if possible
            try {
                contentResolver.openInputStream(uri)?.use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                        val rotated = Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                        )
                        if (rotated != bitmap) {
                            bitmap.recycle()
                            bitmap = rotated
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore exif error, proceed with original bitmap
            }

            // 5. Save to internal files
            val dir = getImagesDirectory(context)
            val fileName = "prod_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}.jpg"
            val destFile = File(dir, fileName)

            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
            }
            bitmap.recycle()

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves a captured Bitmap directly into internal app storage.
     * Returns the absolute path of the saved file.
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val dir = getImagesDirectory(context)
            val fileName = "prod_cam_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}.jpg"
            val destFile = File(dir, fileName)

            // Scale down if needed
            val maxDim = max(bitmap.width, bitmap.height)
            val scaledBitmap = if (maxDim > MAX_DIMENSION) {
                val ratio = MAX_DIMENSION.toFloat() / maxDim
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true
                )
            } else {
                bitmap
            }

            FileOutputStream(destFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
            }

            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Safely deletes an image file from storage if it exists.
     */
    fun deleteImageFile(filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) return false
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Converts a Uri to a ByteArray if needed.
     */
    fun uriToByteArray(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }
}
