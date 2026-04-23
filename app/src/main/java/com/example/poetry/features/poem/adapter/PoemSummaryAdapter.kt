package com.example.poetry.features.poem.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemPoemSummaryBinding
import com.example.poetry.features.poem.model.PoemSummaryUiModel

class PoemSummaryAdapter(
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
            binding.tagText.text =
                if (item.matchedTags.isEmpty()) "标签待补充" else item.matchedTags.joinToString(" / ")
            binding.metaText.text = "热度 ${"%.1f".format(item.hotScore)} · 入库 ${item.publishTime}"
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
