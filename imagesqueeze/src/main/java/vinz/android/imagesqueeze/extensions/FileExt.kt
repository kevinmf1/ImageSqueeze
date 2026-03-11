package vinz.android.imagesqueeze.extensions

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import vinz.android.imagesqueeze.CompressionConfig
import vinz.android.imagesqueeze.ImageSqueeze
import vinz.android.imagesqueeze.SqueezeResult
import java.io.File

/**
 * Extension function for File to perform async compression natively.
 */
suspend fun File.squeeze(
    context: Context,
    destination: File = File(context.cacheDir, "compressed_${this.name}"),
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    configBlock: CompressionConfig.() -> Unit = {}
): SqueezeResult {
    return ImageSqueeze.compress(context, this, destination, dispatcher, configBlock)
}

/**
 * Extension function for File to perform synchronous compression natively.
 */
fun File.squeezeSync(
    context: Context,
    destination: File = File(context.cacheDir, "compressed_${this.name}"),
    configBlock: CompressionConfig.() -> Unit = {}
): SqueezeResult {
    return ImageSqueeze.compressSync(context, this, destination, configBlock)
}
