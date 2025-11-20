package com.hans.i221271_i220889.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.R
import com.hans.i221271_i220889.models.User

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
        holder.userStatusText.text = if (user.isOnline) "Online" else "Offline"
        
        // Set profile image (using profileImageBase64 for Realtime Database)
        val profileImage = if (user.profileImageBase64.isNotEmpty()) {
            user.profileImageBase64
        } else {
            user.profileImageUrl // Fallback for backward compatibility
        }
        
        if (profileImage.isNotEmpty()) {
            try {
                val bitmap = com.hans.i221271_i220889.utils.Base64Image.base64ToBitmap(profileImage)
                holder.profileImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileImageView.setImageResource(R.drawable.ic_default_profile)
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
