package com.example.poetry.features.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.core.auth.SavedLoginAccount
import com.example.poetry.databinding.ItemSavedAccountBinding

class SavedAccountsAdapter(
    private val onSwitch: (SavedLoginAccount) -> Unit,
    private val onRemove: (SavedLoginAccount) -> Unit
) : RecyclerView.Adapter<SavedAccountsAdapter.VH>() {

    private var items: List<SavedLoginAccount> = emptyList()
    private var currentUserId: Long = 0L

    fun submit(list: List<SavedLoginAccount>, currentId: Long) {
        items = list
        currentUserId = currentId
        notifyDataSetChanged()
    }

    class VH(val binding: ItemSavedAccountBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSavedAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val acc = items[position]
        val binding = holder.binding
        val nick = acc.nickname.trim().ifBlank { acc.username }
        binding.nicknameText.text = nick
        binding.usernameText.text = acc.username
        val isCurrent = acc.userId == currentUserId
        binding.currentBadge.isVisible = isCurrent
        binding.switchButton.isEnabled = !isCurrent
        binding.switchButton.alpha = if (isCurrent) 0.45f else 1f
        binding.switchButton.setOnClickListener { onSwitch(acc) }
        binding.removeButton.setOnClickListener { onRemove(acc) }
    }
}
