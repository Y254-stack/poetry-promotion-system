package com.example.poetry.features.auth.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.databinding.ItemAuthTipBinding
import com.example.poetry.features.auth.model.AuthTipUiModel

class AuthTipsAdapter : RecyclerView.Adapter<AuthTipsAdapter.AuthTipViewHolder>() {

    private val items = mutableListOf<AuthTipUiModel>()

    fun submitList(data: List<AuthTipUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuthTipViewHolder {
        val binding = ItemAuthTipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AuthTipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AuthTipViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AuthTipViewHolder(
        private val binding: ItemAuthTipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AuthTipUiModel) {
            binding.titleText.text = item.title
            binding.descriptionText.text = item.description
        }
    }
}
