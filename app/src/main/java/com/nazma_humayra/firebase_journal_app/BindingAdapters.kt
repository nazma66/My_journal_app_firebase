package com.nazma_humayra.firebase_journal_app.binding

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

object ImageLoadingUtils {
    // Existing adapter for handling lists of image URLs
    @BindingAdapter("imageUrls")
    @JvmStatic
    fun loadImages(imageView: ImageView, imageUrls: List<String>?) {
        if (!imageUrls.isNullOrEmpty()) {
            Glide.with(imageView.context)
                .load(imageUrls[0]) // Load the first image
                .into(imageView)
        } else {
            imageView.visibility = View.GONE
        }
    }

    // New adapter for handling single image URL strings
    @BindingAdapter("imageUrl")
    @JvmStatic
    fun loadImage(imageView: ImageView, imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(imageView.context)
                .load(imageUrl) // Load image from URL
                .into(imageView)
            imageView.visibility = View.VISIBLE
        } else {
            imageView.visibility = View.GONE
        }
    }

    // Adapter to control the visibility of ImageView
    @BindingAdapter("imageVisibility")
    @JvmStatic
    fun setImageVisibility(view: ImageView, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
