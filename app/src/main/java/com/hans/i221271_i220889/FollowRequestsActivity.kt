package com.hans.i221271_i220889

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.adapters.FollowRequestsAdapter
import com.hans.i221271_i220889.models.User
import com.hans.i221271_i220889.utils.FirebaseAuthManager
import com.hans.i221271_i220889.utils.FollowManager

class FollowRequestsActivity : AppCompatActivity() {
    private lateinit var followManager: FollowManager
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var requestsRecyclerView: RecyclerView
    private lateinit var requestsAdapter: FollowRequestsAdapter
    private val pendingRequests = mutableListOf<User>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple UI programmatically
        createSimpleFollowRequestsScreen()
        
        try {
            followManager = FollowManager()
            authManager = FirebaseAuthManager()
        } catch (e: Exception) {
            // If Firebase fails to initialize, continue without it
        }
        
        setupFollowRequestsRecyclerView()
        loadPendingFollowRequests()
    }
    
    private fun createSimpleFollowRequestsScreen() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(20, 20, 20, 20)
        }
        
        val titleText = android.widget.TextView(this).apply {
            text = "Follow Requests"
            textSize = 24f
            setTextColor(android.graphics.Color.parseColor("#8e3f42"))
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
            setBackgroundColor(android.graphics.Color.parseColor("#8e3f42"))
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
        
        requestsRecyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@FollowRequestsActivity)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        layout.addView(titleText)
        layout.addView(backButton)
        layout.addView(requestsRecyclerView)
        setContentView(layout)
    }
    
    private fun setupFollowRequestsRecyclerView() {
        requestsAdapter = FollowRequestsAdapter(pendingRequests) { user, action ->
            val currentUser = authManager.getCurrentUser()
            if (currentUser != null) {
                when (action) {
                    "accept" -> {
                        followManager.acceptFollowRequest(user.userId, currentUser.userId, this) { success, message ->
                            if (success) {
                                pendingRequests.remove(user)
                                requestsAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                    "reject" -> {
                        followManager.rejectFollowRequest(user.userId, currentUser.userId, this) { success, message ->
                            if (success) {
                                pendingRequests.remove(user)
                                requestsAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
        requestsRecyclerView.adapter = requestsAdapter
    }
    
    private fun loadPendingFollowRequests() {
        try {
            val currentUser = authManager.getCurrentUser()
            if (currentUser != null) {
                followManager.getPendingFollowRequests(currentUser.userId) { requests ->
                    runOnUiThread {
                        pendingRequests.clear()
                        pendingRequests.addAll(requests)
                        requestsAdapter.notifyDataSetChanged()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Demo mode - No follow requests", Toast.LENGTH_SHORT).show()
        }
    }
}
