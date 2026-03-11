package vinz.android.imagesqueeze.core

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import vinz.android.imagesqueeze.CompressionConfig
import vinz.android.imagesqueeze.SqueezeError
import vinz.android.imagesqueeze.SqueezeResult
import java.io.File

/**
 * Unit tests for CompressorCore that test validation/guard behavior.
 *
 * Tests involving actual bitmap decode/compress are placed in androidTest
 * because Robolectric's BitmapFactory and StatFs shadows have limitations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CompressorCoreTest {

    private lateinit var context: Context
    private lateinit var tempDir: File
    private lateinit var destFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tempDir = File(context.cacheDir, "test_compressor_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        destFile = File(tempDir, "dest.jpg")
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // ─── Scenario 1: Source file does not exist ─────────────────────

    @Test
    fun `returns FILE_NOT_FOUND when source file does not exist`() {
        val nonExistent = File(tempDir, "does_not_exist.jpg")
        val config = CompressionConfig()

        val result = CompressorCore.compressSync(context, nonExistent, destFile, config)

        assertTrue(result is SqueezeResult.Error)
        assertEquals(SqueezeError.FILE_NOT_FOUND, (result as SqueezeResult.Error).errorType)
    }

    @Test
    fun `FILE_NOT_FOUND error has English message`() {
        val nonExistent = File(tempDir, "nope.jpg")
        val config = CompressionConfig()

        val result = CompressorCore.compressSync(context, nonExistent, destFile, config)

        assertTrue(result is SqueezeResult.Error)
        val error = result as SqueezeResult.Error
        assertTrue(error.message.contains("file", ignoreCase = true))
        assertTrue(!error.message.contains("silakan", ignoreCase = true))
        assertTrue(!error.message.contains("gagal", ignoreCase = true))
    }

    @Test
    fun `FILE_NOT_FOUND error has non-null exception`() {
        val nonExistent = File(tempDir, "nope.jpg")
        val config = CompressionConfig()

        val result = CompressorCore.compressSync(context, nonExistent, destFile, config)

        assertTrue(result is SqueezeResult.Error)
        assertNotNull((result as SqueezeResult.Error).exception)
    }

    // ─── Scenario 2: Source file is empty ───────────────────────────

    @Test
    fun `returns FILE_EMPTY when source file is 0 bytes`() {
        val sourceFile = File(tempDir, "empty.jpg")
        sourceFile.createNewFile()
        val config = CompressionConfig()

        val result = CompressorCore.compressSync(context, sourceFile, destFile, config)

        assertTrue(result is SqueezeResult.Error)
        assertEquals(SqueezeError.FILE_EMPTY, (result as SqueezeResult.Error).errorType)
    }

    @Test
    fun `FILE_EMPTY error message is in English`() {
        val sourceFile = File(tempDir, "empty.jpg")
        sourceFile.createNewFile()
        val config = CompressionConfig()

        val result = CompressorCore.compressSync(context, sourceFile, destFile, config) as SqueezeResult.Error

        assertTrue(result.message.contains("empty", ignoreCase = true))
    }

    // ─── Scenario 3: Source file remains untouched on error ─────────

    @Test
    fun `source file is preserved after FILE_NOT_FOUND error`() {
        val nonExistent = File(tempDir, "ghost.jpg")

        CompressorCore.compressSync(context, nonExistent, destFile, CompressionConfig())

        // Original assertions: source didn't exist and still doesn't
        assertTrue(!nonExistent.exists())
    }

    // ─── Scenario 4: Suspend function delegates correctly ───────────

    @Test
    fun `compress suspend returns same error as compressSync for missing file`() = runTest {
        val nonExistent = File(tempDir, "nope.jpg")
        val config = CompressionConfig()

        val syncResult = CompressorCore.compressSync(context, nonExistent, destFile, config)
        val asyncResult = CompressorCore.compress(context, nonExistent, destFile, config)

        assertTrue(syncResult is SqueezeResult.Error)
        assertTrue(asyncResult is SqueezeResult.Error)
        assertEquals(
            (syncResult as SqueezeResult.Error).errorType,
            (asyncResult as SqueezeResult.Error).errorType
        )
    }

    @Test
    fun `compress suspend returns same error as compressSync for empty file`() = runTest {
        val sourceFile = File(tempDir, "empty.jpg")
        sourceFile.createNewFile()
        val config = CompressionConfig()

        val syncResult = CompressorCore.compressSync(context, sourceFile, destFile, config)
        val asyncResult = CompressorCore.compress(context, sourceFile, destFile, config)

        assertEquals(
            (syncResult as SqueezeResult.Error).errorType,
            (asyncResult as SqueezeResult.Error).errorType
        )
    }
}
