package com.hans.i221271_i220889.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.R
import com.hans.i221271_i220889.models.Story
import com.hans.i221271_i220889.utils.Base64Image

class StoryAdapter(
    private val stories: MutableList<Story>,
    private val onStoryClick: (Story) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {
    
    fun updateStories(newStories: List<Story>) {
        android.util.Log.d("StoryAdapter", "updateStories called with ${newStories.size} stories")
        stories.clear()
        stories.addAll(newStories)
        android.util.Log.d("StoryAdapter", "Stories list updated, now has ${stories.size} items, calling notifyDataSetChanged()")
        notifyDataSetChanged()
        android.util.Log.d("StoryAdapter", "notifyDataSetChanged() called, itemCount: ${itemCount}")
    }

    class StoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val storyImage: ImageView = view.findViewById(R.id.storyImageView)
        val username: TextView = view.findViewById(R.id.storyUsername)
        val profileImage: ImageView = view.findViewById(R.id.storyProfileImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.story_item, parent, false)
        android.util.Log.d("StoryAdapter", "Created ViewHolder")
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]
        
        android.util.Log.d("StoryAdapter", "Binding story at position $position: storyId=${story.storyId}, userId=${story.userId}, username=${story.username}, hasImage=${story.imageUrl.isNotEmpty()}")
        
        // Set username
        holder.username.text = story.username
        
        // Set story image from Base64
        if (story.imageUrl.isNotEmpty()) {
            try {
                val bitmap = Base64Image.base64ToBitmap(story.imageUrl)
                if (bitmap != null) {
                    holder.storyImage.setImageBitmap(bitmap)
                    android.util.Log.d("StoryAdapter", "Story image set successfully for position $position")
                } else {
                    android.util.Log.w("StoryAdapter", "Failed to decode Base64 image for position $position")
                    holder.storyImage.setImageResource(R.drawable.ic_default_profile)
                }
            } catch (e: Exception) {
                android.util.Log.e("StoryAdapter", "Error setting story image: ${e.message}", e)
                holder.storyImage.setImageResource(R.drawable.ic_default_profile)
            }
        } else {
            android.util.Log.w("StoryAdapter", "Story imageUrl is empty for position $position")
            holder.storyImage.setImageResource(R.drawable.ic_default_profile)
        }
        
        // Set profile image from Base64
        if (story.userProfileImage.isNotEmpty()) {
            try {
                val bitmap = Base64Image.base64ToBitmap(story.userProfileImage)
                if (bitmap != null) {
                    holder.profileImage.setImageBitmap(bitmap)
                } else {
                    holder.profileImage.setImageResource(R.drawable.ic_default_profile)
                }
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.ic_default_profile)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_default_profile)
        }
        
        // Click listener - set on the entire item view
        holder.itemView.setOnClickListener {
            android.util.Log.d("StoryAdapter", "Story item clicked: ${story.storyId}, userId: ${story.userId}")
            onStoryClick(story)
        }
        
        // Also make sure child views don't intercept clicks
        holder.storyImage.isClickable = false
        holder.storyImage.isFocusable = false
        holder.profileImage.isClickable = false
        holder.profileImage.isFocusable = false
        holder.username.isClickable = false
        holder.username.isFocusable = false
    }

    override fun getItemCount(): Int = stories.size
}

