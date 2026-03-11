package vinz.android.imagesqueeze.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import timber.log.Timber
import java.io.File
import java.io.IOException

internal object ImageUtil {
    private const val TAG = "ImageSqueeze_ImageUtil"

    fun decodeSampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                Timber.w("$TAG: Cannot determine image dimensions")
                return null
            }

            options.apply {
                inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (oom: OutOfMemoryError) {
            Timber.e(oom, "$TAG: OOM while decoding bitmap")
            null
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error decoding bitmap")
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun applyExifRotation(bitmap: Bitmap, file: File): Bitmap {
        val orientation = try {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: IOException) {
            Timber.w(e, "$TAG: Cannot read EXIF data")
            ExifInterface.ORIENTATION_NORMAL
        }

        val angle = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }

        return try {
            val matrix = Matrix()
            matrix.postRotate(angle)
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (oom: OutOfMemoryError) {
            Timber.e(oom, "$TAG: OOM during EXIF rotation, returning unrotated")
            bitmap
        }
    }

    fun Bitmap.safeRecycle() {
        try {
            if (!isRecycled) recycle()
        } catch (e: Exception) {
            Timber.w(e, "$TAG: Failed to recycle bitmap")
        }
    }
}
