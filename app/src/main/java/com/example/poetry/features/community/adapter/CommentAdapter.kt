package com.example.poetry.features.community.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemCommentBinding
import com.example.poetry.features.community.model.CommentUiModel

class CommentAdapter : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val items = mutableListOf<CommentUiModel>()

    fun submitList(data: List<CommentUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class CommentViewHolder(
        private val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CommentUiModel) {
            binding.authorText.text = item.author
            binding.contentText.text = item.content
            binding.timeText.text = item.time
        }
    }
}
