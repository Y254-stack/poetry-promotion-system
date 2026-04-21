package com.example.poetry.features.learning.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemLearningModeBinding
import com.example.poetry.features.learning.model.LearningModeUiModel

class LearningModeAdapter(
    private val onClick: (LearningModeUiModel) -> Unit
) : RecyclerView.Adapter<LearningModeAdapter.LearningModeViewHolder>() {

    private val items = mutableListOf<LearningModeUiModel>()

    fun submitList(data: List<LearningModeUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LearningModeViewHolder {
        val binding =
            ItemLearningModeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LearningModeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LearningModeViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class LearningModeViewHolder(
        private val binding: ItemLearningModeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LearningModeUiModel) {
            binding.titleText.text = item.title
            binding.subtitleText.text = item.subtitle
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
