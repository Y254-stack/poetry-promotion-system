package com.example.poetry.features.community.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemCommentBinding
import com.example.poetry.features.community.model.CommentUiModel
import com.example.poetry.R

class CommentAdapter(
    private val currentUserId: Long?,
    private val onReplyClick: (CommentUiModel) -> Unit,
    private val onLikeClick: (CommentUiModel) -> Unit,
    private val onDeleteClick: (CommentUiModel) -> Unit,
    private val onAuthorClick: (Long, String) -> Unit = { _, _ -> }
) : ListAdapter<CommentUiModel, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    private val indentWidth = 40  // 每层缩进的宽度（dp）

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // 获取扁平化的评论列表（包含子评论）
    fun submitCommentsWithReplies(comments: List<CommentUiModel>) {
        val flatList = mutableListOf<CommentUiModel>()
        flattenComments(comments, 0, flatList)
        submitList(flatList)
    }

    private fun flattenComments(comments: List<CommentUiModel>, level: Int, result: MutableList<CommentUiModel>) {
        comments.forEach { comment ->
            // 添加层级信息
            result.add(comment.copy(
                level = level,
                replies = mutableListOf() // 清除子评论列表，避免重复
            ))
            // 递归处理子评论
            if (comment.replies.isNotEmpty()) {
                flattenComments(comment.replies, level + 1, result)
            }
        }
    }

    inner class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: CommentUiModel) {
            // 设置缩进（根据层级，最大缩进1层）
            val maxLevel = 1  // 最多缩进1层
            val actualLevel = minOf(comment.level, maxLevel)
            val indentPx = actualLevel * indentWidth * binding.root.context.resources.displayMetrics.density
            binding.vIndent.layoutParams.width = indentPx.toInt()
            
            binding.tvCommentAuthor.text = comment.author
            
            // 如果是回复，显示回复对象
            val content = if (comment.replyToAuthor != null && comment.replyToAuthor.isNotEmpty()) {
                "@${comment.replyToAuthor}: ${comment.content}"
            } else {
                comment.content
            }
            binding.tvCommentContent.text = content
            
            binding.tvCommentTime.text = comment.time
            binding.tvLikeCount.text = comment.likeCount.toString()
            
            // 设置头像首字母
            binding.tvCommentAvatar.text = if (comment.author.isNotEmpty()) {
                comment.author.take(1).uppercase()
            } else {
                "?"
            }
            
            // 设置点赞状态
            updateLikeStatus(comment.isLiked)
            
            // 设置删除按钮可见性（只有自己的评论才能删除）
            val isOwnComment = currentUserId != null && comment.userId == currentUserId
            binding.btnDelete.visibility = if (isOwnComment) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
            binding.dividerDelete.visibility = binding.btnDelete.visibility
            
            // 回复按钮始终可见
            binding.btnReply.visibility = android.view.View.VISIBLE
            
            binding.btnReply.setOnClickListener {
                onReplyClick(comment)
            }
            
            binding.btnLike.setOnClickListener {
                onLikeClick(comment)
            }
            
            binding.btnDelete.setOnClickListener {
                onDeleteClick(comment)
            }
            
            // 头像点击跳转到用户主页
            binding.tvCommentAvatar.setOnClickListener {
                onAuthorClick(comment.userId, comment.author)
            }
            
            // 昵称点击跳转到用户主页
            binding.tvCommentAuthor.setOnClickListener {
                onAuthorClick(comment.userId, comment.author)
            }
        }
        
        private fun updateLikeStatus(isLiked: Boolean) {
            if (isLiked) {
                binding.ivLike.setImageResource(R.drawable.ic_like_filled)
                binding.ivLike.setColorFilter(binding.root.context.resources.getColor(R.color.red))
            } else {
                binding.ivLike.setImageResource(R.drawable.ic_like_outline)
                binding.ivLike.setColorFilter(binding.root.context.resources.getColor(R.color.text_light_gray))
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