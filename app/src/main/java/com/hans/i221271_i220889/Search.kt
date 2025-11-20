package com.hans.i221271_i220889

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.hans.i221271_i220889.adapters.UserAdapter
import com.hans.i221271_i220889.models.User
import com.hans.i221271_i220889.repositories.SearchRepository
import com.hans.i221271_i220889.network.SessionManager
import kotlinx.coroutines.launch

class Search : AppCompatActivity() {
    private lateinit var searchRepository: SearchRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val searchResults = mutableListOf<User>()
    private var currentFilter = "all" // "all", "followers", "following"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize repositories
        searchRepository = SearchRepository(this)
        sessionManager = SessionManager(this)
        
        // Create simple UI programmatically
        createSimpleSearchScreen()
        
        setupSearchResultsRecyclerView()
    }
    
    private fun createSimpleSearchScreen() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(20, 20, 20, 20)
        }
        
        val titleText = android.widget.TextView(this).apply {
            text = "Search Users"
            textSize = 24f
            setTextColor(android.graphics.Color.parseColor("#784A34"))
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 30
            }
        }
        
        val backButton = android.widget.Button(this).apply {
            text = "← Back"
            setBackgroundColor(android.graphics.Color.parseColor("#784A34"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
            setOnClickListener {
                finish()
            }
        }
        
        val searchEditText = android.widget.EditText(this).apply {
            hint = "Search by username..."
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }
        
        val searchButton = android.widget.Button(this).apply {
            text = "Search"
            setBackgroundColor(android.graphics.Color.parseColor("#784A34"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
            setOnClickListener {
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchUsers(query)
                } else {
                    Toast.makeText(this@Search, "Please enter a search query", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        val filterLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }
        
        val allFilterButton = android.widget.Button(this).apply {
            text = "All"
            setBackgroundColor(android.graphics.Color.parseColor("#784A34"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 10
            }
            setOnClickListener {
                currentFilter = "all"
                updateFilterButtons()
                searchUsers(searchEditText.text.toString().trim())
            }
        }
        
        val followersFilterButton = android.widget.Button(this).apply {
            text = "Followers"
            setBackgroundColor(android.graphics.Color.parseColor("#784A34"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 10
                marginEnd = 10
            }
            setOnClickListener {
                currentFilter = "followers"
                updateFilterButtons()
                searchUsers(searchEditText.text.toString().trim())
            }
        }
        
        val followingFilterButton = android.widget.Button(this).apply {
            text = "Following"
            setBackgroundColor(android.graphics.Color.parseColor("#784A34"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 10
            }
            setOnClickListener {
                currentFilter = "following"
                updateFilterButtons()
                searchUsers(searchEditText.text.toString().trim())
            }
        }
        
        filterLayout.addView(allFilterButton)
        filterLayout.addView(followersFilterButton)
        filterLayout.addView(followingFilterButton)
        
        searchResultsRecyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@Search)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        layout.addView(titleText)
        layout.addView(backButton)
        layout.addView(searchEditText)
        layout.addView(searchButton)
        layout.addView(filterLayout)
        layout.addView(searchResultsRecyclerView)
        setContentView(layout)
    }
    
    private fun setupSearchResultsRecyclerView() {
        userAdapter = UserAdapter(searchResults) { user ->
            // Handle user click - navigate to user profile (NOT send follow request)
            val currentUser = authManager.getCurrentUser()
            if (currentUser != null && currentUser.userId != user.userId) {
                // Navigate to UserProfile activity - DO NOT SEND FOLLOW REQUEST
                val intent = Intent(this, UserProfile::class.java)
                intent.putExtra("userId", user.userId)
                intent.putExtra("username", user.username)
                startActivity(intent)
            } else if (currentUser != null && currentUser.userId == user.userId) {
                // If clicking on own profile, go to OwnProfile
                val intent = Intent(this, OwnProfile::class.java)
                startActivity(intent)
            }
        }
        searchResultsRecyclerView.adapter = userAdapter
    }
    
    private fun updateFilterButtons() {
        // This would update button colors based on current filter

    }
    
    private fun searchUsers(query: String) {
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val result = searchRepository.searchUsers(query)
            result.onSuccess { userDataList ->
                searchResults.clear()
                userDataList.forEach { userData ->
                    searchResults.add(User(
                        userId = userData.id.toString(),
                        username = userData.username,
                        email = userData.email,
                        profilePictureBase64 = userData.profilePicture ?: "",
                        bio = userData.bio ?: "",
                        followersCount = userData.followersCount,
                        followingCount = userData.followingCount
                    ))
                }
                userAdapter.notifyDataSetChanged()
            }.onFailure { error ->
                Toast.makeText(this@Search, "Search failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun searchAllUsers(query: String) {
        try {
            database.reference.child("users")
                .orderByChild("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val users = mutableListOf<User>()
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null) {
                                users.add(user)
                            }
                        }
                        
                        runOnUiThread {
                            searchResults.clear()
                            searchResults.addAll(users)
                            userAdapter.notifyDataSetChanged()
                            Toast.makeText(this@Search, "Found ${users.size} users", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        runOnUiThread {
                            Toast.makeText(this@Search, "Search failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
        } catch (e: Exception) {
            Toast.makeText(this, "Demo mode - Search functionality", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun searchInFollowers(query: String, userId: String) {
        try {
            followManager.getFollowers(userId) { followers ->
                val filteredFollowers = followers.filter { 
                    it.username.contains(query, ignoreCase = true) 
                }
                runOnUiThread {
                    searchResults.clear()
                    searchResults.addAll(filteredFollowers)
                    userAdapter.notifyDataSetChanged()
                    Toast.makeText(this@Search, "Found ${filteredFollowers.size} followers", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Demo mode - Search in followers", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun searchInFollowing(query: String, userId: String) {
        try {
            followManager.getFollowing(userId) { following ->
                val filteredFollowing = following.filter { 
                    it.username.contains(query, ignoreCase = true) 
                }
                runOnUiThread {
                    searchResults.clear()
                    searchResults.addAll(filteredFollowing)
                    userAdapter.notifyDataSetChanged()
                    Toast.makeText(this@Search, "Found ${filteredFollowing.size} following", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Demo mode - Search in following", Toast.LENGTH_SHORT).show()
        }
    }
}
