package com.example.poetry.features.poem.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemPoemSummaryBinding
import com.example.poetry.features.poem.model.PoemSummaryUiModel

class PoemSummaryAdapter(
    private val showRemoveFavorite: Boolean = false,
    private val onRemoveFavorite: ((PoemSummaryUiModel) -> Unit)? = null,
    private val onClick: (PoemSummaryUiModel) -> Unit
) : RecyclerView.Adapter<PoemSummaryAdapter.PoemSummaryViewHolder>() {

    private val items = mutableListOf<PoemSummaryUiModel>()

    fun submitList(data: List<PoemSummaryUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PoemSummaryViewHolder {
        val binding =
            ItemPoemSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PoemSummaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PoemSummaryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PoemSummaryViewHolder(
        private val binding: ItemPoemSummaryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PoemSummaryUiModel) {
            binding.titleText.text = item.title
            binding.authorText.text = "${item.author} · ${item.dynasty}"
            binding.snippetText.text = item.snippet
            binding.root.setOnClickListener { onClick(item) }

            if (showRemoveFavorite && onRemoveFavorite != null) {
                binding.removeFavoriteButton.visibility = View.VISIBLE
                binding.removeFavoriteButton.setOnClickListener {
                    onRemoveFavorite.invoke(item)
                }
            } else {
                binding.removeFavoriteButton.visibility = View.GONE
                binding.removeFavoriteButton.setOnClickListener(null)
            }
        }
    }
}
