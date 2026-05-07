package com.example.poetry.features.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemCategoryDetailBinding

data class CategoryDetailItem(
    val id: Long,
    val name: String,
    val info: String? = null
)

class CategoryDetailAdapter(
    private val onItemClick: (CategoryDetailItem) -> Unit
) : ListAdapter<CategoryDetailItem, CategoryDetailAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCategoryDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryDetailItem) {
            binding.categoryNameText.text = item.name
            if (item.info != null) {
                binding.categoryInfoText.text = item.info
                binding.categoryInfoText.isVisible = true
            } else {
                binding.categoryInfoText.isVisible = false
            }
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CategoryDetailItem>() {
        override fun areItemsTheSame(oldItem: CategoryDetailItem, newItem: CategoryDetailItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryDetailItem, newItem: CategoryDetailItem): Boolean {
            return oldItem == newItem
        }
    }
}
