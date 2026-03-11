package vinz.android.imagesqueeze

import java.io.File

/**
 * Result wrapper for the compression process, enabling developer-customized handling.
 */
sealed class SqueezeResult {
    data class Success(val file: File) : SqueezeResult()
    
    data class Error(
        val errorType: SqueezeError, 
        val exception: Throwable? = null, 
        val message: String
    ) : SqueezeResult()
}
