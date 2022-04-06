package com.Meteors.android.meteors.ui.LiveVideo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.Meteors.android.meteors.databinding.CommentLiveItemBinding
import com.Meteors.android.meteors.logic.model.Comment

class LiveCommentAdapter(private val comments: List<Comment>) : RecyclerView.Adapter<LiveCommentAdapter.LiveCommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LiveCommentViewHolder {
        val itemBinding = CommentLiveItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LiveCommentViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: LiveCommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.bind(comment)
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    inner class LiveCommentViewHolder(private val itemBinding: CommentLiveItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(comment: Comment){
                itemBinding.textUserId.text = comment.userName
                itemBinding.textComment.text = comment.txt
            }
    }
}