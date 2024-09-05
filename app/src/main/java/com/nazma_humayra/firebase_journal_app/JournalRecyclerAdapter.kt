package com.nazma_humayra.firebase_journal_app

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.nazma_humayra.firebase_journal_app.databinding.JournalRowBinding
import java.text.SimpleDateFormat
import java.util.Locale

class JournalRecyclerAdapter(
    private val context: Context,
    private var journalList: MutableList<Journal> // Make it mutable
) : RecyclerView.Adapter<JournalRecyclerAdapter.MyViewHolder>() {

    // View Holder
    inner class MyViewHolder(private val binding: JournalRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.journalRowBookmarkButton.setOnClickListener {
                binding.journal?.let { journal ->
                    toggleBookmark(journal)
                }
            }

            binding.journalRowShareButton.setOnClickListener {
                binding.journal?.let { journal ->
                    shareJournal(journal)
                }
            }

            binding.journalRowMoreButton.setOnClickListener {
                binding.journal?.let { journal ->
                    showPopupMenu(it, journal)
                }
            }
            binding.root.setOnClickListener {
                binding.journal?.let { journal ->
                    val intent = Intent(context, JournalDetailActivity::class.java).apply {
                        putExtra("journal", journal)
                    }
                    context.startActivity(intent)
                }
            }
        }
        

        fun bind(journal: Journal) {
            fetchUsername(journal.userId) { username ->
                journal.username = username
                binding.journal = journal
                Log.d("JournalAdapter", "Binding journal with username: ${journal.username}")
                binding.executePendingBindings()
                updateBookmarkButton(journal) // Update button state
            }
        }

        private fun updateBookmarkButton(journal: Journal) {
            if (journal.isBookmarked) {
                binding.journalRowBookmarkButton.setImageResource(R.drawable.ic_bookmark_filled)
            } else {
                binding.journalRowBookmarkButton.setImageResource(R.drawable.ic_bookmark_border)
            }
        }

        private fun toggleBookmark(journal: Journal) {
            journal.isBookmarked = !journal.isBookmarked
            updateBookmarkButton(journal)

            val message = if (journal.isBookmarked) "Bookmark added" else "Bookmark removed"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

            updateJournalInFirestore(journal)
        }

        private fun updateJournalInFirestore(journal: Journal) {
            val db = FirebaseFirestore.getInstance()
            val collectionReference = db.collection("Journal")

            val journalId = journal.id ?: return // Return early if id is null

            collectionReference.document(journalId).update("isBookmarked", journal.isBookmarked)
                .addOnSuccessListener {
                    Toast.makeText(context, "Bookmark status updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreError", "Failed to update bookmark for journal ID: $journalId", e)
                    Toast.makeText(context, "Failed to update bookmark: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        private fun fetchUsername(userId: String?, callback: (String?) -> Unit) {
            if (userId == null) {
                callback("Unknown User")
                return
            }

            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("Users").document(userId)

            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val username = document.getString("username")
                        Log.d("JournalAdapter", "Fetched username: $username")
                        callback(username)
                    } else {
                        Log.d("JournalAdapter", "User document does not exist")
                        callback("Unknown User")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("JournalAdapter", "Error fetching username", e)
                    callback("Unknown User")
                }
        }

        private fun shareJournal(journal: Journal) {
            val shareText = "${journal.title}\n${journal.thoughts}\n${journal.imageUrls.firstOrNull()}"
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
        }

        private fun showPopupMenu(view: View, journal: Journal) {
            val popup = PopupMenu(context, view)
            popup.inflate(R.menu.journal_item_menu)

            popup.setOnMenuItemClickListener { menuItem: MenuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        val intent = Intent(context, AddJournalActivity::class.java).apply {
                            putExtra("edit_journal", journal)
                        }
                        context.startActivity(intent)
                        true
                    }
                    R.id.action_delete -> {
                        deleteJournal(journal)
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }

        private fun deleteJournal(journal: Journal) {
            val db = FirebaseFirestore.getInstance()
            val collectionReference = db.collection("Journal")

            val journalId = journal.id ?: return

            collectionReference.document(journalId).delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Journal deleted", Toast.LENGTH_SHORT).show()
                    removeJournal(journal) // Update UI
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreError", "Error deleting journal with ID: $journalId", e)
                    Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        fun removeJournal(journal: Journal) {
            journalList.removeAll { it.id == journal.id }
            notifyDataSetChanged()
        }

        private fun formatTimestamp(timestamp: Timestamp): String {
            val date = timestamp.toDate()
            val formatter = SimpleDateFormat("h:mm a, MM/dd/yyyy", Locale.getDefault())
            return formatter.format(date)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<JournalRowBinding>(
            inflater,
            R.layout.journal_row,
            parent,
            false
        )
        return MyViewHolder(binding)
    }

    fun updateJournalList(newJournalList: List<Journal>) {
        val diffCallback = JournalDiffCallback(journalList, newJournalList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        journalList.clear()
        journalList.addAll(newJournalList)

        diffResult.dispatchUpdatesTo(this) // Apply changes calculated by DiffUtil
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val journal = journalList[position]
        holder.bind(journal)
    }

    override fun getItemCount(): Int = journalList.size
}
