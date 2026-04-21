package com.example.poetry.features.learning.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentLearningHubBinding
import com.example.poetry.features.learning.adapter.LearningModeAdapter
import com.example.poetry.features.learning.viewmodel.LearningViewModel

class LearningHubFragment : Fragment(R.layout.fragment_learning_hub) {

    private var _binding: FragmentLearningHubBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LearningViewModel by viewModels()
    private lateinit var adapter: LearningModeAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLearningHubBinding.bind(view)

        adapter = LearningModeAdapter { findNavController().navigate(it.destinationId) }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LearningHubFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
        }

        viewModel.modes.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
