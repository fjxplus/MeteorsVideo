package com.Meteors.android.meteors.ui.ShortVideo.recyclerView

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.Meteors.android.meteors.R
import com.Meteors.android.meteors.databinding.CommentItemBinding
import com.Meteors.android.meteors.logic.model.Comment
import com.Meteors.android.meteors.logic.network.Repository
import com.bumptech.glide.Glide

private const val TAG = "Meteors_CommentAdapter"

class CommentAdapter(private val context: Context, private val comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentHolder>() {

    private val isPraisedSet = HashSet<Int>()       //HashSet保存点过赞的Item

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentHolder {
        val itemBinding =
            CommentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: CommentHolder, position: Int) {
        holder.bind(position, comments[position])
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    inner class CommentHolder(private val itemBinding: CommentItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        private var curPosition = 0

        fun bind(position: Int, comment: Comment) {
            //增加加载中的提示
            itemBinding.textUserId.text = comment.userName
            itemBinding.textComment.text = comment.txt
            curPosition = position
            Glide.with(context)
                .load(Repository.getUserImageUrl(comment.userId))
                .circleCrop()
                .placeholder(R.drawable.place_holder_user)
                .into(itemBinding.imageUser)
            if(!isPraisedSet.contains(curPosition)) {
                itemBinding.btnPraise.setBackgroundResource(R.drawable.ic_action_thumb_comment)
            }else {
                itemBinding.btnPraise.setBackgroundResource(R.drawable.ic_action_thumb_up)
            }

            //添加监听
            val clickListener = View.OnClickListener { view ->
                when (view?.id) {
                    R.id.image_user -> {
                        Toast.makeText(context, "click ${comment.userId}", Toast.LENGTH_SHORT).show()
                    }
                    R.id.btn_praise -> {
                        if(!isPraisedSet.contains(curPosition)) {
                            itemBinding.btnPraise.setBackgroundResource(R.drawable.ic_action_thumb_up)
                            isPraisedSet.add(curPosition)
                        }else{
                            itemBinding.btnPraise.setBackgroundResource(R.drawable.ic_action_thumb_comment)
                            isPraisedSet.remove(curPosition)
                        }
                    }
                    else -> {}
                }
            }
            itemBinding.imageUser.setOnClickListener(clickListener)
            itemBinding.btnPraise.setOnClickListener(clickListener)
        }
    }
}