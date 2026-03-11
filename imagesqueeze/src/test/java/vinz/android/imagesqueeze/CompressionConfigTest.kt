package vinz.android.imagesqueeze

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CompressionConfigTest {

    @Test
    fun `default config has correct default values`() {
        val config = CompressionConfig()
        assertEquals(612, config.targetWidth)
        assertEquals(816, config.targetHeight)
        assertEquals(1_000_000L, config.maxSizeInBytes)
        assertEquals(10, config.minQuality)
        assertEquals(80, config.defaultQuality)
        assertEquals(Bitmap.CompressFormat.JPEG, config.compressFormat)
        assertFalse(config.isForDisplay)
    }

    @Test
    fun `resolution() updates targetWidth and targetHeight`() {
        val config = CompressionConfig().apply {
            resolution(1920, 1080)
        }
        assertEquals(1920, config.targetWidth)
        assertEquals(1080, config.targetHeight)
    }

    @Test
    fun `format() updates compressFormat`() {
        val config = CompressionConfig().apply {
            format(Bitmap.CompressFormat.PNG)
        }
        assertEquals(Bitmap.CompressFormat.PNG, config.compressFormat)
    }

    @Test
    fun `format() supports WEBP`() {
        val config = CompressionConfig().apply {
            format(Bitmap.CompressFormat.WEBP)
        }
        assertEquals(Bitmap.CompressFormat.WEBP, config.compressFormat)
    }

    @Test
    fun `size() updates maxSizeInBytes`() {
        val config = CompressionConfig().apply {
            size(500_000L)
        }
        assertEquals(500_000L, config.maxSizeInBytes)
    }

    @Test
    fun `quality() updates defaultQuality`() {
        val config = CompressionConfig().apply {
            quality(95)
        }
        assertEquals(95, config.defaultQuality)
    }

    @Test
    fun `minQuality can be set directly`() {
        val config = CompressionConfig().apply {
            minQuality = 30
        }
        assertEquals(30, config.minQuality)
    }

    @Test
    fun `isForDisplay can be set directly (false)`() {
        val config = CompressionConfig().apply {
            isForDisplay = false
        }
        assertEquals(false, config.isForDisplay)
    }

    @Test
    fun `DSL block applies all settings correctly`() {
        val config = CompressionConfig().apply {
            resolution(800, 600)
            quality(70)
            size(250_000L)
            format(Bitmap.CompressFormat.PNG)
            minQuality = 5
        }
        assertEquals(800, config.targetWidth)
        assertEquals(600, config.targetHeight)
        assertEquals(70, config.defaultQuality)
        assertEquals(250_000L, config.maxSizeInBytes)
        assertEquals(Bitmap.CompressFormat.PNG, config.compressFormat)
        assertEquals(5, config.minQuality)
    }
}
