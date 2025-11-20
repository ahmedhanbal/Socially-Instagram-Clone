package com.hans.i221271_i220889.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.R
import com.hans.i221271_i220889.models.User
import com.hans.i221271_i220889.network.ApiConfig
import com.hans.i221271_i220889.utils.Base64Image
import com.squareup.picasso.Picasso

class UserAdapter(
    private val users: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        val userStatusText: TextView = itemView.findViewById(R.id.userStatusText)
        val profileImageView: ImageView = itemView.findViewById(R.id.profileImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        
        holder.usernameText.text = user.username
        // Show full name if available, otherwise show online/offline status
        val statusText = if (user.fullName.isNotEmpty()) {
            if (user.isOnline) {
                "${user.fullName} • Online"
            } else {
                "${user.fullName} • Offline"
            }
        } else {
            if (user.isOnline) "Online" else "Offline"
        }
        holder.userStatusText.text = statusText
        
        // Set profile image (URL or Base64)
        val profileImage = user.profilePicture.ifEmpty { 
            user.profileImageBase64.ifEmpty { user.profileImageUrl }
        }
        
        if (profileImage.isNotEmpty()) {
            if (profileImage.startsWith("http") || profileImage.startsWith("uploads/")) {
                // Load from URL using Picasso
                val imageUrl = if (profileImage.startsWith("http")) {
                    profileImage
                } else {
                    ApiConfig.BASE_URL + profileImage
                }
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(holder.profileImageView)
            } else {
                // Fallback to Base64
                try {
                    val bitmap = Base64Image.base64ToBitmap(profileImage)
                    holder.profileImageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    holder.profileImageView.setImageResource(R.drawable.ic_default_profile)
                }
            }
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_default_profile)
        }
        
        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }

    override fun getItemCount(): Int = users.size
}
