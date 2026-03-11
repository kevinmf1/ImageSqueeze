package vinz.android.imagesqueeze.core

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import vinz.android.imagesqueeze.CompressionConfig
import vinz.android.imagesqueeze.SqueezeError
import vinz.android.imagesqueeze.SqueezeResult
import vinz.android.imagesqueeze.utils.FileUtil
import vinz.android.imagesqueeze.utils.FileUtil.safeDelete
import vinz.android.imagesqueeze.utils.ImageUtil
import vinz.android.imagesqueeze.utils.ImageUtil.safeRecycle
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal object CompressorCore {
    private const val TAG = "ImageSqueeze_Core"
    private const val DISPLAY_TARGET_SIZE = 100

    suspend fun compress(
        context: Context,
        sourceFile: File,
        destinationFile: File,
        config: CompressionConfig,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): SqueezeResult = withContext(dispatcher) {
        compressSync(context, sourceFile, destinationFile, config)
    }

    fun compressSync(
        context: Context,
        sourceFile: File,
        destinationFile: File,
        config: CompressionConfig
    ): SqueezeResult {
        try {
            if (!sourceFile.exists()) {
                return SqueezeResult.Error(
                    SqueezeError.FILE_NOT_FOUND,
                    IOException("Source file does not exist: ${sourceFile.absolutePath}"),
                    "Source file not found. Please select a valid file."
                )
            }

            if (!sourceFile.canRead()) {
                return SqueezeResult.Error(
                    SqueezeError.NOT_READABLE,
                    IOException("Source file is not readable: ${sourceFile.absolutePath}"),
                    "Source file is unreadable. Please check file permissions."
                )
            }

            if (sourceFile.length() == 0L) {
                return SqueezeResult.Error(
                    SqueezeError.FILE_EMPTY,
                    IOException("Source file is empty: ${sourceFile.absolutePath}"),
                    "Source file is empty. Please select a valid image."
                )
            }

            val originalSize = sourceFile.length()

            if (!FileUtil.hasEnoughDiskSpace(context)) {
                return SqueezeResult.Error(
                    SqueezeError.NO_DISK_SPACE,
                    IOException("Not enough disk space"),
                    "Device storage is running out. Please free up some memory."
                )
            }

            val destDir = destinationFile.parentFile
            if (destDir != null && !destDir.exists()) {
                destDir.mkdirs()
            }

            val cacheDir = File(context.cacheDir, "image_compressor")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val workingFile = File(cacheDir, "work_${System.currentTimeMillis()}_${sourceFile.name}")

            try {
                sourceFile.copyTo(workingFile, overwrite = true)
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Failed to copy source to working file")
                return SqueezeResult.Error(
                    SqueezeError.COPY_FAILED,
                    e,
                    "Failed to prepare file for compression. Please try again."
                )
            }

            val (reqWidth, reqHeight) = if (config.isForDisplay) {
                DISPLAY_TARGET_SIZE to DISPLAY_TARGET_SIZE
            } else {
                config.targetWidth to config.targetHeight
            }

            val bitmap = ImageUtil.decodeSampledBitmap(workingFile, reqWidth, reqHeight)
                ?: run {
                    workingFile.safeDelete()
                    return SqueezeResult.Error(
                        SqueezeError.DECODE_FAILED,
                        IllegalStateException("BitmapFactory returned null for: ${sourceFile.absolutePath}"),
                        "Image format is unsupported or corrupted."
                    )
                }

            val rotatedBitmap = try {
                ImageUtil.applyExifRotation(bitmap, workingFile)
            } catch (e: Exception) {
                Timber.w(e, "$TAG: Failed to read EXIF, using unrotated bitmap")
                bitmap
            }

            var compressQuality = config.defaultQuality
            workingFile.safeDelete() // remove cloned file before overwriting

            while (true) {
                try {
                    workingFile.parentFile?.mkdirs()
                    FileOutputStream(workingFile).use { fos ->
                        rotatedBitmap.compress(config.compressFormat, compressQuality, fos)
                        fos.flush()
                        fos.fd.sync()
                    }
                    val currentSize = workingFile.length()

                    // Exit if size target met or quality floor reached.
                    if (currentSize <= config.maxSizeInBytes || compressQuality <= config.minQuality) {
                        break
                    }

                    compressQuality -= 10
                } catch (e: OutOfMemoryError) {
                    rotatedBitmap.safeRecycle()
                    if (rotatedBitmap !== bitmap) bitmap.safeRecycle()
                    workingFile.safeDelete()
                    return SqueezeResult.Error(
                        SqueezeError.OUT_OF_MEMORY,
                        e,
                        "Image is too large to process. Please try taking a lower-resolution picture."
                    )
                }
            }

            rotatedBitmap.safeRecycle()
            if (rotatedBitmap !== bitmap) bitmap.safeRecycle()

            val writeResult = FileUtil.safeCopyFile(workingFile, destinationFile)
            if (writeResult != null) {
                workingFile.safeDelete()
                return SqueezeResult.Error(
                    SqueezeError.COPY_FAILED,
                    writeResult,
                    "Failed to save the compressed file. Check device storage."
                )
            }
            
            workingFile.safeDelete()

            val compressedSize = destinationFile.length()
            val ratio = if (originalSize > 0) {
                ((originalSize - compressedSize).toDouble() / originalSize * 100)
            } else 0.0

            Timber.i(
                "$TAG [COMPRESS] \uD83D\uDCC9 Original: %s -> Compressed: %s | Quality: %d%% | Saved: %.2f%%",
                FileUtil.formatFileSize(originalSize),
                FileUtil.formatFileSize(compressedSize),
                compressQuality,
                ratio
            )

            return SqueezeResult.Success(destinationFile)

        } catch (oom: OutOfMemoryError) {
            Timber.e(oom, "$TAG: OutOfMemoryError during compression")
            return SqueezeResult.Error(
                SqueezeError.OUT_OF_MEMORY,
                oom,
                "Image is too large to process. Please try taking a lower-resolution picture."
            )
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Unexpected error during compression")
            return SqueezeResult.Error(
                SqueezeError.UNKNOWN,
                e,
                "An unexpected application error occurred during compression. Please try again."
            )
        }
    }
}
