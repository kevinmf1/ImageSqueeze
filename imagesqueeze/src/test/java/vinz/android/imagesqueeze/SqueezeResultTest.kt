package vinz.android.imagesqueeze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class SqueezeResultTest {

    @Test
    fun `Success contains the compressed file`() {
        val file = File("/tmp/compressed.jpg")
        val result = SqueezeResult.Success(file)

        assertTrue(result is SqueezeResult.Success)
        assertEquals(file, result.file)
    }

    @Test
    fun `Error contains errorType, exception, and message`() {
        val exception = IOException("test error")
        val result = SqueezeResult.Error(
            errorType = SqueezeError.FILE_NOT_FOUND,
            exception = exception,
            message = "Source file not found."
        )

        assertTrue(result is SqueezeResult.Error)
        assertEquals(SqueezeError.FILE_NOT_FOUND, result.errorType)
        assertEquals(exception, result.exception)
        assertEquals("Source file not found.", result.message)
    }

    @Test
    fun `Error can have null exception`() {
        val result = SqueezeResult.Error(
            errorType = SqueezeError.UNKNOWN,
            exception = null,
            message = "Unknown error"
        )

        assertNull(result.exception)
        assertEquals(SqueezeError.UNKNOWN, result.errorType)
    }

    @Test
    fun `when expression correctly matches Success`() {
        val result: SqueezeResult = SqueezeResult.Success(File("/tmp/test.jpg"))

        val isSuccess = when (result) {
            is SqueezeResult.Success -> true
            is SqueezeResult.Error -> false
        }

        assertTrue(isSuccess)
    }

    @Test
    fun `when expression correctly matches Error`() {
        val result: SqueezeResult = SqueezeResult.Error(
            SqueezeError.OUT_OF_MEMORY,
            OutOfMemoryError("OOM"),
            "Out of memory"
        )

        val isError = when (result) {
            is SqueezeResult.Success -> false
            is SqueezeResult.Error -> true
        }

        assertTrue(isError)
    }

    @Test
    fun `Error with OutOfMemoryError as exception`() {
        val oom = OutOfMemoryError("heap full")
        val result = SqueezeResult.Error(
            SqueezeError.OUT_OF_MEMORY,
            oom,
            "Image is too large"
        )

        assertNotNull(result.exception)
        assertTrue(result.exception is OutOfMemoryError)
    }
}
