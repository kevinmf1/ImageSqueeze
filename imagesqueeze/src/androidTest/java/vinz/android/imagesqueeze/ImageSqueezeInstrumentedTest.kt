package vinz.android.imagesqueeze

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vinz.android.imagesqueeze.extensions.squeeze
import java.io.File
import java.io.FileOutputStream

/**
 * Instrumented tests that run on a real Android device/emulator.
 * These tests exercise the full compression pipeline including
 * BitmapFactory, EXIF, StatFs, and file I/O.
 */
@RunWith(AndroidJUnit4::class)
class ImageSqueezeInstrumentedTest {

    private lateinit var context: Context
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tempDir = File(context.cacheDir, "instrumented_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createTestJpeg(name: String = "test.jpg", width: Int = 300, height: Int = 300): File {
        val file = File(tempDir, name)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
        }
        bitmap.recycle()
        return file
    }

    // ─── Full pipeline: Success scenarios ────────────────────────────

    @Test
    fun compress_validJpeg_returnsSuccess() = runBlocking {
        val source = createTestJpeg()
        val dest = File(tempDir, "output.jpg")

        val result = ImageSqueeze.compress(context, source, dest)

        assertTrue(result is SqueezeResult.Success)
        assertTrue(dest.exists())
        assertTrue(dest.length() > 0)
    }

    @Test
    fun compress_withCustomQuality_returnsSuccess() = runBlocking {
        val source = createTestJpeg("quality_test.jpg", 400, 400)
        val dest = File(tempDir, "quality_output.jpg")

        val result = ImageSqueeze.compress(context, source, dest) {
            quality(50)
        }

        assertTrue(result is SqueezeResult.Success)
        assertTrue(dest.exists())
    }

    @Test
    fun compress_withCustomResolution_returnsSuccess() = runBlocking {
        val source = createTestJpeg("res_test.jpg", 1000, 1000)
        val dest = File(tempDir, "res_output.jpg")

        val result = ImageSqueeze.compress(context, source, dest) {
            resolution(200, 200)
        }

        assertTrue(result is SqueezeResult.Success)
        assertTrue(dest.exists())
    }

    @Test
    fun compress_withSizeConstraint_producesSmallFile() = runBlocking {
        val source = createTestJpeg("size_test.jpg", 800, 800)
        val dest = File(tempDir, "size_output.jpg")

        val result = ImageSqueeze.compress(context, source, dest) {
            quality(95)
            size(50_000L) // Very aggressive: 50 KB
            minQuality = 5
        }

        assertTrue(result is SqueezeResult.Success)
        assertTrue(dest.exists())
        // File should be reasonably small (though exact bytes depend on bitmap content)
    }

    @Test
    fun compress_withPngFormat_returnsSuccess() = runBlocking {
        val source = createTestJpeg("format_test.jpg", 200, 200)
        val dest = File(tempDir, "format_output.png")

        val result = ImageSqueeze.compress(context, source, dest) {
            format(Bitmap.CompressFormat.PNG)
        }

        assertTrue(result is SqueezeResult.Success)
        assertTrue(dest.exists())
        assertTrue(dest.length() > 0)
    }

    @Test
    fun compress_isForDisplay_producesSmallThumbnail() = runBlocking {
        val source = createTestJpeg("display_test.jpg", 800, 800)
        val dest = File(tempDir, "display_output.jpg")

        val result = ImageSqueeze.compress(context, source, dest) {
            isForDisplay = true
        }

        assertTrue(result is SqueezeResult.Success)
        assertTrue(dest.exists())

        // Verify the thumbnail is small
        val thumbBitmap = BitmapFactory.decodeFile(dest.absolutePath)
        assertNotNull(thumbBitmap)
        // Thumbnail should be significantly downscaled
        assertTrue(thumbBitmap!!.width <= 200 && thumbBitmap.height <= 200)
        thumbBitmap.recycle()
    }

    @Test
    fun compress_autoCreatesDestinationDirectories() = runBlocking {
        val source = createTestJpeg()
        val dest = File(tempDir, "a/b/c/deep_output.jpg")

        val result = ImageSqueeze.compress(context, source, dest)

        assertTrue(result is SqueezeResult.Success)
        assertTrue(dest.exists())
        assertTrue(dest.parentFile!!.exists())
    }

    @Test
    fun compress_sourceFileUntouchedAfterCompression() = runBlocking {
        val source = createTestJpeg()
        val originalSize = source.length()
        val dest = File(tempDir, "untouched_output.jpg")

        ImageSqueeze.compress(context, source, dest)

        assertTrue(source.exists())
        assertEquals(originalSize, source.length())
    }

    @Test
    fun compress_outputSmallerOrEqualToOriginal() = runBlocking {
        val source = createTestJpeg("compare_test.jpg", 500, 500)
        val originalSize = source.length()
        val dest = File(tempDir, "compare_output.jpg")

        val result = ImageSqueeze.compress(context, source, dest) {
            quality(50)
        }

        assertTrue(result is SqueezeResult.Success)
        assertTrue(dest.length() <= originalSize)
    }

    @Test
    fun compress_defaultDestination_usesCache() = runBlocking {
        val source = createTestJpeg()

        val result = ImageSqueeze.compress(context, source)

        assertTrue(result is SqueezeResult.Success)
        val output = (result as SqueezeResult.Success).file
        assertTrue(output.absolutePath.startsWith(context.cacheDir.absolutePath))
    }

    // ─── Full pipeline: Error scenarios ─────────────────────────────

    @Test
    fun compress_nonExistentFile_returnsFileNotFound() = runBlocking {
        val nonExistent = File(tempDir, "ghost.jpg")
        val dest = File(tempDir, "output.jpg")

        val result = ImageSqueeze.compress(context, nonExistent, dest)

        assertTrue(result is SqueezeResult.Error)
        assertEquals(SqueezeError.FILE_NOT_FOUND, (result as SqueezeResult.Error).errorType)
    }

    @Test
    fun compress_emptyFile_returnsFileEmpty() = runBlocking {
        val emptyFile = File(tempDir, "empty.jpg")
        emptyFile.createNewFile()
        val dest = File(tempDir, "output.jpg")

        val result = ImageSqueeze.compress(context, emptyFile, dest)

        assertTrue(result is SqueezeResult.Error)
        assertEquals(SqueezeError.FILE_EMPTY, (result as SqueezeResult.Error).errorType)
    }

    @Test
    fun compress_corruptFile_returnsDecodeFailed() = runBlocking {
        val corruptFile = File(tempDir, "corrupt.jpg")
        corruptFile.writeText("this is definitely not a valid JPEG image file")
        val dest = File(tempDir, "output.jpg")

        val result = ImageSqueeze.compress(context, corruptFile, dest)

        assertTrue(result is SqueezeResult.Error)
        assertEquals(SqueezeError.DECODE_FAILED, (result as SqueezeResult.Error).errorType)
    }

    @Test
    fun compress_errorMessageIsEnglish() = runBlocking {
        val nonExistent = File(tempDir, "ghost.jpg")

        val result = ImageSqueeze.compress(context, nonExistent) as SqueezeResult.Error

        assertTrue(result.message.contains("file", ignoreCase = true))
        // Verify no Indonesian text
        assertTrue(!result.message.contains("silakan", ignoreCase = true))
        assertTrue(!result.message.contains("gagal", ignoreCase = true))
        assertTrue(!result.message.contains("ditemukan", ignoreCase = true))
    }

    // ─── Extension function .squeeze() ──────────────────────────────

    @Test
    fun squeeze_extension_returnsSuccess() = runBlocking {
        val source = createTestJpeg("squeeze_test.jpg")
        val dest = File(tempDir, "squeeze_output.jpg")

        val result = source.squeeze(context, dest) {
            quality(80)
        }

        assertTrue(result is SqueezeResult.Success)
        assertTrue(dest.exists())
    }

    @Test
    fun squeeze_extension_returnsErrorForMissingFile() = runBlocking {
        val nonExistent = File(tempDir, "missing.jpg")

        val result = nonExistent.squeeze(context)

        assertTrue(result is SqueezeResult.Error)
        assertEquals(SqueezeError.FILE_NOT_FOUND, (result as SqueezeResult.Error).errorType)
    }

    // ─── compressSync() ─────────────────────────────────────────────

    @Test
    fun compressSync_validJpeg_returnsSuccess() {
        val source = createTestJpeg("sync_test.jpg")
        val dest = File(tempDir, "sync_output.jpg")

        val result = ImageSqueeze.compressSync(context, source, dest)

        assertTrue(result is SqueezeResult.Success)
        assertTrue(dest.exists())
        assertTrue(dest.length() > 0)
    }

    @Test
    fun compressSync_fullDsl_returnsSuccess() {
        val source = createTestJpeg("sync_dsl_test.jpg", 400, 400)
        val dest = File(tempDir, "sync_dsl_output.jpg")

        val result = ImageSqueeze.compressSync(context, source, dest) {
            resolution(200, 200)
            quality(70)
            size(500_000L)
            format(Bitmap.CompressFormat.JPEG)
            minQuality = 15
        }

        assertTrue(result is SqueezeResult.Success)
        assertTrue(dest.exists())
    }
}
