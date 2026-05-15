package com.example.poetry.features.learning.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.R
import com.example.poetry.databinding.ItemChatMessageBinding
import com.example.poetry.features.learning.viewmodel.FeihuaViewModel

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val messages = mutableListOf<FeihuaViewModel.ChatMessage>()

    fun submitList(newList: List<FeihuaViewModel.ChatMessage>) {
        messages.clear()
        messages.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    class ViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: FeihuaViewModel.ChatMessage) {
            binding.tvSender.text = message.sender
            binding.tvMessage.text = message.message

            // 根据发送者改变样式
            if (message.sender == "我") {
                binding.tvSender.setTextColor(0xFF4CAF50.toInt())
                binding.tvMessage.setBackgroundResource(R.drawable.bg_my_message)
            } else if (message.sender == "AI") {
                binding.tvSender.setTextColor(0xFF2196F3.toInt())
                binding.tvMessage.setBackgroundResource(R.drawable.bg_ai_message)
            } else {
                binding.tvSender.setTextColor(0xFFFF9800.toInt())
                binding.tvMessage.setBackgroundResource(R.drawable.bg_system_message)
            }
        }
    }
}