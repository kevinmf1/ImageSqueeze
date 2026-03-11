package vinz.android.imagesqueeze

import org.junit.Assert.assertEquals
import org.junit.Test

class SqueezeErrorTest {

    @Test
    fun `all error types are defined`() {
        val allErrors = SqueezeError.entries
        assertEquals(8, allErrors.size)
    }

    @Test
    fun `error types have correct names`() {
        assertEquals("FILE_NOT_FOUND", SqueezeError.FILE_NOT_FOUND.name)
        assertEquals("NOT_READABLE", SqueezeError.NOT_READABLE.name)
        assertEquals("FILE_EMPTY", SqueezeError.FILE_EMPTY.name)
        assertEquals("NO_DISK_SPACE", SqueezeError.NO_DISK_SPACE.name)
        assertEquals("DECODE_FAILED", SqueezeError.DECODE_FAILED.name)
        assertEquals("OUT_OF_MEMORY", SqueezeError.OUT_OF_MEMORY.name)
        assertEquals("COPY_FAILED", SqueezeError.COPY_FAILED.name)
        assertEquals("UNKNOWN", SqueezeError.UNKNOWN.name)
    }

    @Test
    fun `valueOf returns correct enum from string name`() {
        assertEquals(SqueezeError.FILE_NOT_FOUND, SqueezeError.valueOf("FILE_NOT_FOUND"))
        assertEquals(SqueezeError.OUT_OF_MEMORY, SqueezeError.valueOf("OUT_OF_MEMORY"))
        assertEquals(SqueezeError.UNKNOWN, SqueezeError.valueOf("UNKNOWN"))
    }

    @Test
    fun `each error type can be used in when expression`() {
        for (error in SqueezeError.entries) {
            val message = when (error) {
                SqueezeError.FILE_NOT_FOUND -> "file not found"
                SqueezeError.NOT_READABLE -> "not readable"
                SqueezeError.FILE_EMPTY -> "file empty"
                SqueezeError.NO_DISK_SPACE -> "no disk space"
                SqueezeError.DECODE_FAILED -> "decode failed"
                SqueezeError.OUT_OF_MEMORY -> "out of memory"
                SqueezeError.COPY_FAILED -> "copy failed"
                SqueezeError.UNKNOWN -> "unknown"
            }
            // If this compiles and runs, the when is exhaustive
            assert(message.isNotEmpty())
        }
    }
}
