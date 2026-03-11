package vinz.android.imagesqueeze

import android.graphics.Bitmap

/**
 * Configuration class that allows fine-grained control over the image compression logic.
 */
class CompressionConfig {
    var targetWidth: Int = 612
    var targetHeight: Int = 816
    var maxSizeInBytes: Long = 1_000_000L // Default 1 MB threshold
    var minQuality: Int = 10
    var defaultQuality: Int = 80 // Default
    var compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    var isForDisplay: Boolean = false // If true, constraints ignore targetWidth/Height and set to thumbnail size (100x100)

    /**
     * Limits the maximum width and height.
     */
    fun resolution(width: Int, height: Int) {
        this.targetWidth = width
        this.targetHeight = height
    }

    /**
     * Sets the format of the output file (JPEG, WEBP, PNG).
     */
    fun format(format: Bitmap.CompressFormat) {
        this.compressFormat = format
    }

    /**
     * Imposes a target file size limit. The compressor will iteratively squeeze the image
     * to keep it below this threshold.
     */
    fun size(maxFileSizeInBytes: Long) {
        this.maxSizeInBytes = maxFileSizeInBytes
    }

    /**
     * Determines starting encode quality.
     */
    fun quality(quality: Int) {
        this.defaultQuality = quality
    }
}
