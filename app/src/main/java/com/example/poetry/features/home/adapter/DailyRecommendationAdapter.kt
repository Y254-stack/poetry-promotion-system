package com.example.poetry.features.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemHomeRecommendationBinding
import com.example.poetry.features.home.model.DailyRecommendationUiModel

class DailyRecommendationAdapter(
    private val onCardClick: (DailyRecommendationUiModel) -> Unit,
    private val onAuthorClick: (DailyRecommendationUiModel) -> Unit
) : RecyclerView.Adapter<DailyRecommendationAdapter.RecommendationViewHolder>() {

    private val items = mutableListOf<DailyRecommendationUiModel>()

    fun submitList(data: List<DailyRecommendationUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val binding = ItemHomeRecommendationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecommendationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class RecommendationViewHolder(
        private val binding: ItemHomeRecommendationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DailyRecommendationUiModel) {
            binding.tagText.text = item.tag
            binding.titleText.text = item.title
            binding.authorMetaText.text = "${item.author} \u00B7 ${item.dynasty}"
            binding.summaryText.text = item.summary
            binding.authorAvatarText.text = item.author.take(1).ifBlank { "?" }

            binding.root.setOnClickListener { onCardClick(item) }
            binding.authorRow.setOnClickListener { onAuthorClick(item) }
        }
    }
}
