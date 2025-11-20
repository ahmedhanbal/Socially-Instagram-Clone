package com.hans.i221271_i220889.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.R
import com.hans.i221271_i220889.models.User
import com.hans.i221271_i220889.utils.Base64Image

class FollowRequestsAdapter(
    private val requests: List<User>,
    private val onActionClick: (User, String) -> Unit
) : RecyclerView.Adapter<FollowRequestsAdapter.RequestViewHolder>() {
    
    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: ImageView = itemView.findViewById(R.id.profileImageView)
        val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        val emailTextView: TextView = itemView.findViewById(R.id.emailTextView)
        val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        val rejectButton: Button = itemView.findViewById(R.id.rejectButton)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_request, parent, false)
        return RequestViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        
        holder.usernameTextView.text = request.username
        holder.emailTextView.text = request.email
        
        // Set profile image (using profileImageBase64 for Realtime Database)
        val profileImage = if (request.profileImageBase64.isNotEmpty()) {
            request.profileImageBase64
        } else {
            request.profileImageUrl // Fallback for backward compatibility
        }
        
        if (profileImage.isNotEmpty()) {
            try {
                val bitmap = Base64Image.base64ToBitmap(profileImage)
                holder.profileImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileImageView.setImageResource(R.drawable.ic_default_profile)
            }
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_default_profile)
        }
        
        holder.acceptButton.setOnClickListener {
            onActionClick(request, "accept")
        }
        
        holder.rejectButton.setOnClickListener {
            onActionClick(request, "reject")
        }
    }
    
    override fun getItemCount(): Int = requests.size
}
