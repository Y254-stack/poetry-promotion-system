package com.example.poetry.features.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemUserCollectionBinding
import com.example.poetry.features.user.model.UserCollectionUiModel

class UserCollectionAdapter(
    private val onClick: (UserCollectionUiModel) -> Unit
) : RecyclerView.Adapter<UserCollectionAdapter.UserCollectionViewHolder>() {

    private val items = mutableListOf<UserCollectionUiModel>()

    fun submitList(data: List<UserCollectionUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserCollectionViewHolder {
        val binding =
            ItemUserCollectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserCollectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserCollectionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class UserCollectionViewHolder(
        private val binding: ItemUserCollectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserCollectionUiModel) {
            binding.titleText.text = item.title
            binding.subtitleText.text = item.subtitle
            binding.badgeText.text = item.badge
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
