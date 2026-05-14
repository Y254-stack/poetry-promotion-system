package com.example.poetry.features.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemUserCreationBinding
import com.example.poetry.features.community.model.CommunityPostUiModel
import com.example.poetry.features.user.model.DraftUiModel
import java.text.SimpleDateFormat
import java.util.Locale

class CreationListAdapter(
    private val onOpen: (item: Any) -> Unit,
    private val onDelete: (item: Any) -> Unit
) : RecyclerView.Adapter<CreationListAdapter.VH>() {

    private val items = mutableListOf<Any>()
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    fun submitList(data: List<Any>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUserCreationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val binding: ItemUserCreationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Any) {
            when (item) {
                is CommunityPostUiModel -> {
                    binding.badgeText.text = item.tag.ifEmpty { "帖子" }
                    binding.titleText.text = item.title
                    binding.subtitleText.text = "${item.author} · ${dateFormat.format(item.createdAt)}"
                    binding.root.setOnClickListener { onOpen(item) }
                    binding.deleteButton.setOnClickListener { onDelete(item) }
                }
                is DraftUiModel -> {
                    binding.badgeText.text = item.tag?.ifEmpty { "草稿" } ?: "草稿"
                    binding.titleText.text = item.title.ifEmpty { "无标题" }
                    binding.subtitleText.text = "草稿 · ${dateFormat.format(item.updatedAt)}"
                    binding.root.setOnClickListener { onOpen(item) }
                    binding.deleteButton.setOnClickListener { onDelete(item) }
                }
            }
        }
    }
}