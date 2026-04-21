package com.example.poetry.features.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemHomeCategoryBinding
import com.example.poetry.features.home.model.CategoryEntryUiModel

class CategoryAdapter(
    private val onClick: (CategoryEntryUiModel) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private val items = mutableListOf<CategoryEntryUiModel>()

    fun submitList(data: List<CategoryEntryUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemHomeCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CategoryViewHolder(
        private val binding: ItemHomeCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryEntryUiModel) {
            binding.titleText.text = item.title
            binding.subtitleText.text = item.subtitle
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
