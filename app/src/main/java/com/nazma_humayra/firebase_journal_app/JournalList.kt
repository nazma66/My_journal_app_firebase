package com.nazma_humayra.firebase_journal_app

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nazma_humayra.firebase_journal_app.databinding.ActivityJournalListBinding

class JournalList : AppCompatActivity() {
    lateinit var binding: ActivityJournalListBinding
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var user: FirebaseUser
    private val db = FirebaseFirestore.getInstance()
    private val collectionReference: CollectionReference = db.collection("Journal")
    private lateinit var journalList: MutableList<Journal>
    private lateinit var adapter: JournalRecyclerAdapter
    private lateinit var fabAddJournal: FloatingActionButton
    private lateinit var signOutButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_journal_list)

        // Adjust the system UI to fit the layout with transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.parseColor("#596B8F")

        // Ensure the status bar text color is appropriate for the background color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }



        firebaseAuth = Firebase.auth
        user = firebaseAuth.currentUser!!

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        journalList = arrayListOf()
        adapter = JournalRecyclerAdapter(this, journalList)
        binding.recyclerView.adapter = adapter

        fabAddJournal = findViewById(R.id.fab_add_journal)
        fabAddJournal.setOnClickListener {
            val intent = Intent(this, AddJournalActivity::class.java)
            startActivity(intent)
        }

        signOutButton = findViewById(R.id.action_signout)
        signOutButton.setOnClickListener {
            firebaseAuth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()  // Finish the current activity to prevent going back
        }

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterJournals(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterJournals(newText)
                return true
            }
        })
        loadJournals()
    }

    private fun filterJournals(query: String?) {
        val filteredList = journalList.filter {
            it.title.contains(query ?: "", ignoreCase = true)
        }
        adapter.updateJournalList(filteredList)
        binding.listNoPosts.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadJournals() {
        collectionReference.whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    journalList.clear()
                    for (document in querySnapshot.documents) {
                        val journal = document.toObject<Journal>()
                        journal?.let {
                            it.id = document.id
                            journalList.add(it)
                        }
                    }
                    adapter.notifyDataSetChanged()
                    binding.listNoPosts.visibility = View.GONE
                } else {
                    binding.listNoPosts.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Log.w("JournalList", "Error fetching documents", e)
                Toast.makeText(this, "Oops! Something went wrong!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUsername(userId: String, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("Users").document(userId)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username")
                    callback(username)
                } else {
                    callback("unknown user")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FetchUsernameError", "Error fetching username", e)
                callback("unknown user")
            }
    }
}
