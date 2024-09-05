package com.nazma_humayra.firebase_journal_app

import android.os.Parcelable
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Locale

@Parcelize
data class Journal(
    var id: String = "", // Ensure id is non-nullable and initialized
    var userId: String = "",
    var title: String = "",
    var thoughts: String = "",
    var imageUrls: List<String> = listOf(),
    var isBookmarked: Boolean = false,
    var username: String? = null,
    var timeAdded: Timestamp? = null// Username can be nullable
): Parcelable


 {

    // Binding Adapter for image loading
    object ImageLoadingUtils {
        @BindingAdapter("imageUrls")
        @JvmStatic
        fun loadImages(imageView: ImageView, imageUrls: List<String>) {
            // Use Glide or other image loading libraries to display images in a horizontal list or similar
            if (imageUrls.isNotEmpty()) {
                Glide.with(imageView.context)
                    .load(imageUrls[0]) // For demonstration, only loading the first image
                    .into(imageView)
            }
        }
    }


     object BindingAdapters {
        @BindingAdapter("formattedTimestamp")
        @JvmStatic
        fun setFormattedTimestamp(view: TextView, timestamp: Timestamp?) {
            timestamp?.let {
                val date = it.toDate()
                val formatter = SimpleDateFormat("h:mm a, MM/dd/yyyy", Locale.getDefault())
                view.text = formatter.format(date)
            }
        }
    }
}
