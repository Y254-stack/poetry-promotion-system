package com.example.poetry.features.poem.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentAuthorDetailBinding
import com.example.poetry.features.poem.adapter.PoemSummaryAdapter
import com.example.poetry.features.poem.viewmodel.PoemViewModel

class AuthorDetailFragment : Fragment(R.layout.fragment_author_detail) {

    private var _binding: FragmentAuthorDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PoemViewModel by viewModels()
    private lateinit var worksAdapter: PoemSummaryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAuthorDetailBinding.bind(view)

        worksAdapter = PoemSummaryAdapter {
            findNavController().navigate(
                R.id.poemDetailFragment,
                bundleOf("workId" to it.workId)
            )
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = worksAdapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(10)))
            isNestedScrollingEnabled = false
        }

        viewModel.authorProfile.observe(viewLifecycleOwner) {
            binding.nameText.text = it.name
            binding.dynastyText.text = it.dynasty
            binding.introText.text = it.intro
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
