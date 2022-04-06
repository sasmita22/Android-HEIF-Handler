package com.hiroshisasmita.heicresearch

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.hiroshisasmita.heicresearch.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import linc.com.heifconverter.HeifConverter

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        lifecycleScope.launch {
            HeifConverter(this@MainActivity)
                //.fromUrl("https://github.com/nokiatech/heif/raw/gh-pages/content/images/crowd_1440x960.heic")
                .fromInputStream(resources.openRawResource(R.raw.landscape))
                .withOutputFormat(HeifConverter.Format.PNG)
                .withOutputQuality(100)
                .saveResultImage(false)
                .convert {
                    val bitmap = it[HeifConverter.Key.BITMAP] as Bitmap
                    binding.ivHolder.loadImage(bitmap)
                }
        }
    }
}