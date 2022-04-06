package linc.com.heifconverter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat.getExternalFilesDirs
import com.hiroshisasmita.heicresearch.library.HeifReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import linc.com.heifconverter.HeifConverter.Format.JPEG
import linc.com.heifconverter.HeifConverter.Format.PNG
import linc.com.heifconverter.HeifConverter.Format.WEBP
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class HeifConverter(private val context: Context){

    private var pathToHeicFile: String? = null
    private var url: String? = null
    private var resId: Int? = null
    private var inputStream: InputStream? = null
    private var byteArray: ByteArray? = null

    private var outputQuality: Int = 100
    private var saveResultImage: Boolean = true
    private lateinit var outputFormat: String
    private lateinit var convertedFileName: String
    private lateinit var pathToSaveDirectory: String

    private var fromDataType = InputDataType.NONE

    init {
        HeifReader.initialize(context)
        initDefaultValues()
    }

    fun fromFile(pathToFile: String) : HeifConverter {
        if(!File(pathToFile).exists()) {
            throw FileNotFoundException("HEIC file not found!")
        }
        this.pathToHeicFile = pathToFile
        this.fromDataType = InputDataType.FILE
        return this
    }

    fun fromInputStream(inputStream: InputStream) : HeifConverter {
        this.inputStream = inputStream
        this.fromDataType = InputDataType.INPUT_STREAM
        return this
    }

    fun fromResource(id: Int) : HeifConverter {
        val isResValid = context.resources.getIdentifier(
            context.resources.getResourceName(id),
            "drawable",
            context.packageName
        ) != 0
        if(!isResValid)
            throw FileNotFoundException("Resource not found!")
        this.fromDataType = InputDataType.RESOURCES
        return this
    }

    fun fromUrl(heicImageUrl: String) : HeifConverter {
        this.url = heicImageUrl
        this.fromDataType = InputDataType.URL
        return this
    }

    fun fromByteArray(data: ByteArray) : HeifConverter {
        if(data.isEmpty())
            throw FileNotFoundException("Empty byte array!")
        this.byteArray = data
        this.fromDataType = InputDataType.BYTE_ARRAY
        return this
    }

    fun withOutputFormat(format: String) : HeifConverter {
        this.outputFormat = format
        return this
    }

    fun withOutputQuality(quality: Int) : HeifConverter {
        this.outputQuality = when {
            quality > 100 -> 100
            quality < 0 -> 0
            else -> quality
        }
        return this
    }

    fun saveResultImage(saveResultImage: Boolean) : HeifConverter {
        this.saveResultImage = saveResultImage
        return this
    }

    suspend fun convert(block: (result: Map<String, Any?>) -> Unit) {
        // Android versions below Q
        withContext(Dispatchers.Main) {
            var bitmap: Bitmap? = null

            // Handle Android Q version in every case
            withContext(Dispatchers.IO) {
                bitmap = when (fromDataType) {
                    InputDataType.FILE -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) BitmapFactory.decodeFile(pathToHeicFile)
                        else HeifReader.decodeFile(pathToHeicFile)
                    }
                    InputDataType.URL -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                // Download image
                                val url = URL(url)
                                val connection = url.openConnection() as HttpURLConnection
                                connection.doInput = true
                                connection.connect()
                                val input: InputStream = connection.inputStream
                                BitmapFactory.decodeStream(input)
                        } else HeifReader.decodeUrl(url!!)
                    }
                    InputDataType.RESOURCES -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) BitmapFactory.decodeResource(context.resources, resId!!)
                        else HeifReader.decodeResource(context.resources, resId!!)
                    }
                    InputDataType.INPUT_STREAM -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) BitmapFactory.decodeStream(inputStream!!)
                        else HeifReader.decodeStream(inputStream!!)
                    }
                    InputDataType.BYTE_ARRAY -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            BitmapFactory.decodeByteArray(
                                byteArray!!,
                                0,
                                byteArray!!.size,
                                BitmapFactory.Options().apply {
                                    inJustDecodeBounds = true
                                }
                            )
                        } else HeifReader.decodeByteArray(byteArray!!)
                    }
                    else -> throw IllegalStateException("You forget to pass input type: File, Url etc. Use such functions: fromFile(. . .) etc.")
                }
            }

            val directoryToSave = File(pathToSaveDirectory)
            val dest = File(directoryToSave, "$convertedFileName$outputFormat")

            withContext(Dispatchers.IO) {
                val out = FileOutputStream(dest)
                try {
                    bitmap?.compress(useFormat(outputFormat), outputQuality, out)
                    if(!saveResultImage) {
                        dest.delete()
                    }
                    withContext(Dispatchers.Main) {
                        block(mapOf(
                            Key.BITMAP to bitmap,
                            Key.IMAGE_PATH to (dest.path ?: "You set saveResultImage(false). If you want to save file - pass true"))
                        )
                    }
                } catch (e : Exception) {
                    e.printStackTrace()
                }finally {
                    out.flush()
                    out.close()
                }
            }
        }
    }

    private fun initDefaultValues() {
        outputFormat = JPEG
        convertedFileName  = UUID.randomUUID().toString()
        pathToSaveDirectory = getExternalFilesDirs(context, Environment.DIRECTORY_DCIM)[0].path
    }

    private fun useFormat(format: String) = when(format) {
        WEBP -> Bitmap.CompressFormat.WEBP
        PNG -> Bitmap.CompressFormat.PNG
        else -> Bitmap.CompressFormat.JPEG
    }

    object Format {
        const val JPEG = ".jpg"
        const val PNG = ".png"
        const val WEBP = ".webp"
    }

    object Key {
        const val BITMAP = "converted_bitmap_heic"
        const val IMAGE_PATH = "path_to_converted_heic"
    }

    private enum class InputDataType {
        FILE, URL, RESOURCES, INPUT_STREAM,
        BYTE_ARRAY, NONE
    }

}
