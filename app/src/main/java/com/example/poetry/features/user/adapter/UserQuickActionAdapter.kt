package com.example.poetry.features.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemUserActionBinding
import com.example.poetry.features.user.model.UserQuickActionUiModel

class UserQuickActionAdapter(
    private val onClick: (UserQuickActionUiModel) -> Unit
) : RecyclerView.Adapter<UserQuickActionAdapter.UserQuickActionViewHolder>() {

    private val items = mutableListOf<UserQuickActionUiModel>()

    fun submitList(data: List<UserQuickActionUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserQuickActionViewHolder {
        val binding =
            ItemUserActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserQuickActionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserQuickActionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class UserQuickActionViewHolder(
        private val binding: ItemUserActionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserQuickActionUiModel) {
            binding.titleText.text = item.title
            binding.subtitleText.text = item.subtitle
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
