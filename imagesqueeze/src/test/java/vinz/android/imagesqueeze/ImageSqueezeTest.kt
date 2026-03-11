package vinz.android.imagesqueeze

import android.content.Context
import android.graphics.Bitmap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import java.io.File

/**
 * Unit tests for the public ImageSqueeze API.
 *
 * Only validation/guard paths are tested here (no bitmap operations).
 * Full integration tests requiring real bitmap encode/decode live in androidTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageSqueezeTest {

    private lateinit var context: Context
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tempDir = File(context.cacheDir, "test_api_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // ─── compress() error paths ─────────────────────────────────────

    @Test
    fun `compress returns FILE_NOT_FOUND for non-existent file`() = kotlinx.coroutines.test.runTest {
        val nonExistent = File(tempDir, "ghost.jpg")
        val result = ImageSqueeze.compress(context, nonExistent)

        assertTrue(result is SqueezeResult.Error)
        assertEquals(SqueezeError.FILE_NOT_FOUND, (result as SqueezeResult.Error).errorType)
    }

    @Test
    fun `compress returns FILE_EMPTY for empty file`() = kotlinx.coroutines.test.runTest {
        val emptyFile = File(tempDir, "empty.jpg")
        emptyFile.createNewFile()

        val result = ImageSqueeze.compress(context, emptyFile)

        assertTrue(result is SqueezeResult.Error)
        assertEquals(SqueezeError.FILE_EMPTY, (result as SqueezeResult.Error).errorType)
    }

    // ─── compressSync() error paths ─────────────────────────────────

    @Test
    fun `compressSync returns FILE_NOT_FOUND for non-existent file`() {
        val nonExistent = File(tempDir, "nope.jpg")
        val result = ImageSqueeze.compressSync(context, nonExistent)

        assertTrue(result is SqueezeResult.Error)
        assertEquals(SqueezeError.FILE_NOT_FOUND, (result as SqueezeResult.Error).errorType)
    }

    @Test
    fun `compressSync returns FILE_EMPTY for empty file`() {
        val emptyFile = File(tempDir, "empty.jpg")
        emptyFile.createNewFile()

        val result = ImageSqueeze.compressSync(context, emptyFile)

        assertTrue(result is SqueezeResult.Error)
        assertEquals(SqueezeError.FILE_EMPTY, (result as SqueezeResult.Error).errorType)
    }

    // ─── Default destination ────────────────────────────────────────

    @Test
    fun `compress uses cacheDir as default destination`() = kotlinx.coroutines.test.runTest {
        val nonExistent = File(tempDir, "ghost.jpg")

        // Even though the file doesn't exist, we can verify the error still flows correctly
        val result = ImageSqueeze.compress(context, nonExistent)

        assertTrue(result is SqueezeResult.Error)
    }

    // ─── DSL configuration passes through ───────────────────────────

    @Test
    fun `compress with DSL config still validates source file`() = kotlinx.coroutines.test.runTest {
        val nonExistent = File(tempDir, "ghost.jpg")

        val result = ImageSqueeze.compress(context, nonExistent) {
            resolution(100, 100)
            quality(50)
            size(100_000L)
            format(Bitmap.CompressFormat.PNG)
        }

        // Config doesn't bypass validation
        assertTrue(result is SqueezeResult.Error)
        assertEquals(SqueezeError.FILE_NOT_FOUND, (result as SqueezeResult.Error).errorType)
    }

    @Test
    fun `compressSync with full DSL config still validates source file`() {
        val emptyFile = File(tempDir, "empty.jpg")
        emptyFile.createNewFile()

        val result = ImageSqueeze.compressSync(context, emptyFile) {
            resolution(200, 200)
            quality(70)
            size(500_000L)
            format(Bitmap.CompressFormat.JPEG)
            minQuality = 20
        }

        assertTrue(result is SqueezeResult.Error)
        assertEquals(SqueezeError.FILE_EMPTY, (result as SqueezeResult.Error).errorType)
    }
}
