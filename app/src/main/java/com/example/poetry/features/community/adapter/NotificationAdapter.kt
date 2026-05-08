package com.example.poetry.features.community.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemNotificationBinding
import com.example.poetry.features.community.model.NotificationUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val onItemClick: (NotificationUiModel) -> Unit,
    private val onDeleteClick: (NotificationUiModel) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val items = mutableListOf<NotificationUiModel>()

    fun submitList(data: List<NotificationUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding =
            ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class NotificationViewHolder(
        private val binding: ItemNotificationBinding,
        private val onItemClick: (NotificationUiModel) -> Unit,
        private val onDeleteClick: (NotificationUiModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        fun bind(item: NotificationUiModel) {
            binding.tvActorAvatar.text = if (item.actorName.isNotEmpty()) item.actorName.take(1).uppercase() else "?"
            binding.tvActorName.text = item.actorName

            val actionText = when (item.type) {
                "LIKE" -> "点赞了你的帖子"
                "COLLECT" -> "收藏了你的帖子"
                "COMMENT" -> "评论了你的帖子"
                else -> "与你产生了互动"
            }
            binding.tvAction.text = actionText

            binding.tvPostTitle.text = item.postTitle

            if (item.type == "COMMENT" && item.commentContent != null) {
                binding.tvCommentContent.text = item.commentContent
                binding.tvCommentContent.visibility = android.view.View.VISIBLE
            } else {
                binding.tvCommentContent.visibility = android.view.View.GONE
            }

            binding.tvTime.text = formatTime(item.createdAt)

            binding.unreadIndicator.visibility = if (item.isRead) android.view.View.GONE else android.view.View.VISIBLE

            binding.notificationContainer.setOnClickListener {
                onItemClick(item)
            }

            binding.ivDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }

        private fun formatTime(date: Date): String {
            val now = Date()
            val diff = now.time - date.time
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                seconds < 60 -> "刚刚"
                minutes < 60 -> "${minutes}分钟前"
                hours < 24 -> "${hours}小时前"
                days < 7 -> "${days}天前"
                else -> dateFormat.format(date)
            }
        }
    }
}
