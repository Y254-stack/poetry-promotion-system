package com.example.poetry.features.favorite.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemPoemSummaryBinding
import com.example.poetry.features.favorite.model.FavoritePostUiModel

class FavoritePostAdapter(
    private val showRemove: Boolean = false,
    private val onRemove: ((FavoritePostUiModel) -> Unit)? = null,
    private val onClick: (FavoritePostUiModel) -> Unit
) : RecyclerView.Adapter<FavoritePostAdapter.FavoritePostViewHolder>() {

    private val items = mutableListOf<FavoritePostUiModel>()

    fun submitList(data: List<FavoritePostUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritePostViewHolder {
        val binding = ItemPoemSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavoritePostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoritePostViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class FavoritePostViewHolder(
        private val binding: ItemPoemSummaryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FavoritePostUiModel) {
            binding.titleText.text = item.title
            binding.authorText.text = item.authorLine
            binding.snippetText.text = item.snippet
            binding.root.setOnClickListener { onClick(item) }

            if (showRemove && onRemove != null) {
                binding.removeFavoriteButton.visibility = View.VISIBLE
                binding.removeFavoriteButton.setOnClickListener { onRemove.invoke(item) }
            } else {
                binding.removeFavoriteButton.visibility = View.GONE
                binding.removeFavoriteButton.setOnClickListener(null)
            }
        }
    }
}
