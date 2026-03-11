package vinz.android.imagesqueeze

import android.content.Context
import vinz.android.imagesqueeze.core.CompressorCore
import java.io.File

/**
 * Main entry point for ImageSqueeze library.
 * A robust and safe image compressor alternative to Zelory.
 */
object ImageSqueeze {

    /**
     * Suspend function to compress an image asynchronously using coroutines.
     *
     * @param context the application context.
     * @param source the original image file.
     * @param destination the destination file where compressed image will be saved (defaults to cache directory).
     * @param configBlock DSL block to customize compression parameters (size, format, quality, dimensions).
     * @return SqueezeResult containing either the Success (with output File) or Error (with details).
     */
    suspend fun compress(
        context: Context,
        source: File,
        destination: File = File(context.cacheDir, "compressed_${source.name}"),
        configBlock: CompressionConfig.() -> Unit = {}
    ): SqueezeResult {
        val config = CompressionConfig().apply(configBlock)
        return CompressorCore.compress(context, source, destination, config)
    }

    /**
     * Synchronous function to compress an image on the current thread.
     * Note: Prefer [compress] to avoid freezing the main thread.
     */
    fun compressSync(
        context: Context,
        source: File,
        destination: File = File(context.cacheDir, "compressed_${source.name}"),
        configBlock: CompressionConfig.() -> Unit = {}
    ): SqueezeResult {
        val config = CompressionConfig().apply(configBlock)
        return CompressorCore.compressSync(context, source, destination, config)
    }
}
