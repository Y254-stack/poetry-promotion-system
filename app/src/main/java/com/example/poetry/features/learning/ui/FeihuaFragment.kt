package com.example.poetry.features.learning.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.databinding.FragmentFeihuaBinding
import com.example.poetry.features.learning.viewmodel.FeihuaViewModel
import androidx.fragment.app.viewModels
import kotlinx.coroutines.launch

class FeihuaFragment : Fragment(R.layout.fragment_feihua) {

    private var _binding: FragmentFeihuaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeihuaViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFeihuaBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
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

        binding.btnChangeKeyword.setOnClickListener {
            viewModel.changeKeyword()
        }

        binding.btnExit.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun observeViewModel() {
        viewModel.keyword.observe(viewLifecycleOwner) { keyword ->
            binding.tvKeyword.text = "🔖 关键字：$keyword"
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}