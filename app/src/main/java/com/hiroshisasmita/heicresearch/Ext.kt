package com.hiroshisasmita.heicresearch

import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.Glide

fun ImageView.loadImage(bitmap: Bitmap) {
    Glide.with(this)
        .load(bitmap)
        .into(this)
}