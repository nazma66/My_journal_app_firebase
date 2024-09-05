package com.nazma_humayra.firebase_journal_app



import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.nazma_humayra.firebase_journal_app.databinding.ActivityAddJournalBinding
import java.io.InputStream
import java.util.*

class AddJournalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddJournalBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var storageReference: StorageReference
    private var collectionReference: CollectionReference = db.collection("Journal")
    private var imageUris: MutableList<Uri> = mutableListOf()
    private var currentUserId: String = ""
    private var currentUserName: String = ""

    private val selectImageLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                result.data?.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        Log.d("AddJournalActivity", "Selected image URI: $uri, Scheme: ${uri.scheme}, Path: ${uri.path}")
                        if (isUriValid(uri)) {
                            imageUris.add(uri)
                        } else {
                            Log.e("AddJournalActivity", "Invalid image URI: $uri")
                            Toast.makeText(this, "Invalid image URI", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: run {
                    result.data?.data?.let { uri ->
                        Log.d("AddJournalActivity", "Selected image URI: $uri, Scheme: ${uri.scheme}, Path: ${uri.path}")
                        if (isUriValid(uri)) {
                            imageUris.add(uri)
                        } else {
                            Log.e("AddJournalActivity", "Invalid image URI: $uri")
                            Toast.makeText(this, "Invalid image URI", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                updateImageView()
            }
        }

    private fun isUriValid(uri: Uri): Boolean {
        return uri.scheme?.let { scheme ->
            (scheme == "content" || scheme == "file") && !uri.path.isNullOrEmpty()
        } ?: false
    }

    private fun updateImageView() {
        if (imageUris.isNotEmpty()) {
            val uri = imageUris[0] // Assuming first image for demonstration
            Log.d("AddJournalActivity", "Displaying image URI: $uri")
            Glide.with(this)
                .load(uri)
                .into(binding.postImageView)
        } else {
            binding.postImageView.setImageDrawable(null) // Clear if no images
        }
    }


    private fun uploadImagesAndSaveJournal(title: String, thoughts: String) {
        if (!ensureUserAuthenticated()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val uploadTasks = mutableListOf<Task<Uri>>()
        for (uri in imageUris) {
            val filePath = storageReference.child("journal_images/${currentUserId}/my_image_${Timestamp.now().seconds}")
            val inputStream = getInputStreamFromUri(uri)
            if (inputStream != null) {
                uploadTasks.add(filePath.putStream(inputStream).continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    filePath.downloadUrl
                })
            } else {
                handleError(Exception("Unable to open input stream for URI"), "Invalid URI")
                return
            }
        }

        Tasks.whenAllSuccess<Uri>(uploadTasks).addOnSuccessListener { downloadUris ->
            val imageUrls = downloadUris.map { it.toString() }
            val journal = Journal(
                title = title,
                thoughts = thoughts,
                imageUrls = imageUrls,
                userId = currentUserId,
                timeAdded = Timestamp(Date()),
                username = currentUserName
            )

            collectionReference.add(journal)
                .addOnSuccessListener {
                    binding.postProgressBar.visibility = View.INVISIBLE
                    startActivity(Intent(this, JournalList::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    handleError(e, "Failed to add journal")
                }
        }.addOnFailureListener { e ->
            handleError(e, "Failed to upload images")
        }
    }

    private fun uploadImageAndSaveJournal(title: String, thoughts: String) {
        if (!ensureUserAuthenticated()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val filePath = storageReference.child("journal_images/${currentUserId}/my_image_${Timestamp.now().seconds}")

        filePath.putFile(imageUris[0])
            .addOnSuccessListener {
                filePath.downloadUrl.addOnSuccessListener { downloadUri ->
                    val journal = Journal(
                        title = title,
                        thoughts = thoughts,
                        imageUrls = listOf(downloadUri.toString()),
                        userId = currentUserId,
                        timeAdded = Timestamp(Date()),
                        username = currentUserName
                    )

                    collectionReference.add(journal)
                        .addOnSuccessListener {
                            binding.postProgressBar.visibility = View.INVISIBLE
                            startActivity(Intent(this, JournalList::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            handleError(e, "Failed to add journal")
                        }
                }.addOnFailureListener { e ->
                    handleError(e, "Failed to get download URL")
                }
            }
            .addOnFailureListener { e ->
                handleError(e, "Failed to upload image")
            }
    }
    private fun ensureUserAuthenticated(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    private fun addJournalWithoutImage(title: String, thoughts: String) {
        val journal = Journal(
            title = title,
            thoughts = thoughts,
            imageUrls = emptyList(),
            userId = currentUserId,
            timeAdded = Timestamp(Date()),
            username = currentUserName
        )

        collectionReference.add(journal)
            .addOnSuccessListener {
                binding.postProgressBar.visibility = View.INVISIBLE
                startActivity(Intent(this, JournalList::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                handleError(e, "Failed to add journal")
            }
    }
    private fun updateJournal(journal: Journal) {
        val title = binding.postTitleEt.text.toString().trim()
        val thoughts = binding.postDescriptionEt.text.toString().trim()

        if (title.isBlank() || thoughts.isBlank()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.postProgressBar.visibility = View.VISIBLE

        val updatedJournal = journal.copy(
            title = title,
            thoughts = thoughts,
            timeAdded = Timestamp(Date()) // Optionally update the timestamp
        )

        // Separate URIs into new (local) and existing (remote)
        val newImageUris = imageUris.filter { uri -> uri.scheme == "content" || uri.scheme == "file" }
        val existingImageUrls = imageUris.filter { uri -> uri.scheme == "https" }.map { it.toString() }

        if (newImageUris.isEmpty()) {
            // If no new images, just update text or use existing image URLs
            updateExistingJournalWithoutImage(journal, updatedJournal.copy(imageUrls = existingImageUrls))
        } else {
            // Upload new images and update the journal with both new and existing images
            uploadImagesAndUpdateJournal(journal, updatedJournal, existingImageUrls)
        }
    }


    private fun updateExistingJournalWithoutImage(journal: Journal, updatedJournal: Journal) {
        collectionReference.document(journal.id).set(updatedJournal)
            .addOnSuccessListener {
                binding.postProgressBar.visibility = View.INVISIBLE
                Toast.makeText(this, "Journal updated", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, JournalList::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                handleError(e, "Failed to update journal")
            }
    }


    private fun uploadImagesAndUpdateJournal(journal: Journal, updatedJournal: Journal, existingImageUrls: List<String>) {
        val uploadTasks = mutableListOf<Task<Uri>>()
        val newImageUris = imageUris.filter { uri -> uri.scheme == "content" || uri.scheme == "file" } // Only local URIs

        // Replace existing image URLs if new images are uploaded
        val allImageUrls = mutableListOf<String>()

        if (newImageUris.isNotEmpty()) {
            // Clear the existing images if new ones are added
            allImageUrls.clear()

            // Upload each new image
            newImageUris.forEach { uri ->
                val filePath = storageReference.child("journal_images/${currentUserId}/my_image_${Timestamp.now().seconds}")
                val inputStream = getInputStreamFromUri(uri)
                if (inputStream != null) {
                    val uploadTask = filePath.putStream(inputStream).continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let { throw it }
                        }
                        filePath.downloadUrl
                    }
                    uploadTasks.add(uploadTask)
                } else {
                    handleError(Exception("Unable to open input stream for URI: $uri"), "Invalid URI")
                    return
                }
            }

            // When all uploads are completed, combine URLs and update Firestore
            Tasks.whenAllSuccess<Uri>(uploadTasks).addOnSuccessListener { downloadUris ->
                val newImageUrls = downloadUris.map { it.toString() } // Get new URLs
                allImageUrls.addAll(newImageUrls) // Add new URLs to the list

                val journalWithImages = updatedJournal.copy(imageUrls = allImageUrls) // Replace with new URLs

                // Update the Firestore document with new image URLs
                collectionReference.document(journal.id).set(journalWithImages)
                    .addOnSuccessListener {
                        binding.postProgressBar.visibility = View.INVISIBLE
                        Toast.makeText(this, "Journal updated", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, JournalList::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        handleError(e, "Failed to update journal")
                    }
            }.addOnFailureListener { e ->
                handleError(e, "Failed to upload images")
            }
        } else {
            // If no new images and existing images should remain, use existing URLs
            allImageUrls.addAll(existingImageUrls) // Keep existing URLs if no new ones
            updateExistingJournalWithoutImage(journal, updatedJournal.copy(imageUrls = allImageUrls))
        }
    }

    private fun getInputStreamFromUri(uri: Uri): InputStream? {
        return try {
            contentResolver.openInputStream(uri) ?: run {
                handleError(
                    Exception("Skipping URI due to null input stream: $uri"),
                    "Skipping invalid URI"
                )
                null
            }
        } catch (e: Exception) {
            Log.e("AddJournalActivity", "Failed to get input stream from URI: $uri", e)
            handleError(e, "Failed to open input stream")
            null
        }
    }




    private fun handleError(exception: Exception, message: String) {
        binding.postProgressBar.visibility = View.INVISIBLE
        Log.e("FirebaseError", "$message: ${exception.message}", exception)
        Toast.makeText(this, "$message: ${exception.message}", Toast.LENGTH_SHORT).show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_journal)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.parseColor("#596B8F")

        // Ensure the status bar text color is appropriate for the background color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        storageReference = FirebaseStorage.getInstance().reference
        auth = Firebase.auth
        user = auth.currentUser ?: run {
            Toast.makeText(this, "No authenticated user", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentUserId = user.uid
        currentUserName = user.displayName ?: "Unknown User"

        val journalToEdit: Journal? = intent.getParcelableExtra("edit_journal")
        if (journalToEdit != null) {
            populateFieldsForEditing(journalToEdit)
        } else {
            setupForNewJournal()
        }

        binding.apply {
            postProgressBar.visibility = View.INVISIBLE
            postCameraButton.setOnClickListener { showImageSourceDialog() }
            postSaveJournalButton.setOnClickListener { handleSaveButtonClick(journalToEdit) }
            postCancelJournalButton.setOnClickListener { cancelAction() }
        }
    }



    private fun populateFieldsForEditing(journal: Journal) {
        binding.postTitleEt.setText(journal.title)
        binding.postDescriptionEt.setText(journal.thoughts)
        // Load images if available
        imageUris = journal.imageUrls.map { Uri.parse(it) }.toMutableList()
        updateImageView()
        binding.postSaveJournalButton.text = "Update"
    }

    private fun setupForNewJournal() {
        binding.postSaveJournalButton.text = "Save"
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        selectImageLauncher.launch(intent)
    }

    private fun handleSaveButtonClick(journalToEdit: Journal?) {
        if (journalToEdit != null) {
            updateJournal(journalToEdit)
        } else {
            saveJournal()
        }
    }

    private fun cancelAction() {
        startActivity(Intent(this, JournalList::class.java))
        finish()
    }

    private fun saveJournal() {
        val title = binding.postTitleEt.text.toString().trim()
        val thoughts = binding.postDescriptionEt.text.toString().trim()

        if (title.isBlank() || thoughts.isBlank()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.postProgressBar.visibility = View.VISIBLE

        if (imageUris.isEmpty()) {
            addJournalWithoutImage(title, thoughts)
        } else {
            uploadImagesAndSaveJournal(title, thoughts)
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf( "Gallery", "File Manager")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image Source")
        builder.setItems(options) { _, which ->
            when (which) {

                0 -> openGallery()
                1 -> openFileManager()
            }
        }
        builder.show()
    }



    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    private fun openFileManager() {
        val fileManagerIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(fileManagerIntent, FILE_MANAGER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {

                GALLERY_REQUEST_CODE, FILE_MANAGER_REQUEST_CODE -> {
                    val imageUri = data?.data
                    imageUri?.let {
                        imageUris.add(it)
                        updateImageView()
                    } ?: run {
                        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImagePicker()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    companion object {
        private const val PERMISSION_REQUEST_CODE = 100

            private const val GALLERY_REQUEST_CODE = 2
            private const val FILE_MANAGER_REQUEST_CODE = 3


    }


}