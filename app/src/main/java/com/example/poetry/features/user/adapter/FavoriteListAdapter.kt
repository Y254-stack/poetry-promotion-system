package com.example.poetry.features.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.R
import com.example.poetry.databinding.ItemUserFavoriteBinding
import com.example.poetry.features.user.model.FavoriteItemUiModel
import com.example.poetry.features.user.model.FavoriteTypeUi

class FavoriteListAdapter(
    private val onOpen: (FavoriteItemUiModel) -> Unit,
    private val onUnfavorite: (FavoriteItemUiModel) -> Unit
) : RecyclerView.Adapter<FavoriteListAdapter.VH>() {

    private val items = mutableListOf<FavoriteItemUiModel>()

    fun submitList(data: List<FavoriteItemUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUserFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val binding: ItemUserFavoriteBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FavoriteItemUiModel) {
            val ctx = binding.root.context
            binding.badgeText.text = when (item.type) {
                FavoriteTypeUi.POST -> "帖子"
                FavoriteTypeUi.POEM -> "诗词"
            }
            val badgeTint = when (item.type) {
                FavoriteTypeUi.POST -> R.color.user_center_blue
                FavoriteTypeUi.POEM -> R.color.user_center_green
            }
            binding.badgeText.setTextColor(ContextCompat.getColor(ctx, R.color.black))
            binding.badgeText.backgroundTintList = ContextCompat.getColorStateList(ctx, badgeTint)

            binding.titleText.text = item.title
            binding.subtitleText.text = item.subtitle

            binding.contentArea.setOnClickListener { onOpen(item) }
            binding.unfavoriteButton.setOnClickListener { onUnfavorite(item) }
        }
    }
}

