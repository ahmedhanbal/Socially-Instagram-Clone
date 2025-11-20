package com.hans.i221271_i220889.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.R
import com.hans.i221271_i220889.models.Notification
import com.hans.i221271_i220889.utils.Base64Image
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val notifications: MutableList<Notification>,
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.notificationProfileImage)
        val titleText: TextView = itemView.findViewById(R.id.notificationTitle)
        val bodyText: TextView = itemView.findViewById(R.id.notificationBody)
        val timeText: TextView = itemView.findViewById(R.id.notificationTime)
        val container: LinearLayout = itemView.findViewById(R.id.notificationContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        
        // Set profile image
        if (notification.fromUserProfileImage.isNotEmpty()) {
            try {
                val bitmap = Base64Image.base64ToBitmap(notification.fromUserProfileImage)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.ic_default_profile)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_default_profile)
        }
        
        // Set title and body
        holder.titleText.text = notification.title
        holder.bodyText.text = notification.body
        
        // Set time
        val timeFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.timeText.text = timeFormat.format(Date(notification.timestamp))
        
        // Set background color if unread
        if (!notification.isRead) {
            holder.container.setBackgroundColor(
                holder.itemView.context.getColor(R.color.light_grey)
            )
        } else {
            holder.container.setBackgroundColor(
                holder.itemView.context.getColor(R.color.white)
            )
        }
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onNotificationClick(notification)
        }
    }

    override fun getItemCount(): Int = notifications.size
    
    fun updateNotifications(newNotifications: List<Notification>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }
}

