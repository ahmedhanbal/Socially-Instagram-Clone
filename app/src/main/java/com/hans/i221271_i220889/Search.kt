package com.hans.i221271_i220889

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.adapters.UserAdapter
import com.hans.i221271_i220889.models.User
import com.hans.i221271_i220889.network.SessionManager
import com.hans.i221271_i220889.repositories.SearchRepository
import kotlinx.coroutines.launch

class Search : AppCompatActivity() {
    private lateinit var searchRepository: SearchRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var searchInput: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var filterAll: Button
    private lateinit var filterFollowers: Button
    private lateinit var filterFollowing: Button
    private val searchResults = mutableListOf<User>()
    private var currentFilter: String? = null // null = all, followers, following
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        
        searchRepository = SearchRepository(this)
        sessionManager = SessionManager(this)
        
        bindViews()
        setupSearchResultsRecyclerView()
        setupListeners()
        updateFilterButtons()
    }
    
    private fun bindViews() {
        searchInput = findViewById(R.id.search_input)
        searchButton = findViewById(R.id.search_button)
        filterAll = findViewById(R.id.filter_all)
        filterFollowers = findViewById(R.id.filter_followers)
        filterFollowing = findViewById(R.id.filter_following)
        findViewById<ImageButton>(R.id.back_button).setOnClickListener { finish() }
        searchResultsRecyclerView = findViewById(R.id.search_results_recycler)
    }
    
    private fun setupListeners() {
        searchButton.setOnClickListener { performSearch() }
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
        
        filterAll.setOnClickListener {
            currentFilter = null
            updateFilterButtons()
            performSearch()
        }
        
        filterFollowers.setOnClickListener {
            currentFilter = "followers"
            updateFilterButtons()
            performSearch()
        }
        
        filterFollowing.setOnClickListener {
            currentFilter = "following"
            updateFilterButtons()
            performSearch()
        }
    }
    
    private fun setupSearchResultsRecyclerView() {
        userAdapter = UserAdapter(searchResults) { user ->
            val currentUserId = sessionManager.getUserId().toString()
            if (currentUserId != "-1" && currentUserId != user.userId) {
                val intent = Intent(this, UserProfile::class.java)
                intent.putExtra("userId", user.userId)
                intent.putExtra("username", user.username)
                startActivity(intent)
            } else if (currentUserId != "-1") {
                startActivity(Intent(this, OwnProfile::class.java))
            }
        }
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsRecyclerView.adapter = userAdapter
    }
    
    private fun performSearch() {
        val query = searchInput.text.toString().trim()
        if (query.isEmpty() && currentFilter == null) {
            Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show()
            return
        }
        searchUsers(query)
    }
    
    private fun updateFilterButtons() {
        val selectedColor = ContextCompat.getColor(this, R.color.brown)
        val unselectedColor = ContextCompat.getColor(this, R.color.white)
        val selectedText = ContextCompat.getColor(this, android.R.color.white)
        val unselectedText = ContextCompat.getColor(this, R.color.brown)
        
        fun Button.update(selected: Boolean) {
            backgroundTintList = ColorStateList.valueOf(if (selected) selectedColor else unselectedColor)
            setTextColor(if (selected) selectedText else unselectedText)
        }
        
        filterAll.update(currentFilter == null)
        filterFollowers.update(currentFilter == "followers")
        filterFollowing.update(currentFilter == "following")
    }
    
    private fun searchUsers(query: String) {
        lifecycleScope.launch {
            val result = searchRepository.searchUsers(query, currentFilter)
            result.onSuccess { userDataList ->
                searchResults.clear()
                
                // Get user IDs to fetch status
                val userIds = userDataList.map { it.id }
                
                // Fetch statuses for all users
                val statusResult = if (userIds.isNotEmpty()) {
                    searchRepository.getUsersStatus(userIds)
                } else {
                    Result.success(emptyList())
                }
                
                val statusMap = statusResult.getOrNull()?.associateBy { it.userId } ?: emptyMap()
                
                userDataList.forEach { userData ->
                    val userStatus = statusMap[userData.id]
                    searchResults.add(
                        User(
                            userId = userData.id.toString(),
                            username = userData.username,
                            email = userData.email,
                            fullName = userData.fullName ?: "",
                            bio = userData.bio ?: "",
                            profilePicture = userData.profilePicture ?: "",
                            coverPhoto = userData.coverPhoto ?: "",
                            isPrivate = userData.isPrivate,
                            followersCount = userData.followersCount,
                            followingCount = userData.followingCount,
                            isOnline = userStatus?.isOnline ?: false
                        )
                    )
                }
                userAdapter.notifyDataSetChanged()
            }.onFailure { error ->
                Toast.makeText(this@Search, "Search failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Update status to online
        lifecycleScope.launch {
            searchRepository.updateOnlineStatus(true)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Update status to offline
        lifecycleScope.launch {
            searchRepository.updateOnlineStatus(false)
        }
    }
}
