package vinz.android.imagesqueeze

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vinz.android.imagesqueeze.extensions.squeeze
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var btnPick: Button
    private lateinit var btnCamera: Button
    private lateinit var ivOriginalMini: ImageView
    private lateinit var tvOriginalSize: TextView
    private lateinit var lytOriginal: LinearLayout
    private lateinit var ivCompressedMini: ImageView
    private lateinit var tvCompressedSize: TextView
    private lateinit var lytCompressed: LinearLayout
    private lateinit var tvPreviewTitle: TextView
    private lateinit var ivLargePreview: ImageView
    
    private lateinit var lytZelory: LinearLayout
    private lateinit var ivZeloryMini: ImageView
    private lateinit var tvZelorySize: TextView

    private var currentCameraFile: File? = null
    private var originalFile: File? = null
    private var compressedFile: File? = null
    private var zeloryFile: File? = null

    // Activity Result Launcher for picking from Gallery
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            handleImageUri(uri)
        }
    }

    // Activity Result Launcher for Camera
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            currentCameraFile?.let { file ->
                processOriginalFile(file)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()

        btnPick.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnCamera.setOnClickListener {
            val file = createTempImageFile()
            currentCameraFile = file
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            takePictureLauncher.launch(uri)
        }

        lytOriginal.setOnClickListener {
            originalFile?.let {
                ivLargePreview.setImageURI(Uri.fromFile(it))
                tvPreviewTitle.text = "Large Preview: Original"
            }
        }

        lytCompressed.setOnClickListener {
            compressedFile?.let {
                ivLargePreview.setImageURI(Uri.fromFile(it))
                tvPreviewTitle.text = "Large Preview: ImageSqueeze"
            }
        }

        lytZelory.setOnClickListener {
            zeloryFile?.let {
                ivLargePreview.setImageURI(Uri.fromFile(it))
                tvPreviewTitle.text = "Large Preview: Zelory"
            }
        }
    }

    private fun initViews() {
        btnPick = findViewById(R.id.btnPick)
        btnCamera = findViewById(R.id.btnCamera)
        ivOriginalMini = findViewById(R.id.ivOriginalMini)
        tvOriginalSize = findViewById(R.id.tvOriginalSize)
        lytOriginal = findViewById(R.id.lytOriginal)
        ivCompressedMini = findViewById(R.id.ivCompressedMini)
        tvCompressedSize = findViewById(R.id.tvCompressedSize)
        lytCompressed = findViewById(R.id.lytCompressed)
        tvPreviewTitle = findViewById(R.id.tvPreviewTitle)
        ivLargePreview = findViewById(R.id.ivLargePreview)
        
        lytZelory = findViewById(R.id.lytZelory)
        ivZeloryMini = findViewById(R.id.ivZeloryMini)
        tvZelorySize = findViewById(R.id.tvZelorySize)
    }

    private fun handleImageUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val tempSource = createTempImageFile()
                    val outputStream = FileOutputStream(tempSource)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    
                    withContext(Dispatchers.Main) {
                        processOriginalFile(tempSource)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun processOriginalFile(file: File) {
        originalFile = file
        ivOriginalMini.setImageURI(Uri.fromFile(file))
        tvOriginalSize.text = "Size: ${formatSize(file.length())}"

        // Now compress it
        compressImage(file)
    }

    private fun compressImage(file: File) {
        val originalSize = file.length()
        
        lifecycleScope.launch {
            val result = file.squeeze(this@MainActivity) {
                // Here we customize the config
                resolution(1024, 1024)
                quality(85)
                size(500_000L) // Target around 500KB
            }

            when (result) {
                is SqueezeResult.Success -> {
                    compressedFile = result.file
                    ivCompressedMini.setImageURI(Uri.fromFile(result.file))
                    val newSize = result.file.length()
                    val saved = if (originalSize > 0) ((originalSize - newSize).toDouble() / originalSize * 100).toInt() else 0
                    tvCompressedSize.text = "Size: ${formatSize(newSize)}\nSaved: $saved%"
                    
                    // Show large preview automatically for compressed
                    ivLargePreview.setImageURI(Uri.fromFile(result.file))
                    tvPreviewTitle.text = "Large Preview: ImageSqueeze"
                }
                is SqueezeResult.Error -> {
                    Toast.makeText(this@MainActivity, "ImageSqueeze Error: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        lifecycleScope.launch {
            try {
                val zFile = Compressor.compress(this@MainActivity, file) {
                    resolution(1024, 1024)
                    quality(85)
                    size(500_000L)
                }
                zeloryFile = zFile
                ivZeloryMini.setImageURI(Uri.fromFile(zFile))
                val zSize = zFile.length()
                val zSaved = if (originalSize > 0) ((originalSize - zSize).toDouble() / originalSize * 100).toInt() else 0
                tvZelorySize.text = "Size: ${formatSize(zSize)}\nSaved: $zSaved%"
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Zelory Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createTempImageFile(): File {
        val dir = File(cacheDir, "images")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "IMG_${System.currentTimeMillis()}.jpg")
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(
            size / Math.pow(1024.0, digitGroups.toDouble())
        ) + " " + units[digitGroups]
    }
}