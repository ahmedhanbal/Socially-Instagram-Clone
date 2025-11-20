package com.hans.i221271_i220889.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.R
import com.hans.i221271_i220889.models.Post
import com.hans.i221271_i220889.utils.Base64Image
import com.hans.i221271_i220889.repositories.PostRepositoryApi
import com.hans.i221271_i220889.network.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PostAdapter(
    private val posts: MutableList<Post>,
    private val postRepository: PostRepositoryApi,
    private val sessionManager: SessionManager,
    private val lifecycleScope: CoroutineScope,
    private val onCommentClick: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val username: TextView = itemView.findViewById(R.id.username)
        val location: TextView = itemView.findViewById(R.id.location)
        val postImage: ImageView = itemView.findViewById(R.id.postImage)
        val likeButton: ImageButton = itemView.findViewById(R.id.likeButton)
        val commentButton: ImageButton = itemView.findViewById(R.id.commentButton)
        val shareButton: ImageButton = itemView.findViewById(R.id.shareButton)
        val saveButton: ImageButton = itemView.findViewById(R.id.saveButton)
        val likeCount: TextView = itemView.findViewById(R.id.likeCount)
        val caption: TextView = itemView.findViewById(R.id.caption)
        val commentCount: TextView = itemView.findViewById(R.id.commentCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        val currentUserId = sessionManager.getSession()?.userId?.toString()

        // Set profile image (Base64 encoded)
        if (post.userProfileImage.isNotEmpty()) {
            try {
                val bitmap = Base64Image.base64ToBitmap(post.userProfileImage)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.ic_default_profile)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_default_profile)
        }

        // Set username and location
        holder.username.text = post.username
        holder.location.text = "Tokyo, Japan" // You can add location to Post model

        // Set post image (Base64)
        if (post.imageUrl.isNotEmpty()) {
            try {
                val bitmap = Base64Image.base64ToBitmap(post.imageUrl)
                holder.postImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.postImage.setImageResource(R.drawable.placeholder_image)
            }
        }

        // Set like button state
        val isLiked = post.isLikedByCurrentUser
        holder.likeButton.setImageResource(
            if (isLiked) R.drawable.like_filled else R.drawable.like
        )

        // Set like count
        holder.likeCount.text = "${post.likesCount} likes"

        // Set caption
        holder.caption.text = "${post.username} ${post.caption}"

        // Set comment count
        holder.commentCount.text = "View all ${post.commentsCount} comments"

        // Set up click listeners
        // Profile image click - navigate to user profile
        holder.profileImage.setOnClickListener {
            val context = holder.itemView.context
            val currentUserId = sessionManager.getSession()?.userId?.toString()
            // If clicking own profile, go to OwnProfile, else UserProfile
            if (post.userId == currentUserId) {
                val intent = Intent(context, com.hans.i221271_i220889.OwnProfile::class.java)
                context.startActivity(intent)
            } else {
                val intent = Intent(context, com.hans.i221271_i220889.UserProfile::class.java)
                intent.putExtra("userId", post.userId)
                context.startActivity(intent)
            }
        }
        
        // Username click - navigate to user profile
        holder.username.setOnClickListener {
            val context = holder.itemView.context
            val currentUserId = sessionManager.getSession()?.userId?.toString()
            // If clicking own profile, go to OwnProfile, else UserProfile
            if (post.userId == currentUserId) {
                val intent = Intent(context, com.hans.i221271_i220889.OwnProfile::class.java)
                context.startActivity(intent)
            } else {
                val intent = Intent(context, com.hans.i221271_i220889.UserProfile::class.java)
                intent.putExtra("userId", post.userId)
                context.startActivity(intent)
            }
        }
        
        // Comment count click - navigate to comments
        holder.commentCount.setOnClickListener {
            onCommentClick(post)
        }

        holder.likeButton.setOnClickListener {
            val postId = post.postId.toIntOrNull() ?: return@setOnClickListener
            
            lifecycleScope.launch {
                val result = if (isLiked) {
                    postRepository.unlikePost(postId)
                } else {
                    postRepository.likePost(postId)
                }
                
                result.onSuccess {
                    // Update UI optimistically
                    post.isLikedByCurrentUser = !isLiked
                    post.likesCount = if (isLiked) post.likesCount - 1 else post.likesCount + 1
                    
                    holder.likeButton.setImageResource(
                        if (post.isLikedByCurrentUser) R.drawable.like_filled else R.drawable.like
                    )
                    holder.likeCount.text = "${post.likesCount} likes"
                }.onFailure { error ->
                    Toast.makeText(holder.itemView.context, "Failed to update like: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        holder.commentButton.setOnClickListener {
            onCommentClick(post)
        }

        holder.shareButton.setOnClickListener {
            // Implement share functionality
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "${post.username}: ${post.caption}")
                type = "text/plain"
            }
            holder.itemView.context.startActivity(Intent.createChooser(shareIntent, "Share post"))
        }
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
}
