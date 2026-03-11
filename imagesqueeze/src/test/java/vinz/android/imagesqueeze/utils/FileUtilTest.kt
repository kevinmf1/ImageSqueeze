package vinz.android.imagesqueeze.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vinz.android.imagesqueeze.utils.FileUtil.safeDelete
import java.io.File

class FileUtilTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "imagesqueeze_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // ─── formatFileSize tests ───────────────────────────────────────

    @Test
    fun `formatFileSize returns 0 B for zero`() {
        assertEquals("0 B", FileUtil.formatFileSize(0))
    }

    @Test
    fun `formatFileSize returns 0 B for negative`() {
        assertEquals("0 B", FileUtil.formatFileSize(-100))
    }

    @Test
    fun `formatFileSize formats bytes correctly`() {
        assertEquals("500 B", FileUtil.formatFileSize(500))
    }

    @Test
    fun `formatFileSize formats kilobytes correctly`() {
        assertEquals("1 KB", FileUtil.formatFileSize(1024))
    }

    @Test
    fun `formatFileSize formats megabytes correctly`() {
        assertEquals("1 MB", FileUtil.formatFileSize(1024 * 1024))
    }

    @Test
    fun `formatFileSize formats fractional kilobytes`() {
        // 1536 bytes = 1.5 KB
        assertEquals("1.5 KB", FileUtil.formatFileSize(1536))
    }

    @Test
    fun `formatFileSize formats gigabytes correctly`() {
        assertEquals("1 GB", FileUtil.formatFileSize(1024L * 1024 * 1024))
    }

    @Test
    fun `formatFileSize formats large MB value correctly`() {
        // 5 MB = 5 * 1024 * 1024 = 5,242,880
        assertEquals("5 MB", FileUtil.formatFileSize(5L * 1024 * 1024))
    }

    // ─── safeCopyFile tests ─────────────────────────────────────────

    @Test
    fun `safeCopyFile returns null when source and dest are the same`() {
        val file = File(tempDir, "same.txt")
        file.writeText("same file")

        val result = FileUtil.safeCopyFile(file, file)
        assertNull(result)
    }

    @Test
    fun `safeCopyFile copies file to new destination successfully`() {
        val source = File(tempDir, "source.txt")
        source.writeText("hello world")

        val dest = File(tempDir, "dest.txt")
        val result = FileUtil.safeCopyFile(source, dest)

        assertNull(result) // null means success
        assertTrue(dest.exists())
        assertEquals("hello world", dest.readText())
    }

    @Test
    fun `safeCopyFile overwrites existing destination`() {
        val source = File(tempDir, "source.txt")
        source.writeText("new content")

        val dest = File(tempDir, "dest.txt")
        dest.writeText("old content")

        val result = FileUtil.safeCopyFile(source, dest)

        assertNull(result)
        assertEquals("new content", dest.readText())
    }

    @Test
    fun `safeCopyFile creates parent directories for destination`() {
        val source = File(tempDir, "source.txt")
        source.writeText("nested test")

        val dest = File(tempDir, "sub/dir/dest.txt")
        val result = FileUtil.safeCopyFile(source, dest)

        assertNull(result)
        assertTrue(dest.exists())
        assertEquals("nested test", dest.readText())
    }

    @Test
    fun `safeCopyFile returns exception when source does not exist`() {
        val source = File(tempDir, "non_existent.txt")
        val dest = File(tempDir, "dest.txt")

        val result = FileUtil.safeCopyFile(source, dest)
        assertNotNull(result)
    }

    @Test
    fun `safeCopyFile preserves source file after copy`() {
        val source = File(tempDir, "source.txt")
        source.writeText("original")

        val dest = File(tempDir, "dest.txt")
        FileUtil.safeCopyFile(source, dest)

        assertTrue(source.exists())
        assertEquals("original", source.readText())
    }

    // ─── safeDelete tests ───────────────────────────────────────────

    @Test
    fun `safeDelete deletes existing file`() {
        val file = File(tempDir, "to_delete.txt")
        file.writeText("delete me")
        assertTrue(file.exists())

        file.safeDelete()
        assertFalse(file.exists())
    }

    @Test
    fun `safeDelete does not throw on non-existent file`() {
        val file = File(tempDir, "non_existent.txt")
        assertFalse(file.exists())

        // Should not throw
        file.safeDelete()
    }
}
