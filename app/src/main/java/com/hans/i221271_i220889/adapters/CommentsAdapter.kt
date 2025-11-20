package com.hans.i221271_i220889.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hans.i221271_i220889.R
import com.hans.i221271_i220889.models.Comment
import java.text.SimpleDateFormat
import java.util.*

class CommentsAdapter(
    private val comments: List<Comment>
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        val commentText: TextView = itemView.findViewById(R.id.commentText)
        val timeText: TextView = itemView.findViewById(R.id.timeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        
        holder.usernameText.text = comment.username
        holder.commentText.text = comment.text
        
        val timeFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.timeText.text = timeFormat.format(Date(comment.timestamp))
    }

    override fun getItemCount(): Int = comments.size
}
