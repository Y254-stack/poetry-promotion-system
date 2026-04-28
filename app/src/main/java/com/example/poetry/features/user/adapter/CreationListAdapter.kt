package com.example.poetry.features.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemUserCreationBinding
import com.example.poetry.features.user.model.UserCollectionUiModel

class CreationListAdapter(
    private val onOpen: (UserCollectionUiModel) -> Unit,
    private val onDelete: (UserCollectionUiModel) -> Unit
) : RecyclerView.Adapter<CreationListAdapter.VH>() {

    private val items = mutableListOf<UserCollectionUiModel>()

    fun submitList(data: List<UserCollectionUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUserCreationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val binding: ItemUserCreationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserCollectionUiModel) {
            binding.badgeText.text = item.badge
            binding.titleText.text = item.title
            binding.subtitleText.text = item.subtitle
            binding.root.setOnClickListener { onOpen(item) }
            binding.deleteButton.setOnClickListener { onDelete(item) }
        }
    }
}
