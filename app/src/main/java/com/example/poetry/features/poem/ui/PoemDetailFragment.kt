package com.example.poetry.features.poem.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentPoemDetailBinding
import com.example.poetry.features.poem.adapter.PoemSummaryAdapter
import com.example.poetry.features.poem.viewmodel.PoemViewModel

class PoemDetailFragment : Fragment(R.layout.fragment_poem_detail) {

    private var _binding: FragmentPoemDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PoemViewModel by viewModels()
    private lateinit var relatedAdapter: PoemSummaryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPoemDetailBinding.bind(view)

        relatedAdapter = PoemSummaryAdapter {
            // TODO: 传递真实相关推荐参数
        }
        binding.relatedRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = relatedAdapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(10)))
            isNestedScrollingEnabled = false
        }

        viewModel.poemDetail.observe(viewLifecycleOwner) { detail ->
            binding.titleText.text = detail.title
            binding.authorButton.text = "${detail.author} · ${detail.dynasty}"
            binding.contentText.text = detail.content
            binding.translationText.text = detail.translation
            binding.annotationText.text = detail.annotation
        }
        viewModel.searchResults.observe(viewLifecycleOwner) { relatedAdapter.submitList(it) }

        binding.authorButton.setOnClickListener {
            findNavController().navigate(R.id.action_poemDetail_to_authorDetail)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
