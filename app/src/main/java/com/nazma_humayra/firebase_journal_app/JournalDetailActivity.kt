package com.nazma_humayra.firebase_journal_app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import com.nazma_humayra.firebase_journal_app.databinding.ActivityJournalDetailBinding
import java.text.SimpleDateFormat
import java.util.Locale

class JournalDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJournalDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJournalDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.parseColor("#596B8F")

        // Ensure the status bar text color is appropriate for the background color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        // Retrieve the journal object from intent
        val journal = intent.getParcelableExtra<Journal>("journal")

        // Populate the views
        binding.detailTitle.text = journal?.title
        binding.detailThoughts.text = journal?.thoughts

        // Format and display the timestamp
        journal?.timeAdded?.let {
            val formatter = SimpleDateFormat("h:mm a, MM/dd/yyyy", Locale.getDefault())
            binding.detailTimestamp.text = formatter.format(it.toDate())
        }

        // Load image using Glide
        val imageUrl = journal?.imageUrls?.firstOrNull()
        Glide.with(this)
            .load(imageUrl)
            .into(binding.detailImage)
    }
}
