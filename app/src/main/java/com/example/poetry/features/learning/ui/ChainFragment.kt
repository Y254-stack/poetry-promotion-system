package com.example.poetry.features.learning.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.R
import com.example.poetry.databinding.FragmentChainBinding
import com.example.poetry.databinding.ItemChatMessageBinding
import com.example.poetry.features.learning.viewmodel.ChainViewModel
import androidx.fragment.app.viewModels
import kotlinx.coroutines.launch

class ChainFragment : Fragment(R.layout.fragment_chain) {

    private var _binding: FragmentChainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChainViewModel by viewModels()
    private lateinit var chatAdapter: ChainChatAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChainBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChainChatAdapter()
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        binding.btnSubmit.setOnClickListener {
            val input = binding.etInput.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(requireContext(), "请输入诗句", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val success = viewModel.submitAnswer(input)
                if (success) {
                    binding.etInput.setText("")
                }
            }
        }

        binding.btnRestart.setOnClickListener {
            viewModel.startGame()
        }

        binding.btnExit.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun observeViewModel() {
        viewModel.currentChar.observe(viewLifecycleOwner) { char ->
            if (char.isNotEmpty()) {
                binding.tvCurrentChar.text = "🔗 接龙字：【${char}】"
            }
        }

        viewModel.chatMessages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages)
            binding.rvChat.scrollToPosition(messages.size - 1)
        }

        viewModel.isPlayerTurn.observe(viewLifecycleOwner) { isPlayerTurn ->
            binding.btnSubmit.isEnabled = isPlayerTurn && viewModel.gameOver.value != true
            binding.etInput.isEnabled = isPlayerTurn && viewModel.gameOver.value != true
            if (isPlayerTurn && viewModel.gameOver.value != true) {
                binding.tvStatus.text = "👤 轮到你了"
            } else if (!isPlayerTurn && viewModel.gameOver.value != true) {
                binding.tvStatus.text = "🤖 AI思考中..."
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSubmit.isEnabled = !isLoading && viewModel.isPlayerTurn.value == true
        }

        viewModel.gameOver.observe(viewLifecycleOwner) { isOver ->
            if (isOver) {
                binding.btnSubmit.isEnabled = false
                binding.etInput.isEnabled = false
                viewModel.winner.value?.let { winner ->
                    binding.tvStatus.text = if (winner == "我") "🎉 恭喜获胜！🎉" else "😢 AI获胜..."
                }
            }
        }

        viewModel.timeRemaining.observe(viewLifecycleOwner) { time ->
            binding.tvTimer.text = if (time > 0) {
                "⏱️ ${time}秒"
            } else {
                "⏱️ 时间到！"
            }
        }

        viewModel.roundCount.observe(viewLifecycleOwner) { round ->
            binding.tvRound.text = "    🔢 第${round + 1}轮"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ChainChatAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<ChainChatAdapter.ViewHolder>() {

    private val messages = mutableListOf<ChainViewModel.ChatMessage>()

    fun submitList(newList: List<ChainViewModel.ChatMessage>) {
        messages.clear()
        messages.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    class ViewHolder(private val binding: ItemChatMessageBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChainViewModel.ChatMessage) {
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