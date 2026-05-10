package com.example.poetry.features.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemUserFollowBinding
import com.example.poetry.features.user.model.FollowUiModel

class FollowListAdapter(
    private val onOpenProfile: (FollowUiModel) -> Unit,
    private val onUnfollow: (FollowUiModel) -> Unit
) : RecyclerView.Adapter<FollowListAdapter.VH>() {

    private val items = mutableListOf<FollowUiModel>()

    fun submitList(data: List<FollowUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUserFollowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val binding: ItemUserFollowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FollowUiModel) {
            binding.badgeText.text = item.roleBadge
            binding.titleText.text = item.displayName
            binding.contentArea.setOnClickListener { onOpenProfile(item) }
            binding.unfollowButton.setOnClickListener { onUnfollow(item) }
        }
    }
}
