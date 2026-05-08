package com.example.poetry.features.community.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemCommentBinding
import com.example.poetry.features.community.model.CommentUiModel

class CommentAdapter(
    private val onReplyClick: (CommentUiModel) -> Unit
) : ListAdapter<CommentUiModel, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: CommentUiModel) {
            binding.commentNickname.text = comment.author
            binding.commentContent.text = comment.content
            binding.commentTime.text = comment.time
            
            binding.replyButton.setOnClickListener {
                onReplyClick(comment)
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<CommentUiModel>() {
        override fun areItemsTheSame(oldItem: CommentUiModel, newItem: CommentUiModel): Boolean {
            return oldItem.commentId == newItem.commentId
        }

        override fun areContentsTheSame(oldItem: CommentUiModel, newItem: CommentUiModel): Boolean {
            return oldItem == newItem
        }
    }
}