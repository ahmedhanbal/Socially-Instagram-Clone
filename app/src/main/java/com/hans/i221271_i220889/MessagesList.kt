package com.hans.i221271_i220889

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.adapters.UserAdapter
import com.hans.i221271_i220889.models.User

class MessagesList : AppCompatActivity() {
    
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val users = mutableListOf<User>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_message_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val usernameTextView = findViewById<TextView>(R.id.header_title)
        
        // Fetch username from Firebase
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            com.google.firebase.database.FirebaseDatabase.getInstance().reference
                .child("users")
                .child(currentUserId)
                .child("username")
                .get()
                .addOnSuccessListener { snapshot ->
                    val username = snapshot.getValue(String::class.java) ?: "User"
                    usernameTextView.text = username
                }
                .addOnFailureListener {
                    // Fallback to SharedPreferences or default
                    val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                    val username = sharedPref.getString("USERNAME_KEY", "Guest")
                    usernameTextView.text = username
                }
        } else {
            // Fallback to SharedPreferences or default
            val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val username = sharedPref.getString("USERNAME_KEY", "Guest")
            usernameTextView.text = username
        }

        // Setup users list
        setupUsersRecyclerView()
        loadUsers()
    }
    
    private fun setupUsersRecyclerView() {
        usersRecyclerView = findViewById(R.id.usersRecyclerView)
        userAdapter = UserAdapter(users) { user ->
            // Handle user click - start chat
            val intentChat = Intent(this, Chat::class.java)
            intentChat.putExtra("PersonName", user.username)
            intentChat.putExtra("userId", user.userId)
            startActivity(intentChat)
        }
        usersRecyclerView.layoutManager = LinearLayoutManager(this)
        usersRecyclerView.adapter = userAdapter
    }
    
    private fun loadUsers() {
        // Load real users from Firebase
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            return
        }
        
        com.google.firebase.database.FirebaseDatabase.getInstance().reference
            .child("users")
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    users.clear()
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        // Only show other users, not the current user
                        if (user != null && user.userId != currentUserId) {
                            users.add(user)
                        }
                    }
                    userAdapter.notifyDataSetChanged()
                }
                
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    android.widget.Toast.makeText(this@MessagesList, "Failed to load users", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
    }
}
