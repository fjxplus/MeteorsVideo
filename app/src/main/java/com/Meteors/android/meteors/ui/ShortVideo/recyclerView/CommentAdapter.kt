package com.Meteors.android.meteors.ui.ShortVideo.recyclerView

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.Meteors.android.meteors.R
import com.Meteors.android.meteors.databinding.CommentItemBinding
import com.Meteors.android.meteors.logic.model.Comment
import com.Meteors.android.meteors.logic.network.Repository
import com.bumptech.glide.Glide

private const val TAG = "Meteors_CommentAdapter"

class CommentAdapter(private val context: Context, private val comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentHolder {
        val itemBinding =
            CommentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: CommentHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    inner class CommentHolder(private val itemBinding: CommentItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(comment: Comment) {
            //增加加载中的提示
            itemBinding.textUserId.text = comment.userId
            itemBinding.textComment.text = comment.txt
            Glide.with(context)
                .load(Repository.getUserImageUrl(comment.userId))
                .circleCrop()
                .placeholder(R.drawable.place_holder_user)
                .into(itemBinding.imageUser)
        }
    }
}