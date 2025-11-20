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
import com.hans.i221271_i220889.repositories.FollowRepository
import com.hans.i221271_i220889.network.SessionManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class FollowRequestsActivity : AppCompatActivity() {
    private lateinit var followRepository: FollowRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var requestsRecyclerView: RecyclerView
    private lateinit var requestsAdapter: FollowRequestsAdapter
    private val pendingRequests = mutableListOf<User>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple UI programmatically
        createSimpleFollowRequestsScreen()
        
        followRepository = FollowRepository(this)
        sessionManager = SessionManager(this)
        
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
            // TODO: Implement accept/reject follow request API
            Toast.makeText(this, "Follow requests feature coming soon", Toast.LENGTH_SHORT).show()
        }
        requestsRecyclerView.adapter = requestsAdapter
    }
    
    private fun loadPendingFollowRequests() {
        // TODO: Implement follow requests API endpoint
        // For now, show empty list
        android.util.Log.d("FollowRequests", "Follow requests feature to be implemented")
    }
}
