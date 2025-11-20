package com.hans.i221271_i220889.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.R
import com.hans.i221271_i220889.utils.Base64Image
import com.hans.i221271_i220889.utils.ChatMessage
import com.hans.i221271_i220889.network.ApiConfig
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: MutableList<ChatMessage>,
    private val currentUserId: String,
    private val onMessageLongClick: (ChatMessage) -> Unit,
    private val onFileClick: (ChatMessage) -> Unit = {}
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val messageImage: ImageView = itemView.findViewById(R.id.messageImage)
        val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        val messageContainer: LinearLayout = itemView.findViewById(R.id.messageContainer)
        val messageBubble: LinearLayout = itemView.findViewById(R.id.messageBubble)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        val messageActions: LinearLayout = itemView.findViewById(R.id.messageActions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == 0) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message_received
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val isCurrentUser = message.senderId == currentUserId
        val canEdit = System.currentTimeMillis() <= message.editableUntil && isCurrentUser

        // Set message content based on type
        when (message.type) {
            "text" -> {
                holder.messageText.text = message.content
                holder.messageText.visibility = View.VISIBLE
                holder.messageImage.visibility = View.GONE
            }
            "image" -> {
                holder.messageImage.visibility = View.VISIBLE
                holder.messageText.visibility = if (message.content.isNotEmpty()) View.VISIBLE else View.GONE
                if (message.content.isNotEmpty()) {
                    holder.messageText.text = message.content
                }

                val mediaUrl = message.mediaUrl ?: message.content
                if (mediaUrl.startsWith("http") || mediaUrl.startsWith("uploads/")) {
                    // Load from URL using Picasso
                    val imageUrl = if (mediaUrl.startsWith("http")) {
                        mediaUrl
                    } else {
                        ApiConfig.BASE_URL + mediaUrl
                    }
                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(holder.messageImage)
                    
                    // Make image clickable to open in full screen
                    holder.messageImage.setOnClickListener {
                        onFileClick(message)
                    }
                } else {
                    // Fallback to Base64
                    try {
                        val bitmap = Base64Image.base64ToBitmap(mediaUrl)
                        holder.messageImage.setImageBitmap(bitmap)
                        holder.messageImage.setOnClickListener {
                            onFileClick(message)
                        }
                    } catch (e: Exception) {
                        holder.messageImage.setImageResource(R.drawable.placeholder_image)
                    }
                }
            }
            "video" -> {
                holder.messageImage.visibility = View.VISIBLE
                holder.messageText.visibility = if (message.content.isNotEmpty()) View.VISIBLE else View.GONE
                if (message.content.isNotEmpty()) {
                    holder.messageText.text = message.content
                }
                
                // Show video icon/thumbnail (use placeholder for now, can be replaced with actual video thumbnail)
                holder.messageImage.setImageResource(R.drawable.video_on)
                holder.messageImage.setOnClickListener {
                    onFileClick(message)
                }
            }
            "file" -> {
                holder.messageImage.visibility = View.VISIBLE
                holder.messageText.visibility = View.VISIBLE
                
                // Show file icon and filename
                val fileName = message.mediaUrl?.substringAfterLast("/") ?: "File"
                holder.messageText.text = "📎 $fileName"
                holder.messageImage.setImageResource(R.drawable.ic_more)
                holder.messageImage.setOnClickListener {
                    onFileClick(message)
                }
            }
            "post" -> {
                holder.messageText.text = "Shared a post"
                holder.messageText.visibility = View.VISIBLE
                holder.messageImage.visibility = View.GONE
            }
        }

        // Set timestamp
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.messageTime.text = timeFormat.format(Date(message.timestamp))

        // Set up edit/delete buttons for current user's messages
        if (isCurrentUser) {
            holder.editButton.visibility = if (canEdit) View.VISIBLE else View.GONE
            holder.deleteButton.visibility = if (canEdit) View.VISIBLE else View.GONE
            holder.messageActions.visibility = if (canEdit) View.VISIBLE else View.GONE
        } else {
            holder.messageActions.visibility = View.GONE
        }

        // Set up click listeners
        holder.messageContainer.setOnLongClickListener {
            if (isCurrentUser) {
                onMessageLongClick(message)
                true
            } else {
                false
            }
        }

        holder.editButton.setOnClickListener {
            // Handle edit message
            onMessageLongClick(message)
        }

        holder.deleteButton.setOnClickListener {
            // Handle delete message
            onMessageLongClick(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderId == currentUserId) 0 else 1
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}
