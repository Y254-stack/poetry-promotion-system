package com.example.poetry.features.community.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemCommunityPostBinding
import com.example.poetry.features.community.model.CommunityPostUiModel
import java.text.SimpleDateFormat
import java.util.Locale

class PostAdapter(
    private val onClick: (CommunityPostUiModel) -> Unit,
    private val onAuthorClick: (Long, String) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val items = mutableListOf<CommunityPostUiModel>()
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    fun submitList(data: List<CommunityPostUiModel>) {
        items.clear()
        items.addAll(data)
        try {
            notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrentItem(position: Int): CommunityPostUiModel? {
        return if (position >= 0 && position < items.size) {
            items[position]
        } else {
            null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemCommunityPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        if (position < items.size) {
            holder.bind(items[position])
        }
    }

    override fun getItemCount(): Int = items.size

    inner class PostViewHolder(
        private val binding: ItemCommunityPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CommunityPostUiModel) {
            try {
                binding.authorText.text = item.author
                binding.titleText.text = item.title
                binding.previewText.text = item.preview
                binding.tagText.text = item.tag.ifEmpty { "分享" }
                binding.timeText.text = dateFormat.format(item.createdAt)
                binding.statsText.text = "${item.viewCount}阅读 · ${item.commentCount}评论 · ${item.likeCount}点赞"

                binding.authorAvatar.text = if (item.author.isNotEmpty()) item.author.take(1).uppercase() else "?"

                binding.root.setOnClickListener { onClick(item) }

                binding.authorLayout.setOnClickListener {
                    onAuthorClick(item.userId, item.author)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
