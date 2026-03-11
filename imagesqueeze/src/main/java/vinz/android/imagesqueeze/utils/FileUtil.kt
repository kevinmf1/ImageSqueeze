package vinz.android.imagesqueeze.utils

import android.content.Context
import android.os.StatFs
import timber.log.Timber
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

internal object FileUtil {
    private const val MIN_FREE_SPACE_BYTES = 10L * 1024 * 1024 // 10 MB safety margin
    private const val TAG = "ImageSqueeze_FileUtil"

    fun hasEnoughDiskSpace(context: Context): Boolean {
        return try {
            val stat = StatFs(context.cacheDir.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes > MIN_FREE_SPACE_BYTES
        } catch (e: Exception) {
            Timber.w(e, "$TAG: Cannot check disk space, proceeding anyway")
            true
        }
    }

    fun safeCopyFile(source: File, dest: File): Exception? {
        if (source.absolutePath == dest.absolutePath) return null

        return try {
            if (dest.exists()) {
                if (!dest.delete()) {
                    val tempFile = File(dest.parent, "${dest.nameWithoutExtension}_tmp.${dest.extension}")
                    source.copyTo(tempFile, overwrite = true)
                    if (!tempFile.renameTo(dest)) {
                        tempFile.copyTo(dest, overwrite = true)
                        tempFile.safeDelete()
                    }
                    return null
                }
            }

            dest.parentFile?.mkdirs()
            source.copyTo(dest, overwrite = true)
            null
        } catch (e: Exception) {
            Timber.e(e, "$TAG: safeCopyFile failed: ${source.absolutePath} -> ${dest.absolutePath}")
            e
        }
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(
            size / 1024.0.pow(digitGroups.toDouble())
        ) + " " + units[digitGroups]
    }

    fun File.safeDelete() {
        try {
            if (exists()) delete()
        } catch (e: Exception) {
            Timber.w(e, "$TAG: Failed to delete $absolutePath")
        }
    }
}
