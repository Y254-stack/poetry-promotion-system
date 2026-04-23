package com.example.poetry.features.poem.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentSearchResultBinding
import com.example.poetry.features.poem.adapter.PoemSummaryAdapter
import com.example.poetry.features.poem.model.TagSearchSort
import com.example.poetry.features.poem.model.TagUiModel
import com.example.poetry.features.poem.viewmodel.PoemViewModel
import com.google.android.material.chip.Chip

class SearchResultFragment : Fragment(R.layout.fragment_search_result) {

    private var _binding: FragmentSearchResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PoemViewModel by viewModels()
    private lateinit var adapter: PoemSummaryAdapter

    private var selectedTagIds: MutableList<Long> = mutableListOf()
    private var selectedTagNames: MutableList<String> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchResultBinding.bind(view)

        selectedTagIds = (arguments?.getLongArray("selectedTagIds") ?: longArrayOf()).toMutableList()
        selectedTagNames = (arguments?.getStringArrayList("selectedTagNames") ?: arrayListOf()).toMutableList()

        setupResultList()
        setupSortToggle()
        setupPagination()
        binding.sortToggleGroup.check(R.id.hotSortButton)
        renderSelectedTags()
        bindState()

        if (selectedTagIds.isNotEmpty()) {
            viewModel.searchByTags(selectedTagIds, TagSearchSort.HOT, page = 1)
        }
    }

    private fun setupResultList() {
        adapter = PoemSummaryAdapter {
            findNavController().navigate(
                R.id.poemDetailFragment,
                bundleOf("workId" to it.workId)
            )
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SearchResultFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
        }
    }

    private fun setupSortToggle() {
        binding.sortToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || selectedTagIds.isEmpty()) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.hotSortButton -> viewModel.searchByTags(selectedTagIds, TagSearchSort.HOT, page = 1)
                R.id.publishSortButton -> viewModel.searchByTags(selectedTagIds, TagSearchSort.PUBLISH_TIME, page = 1)
            }
        }
    }

    private fun setupPagination() {
        binding.previousPageButton.setOnClickListener {
            val state = viewModel.searchUiState.value ?: return@setOnClickListener
            if (state.currentPage > 1) {
                viewModel.searchByTags(selectedTagIds, state.sort, page = state.currentPage - 1)
            }
        }

        binding.nextPageButton.setOnClickListener {
            val state = viewModel.searchUiState.value ?: return@setOnClickListener
            if (state.currentPage < state.totalPages) {
                viewModel.searchByTags(selectedTagIds, state.sort, page = state.currentPage + 1)
            }
        }
    }

    private fun bindState() {
        viewModel.searchUiState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state.isLoading
            binding.errorText.isVisible = !state.errorMessage.isNullOrBlank()
            binding.errorText.text = state.errorMessage

            val hasResults = state.results.isNotEmpty()
            binding.recyclerView.isVisible = hasResults
            adapter.submitList(state.results)

            binding.paginationContainer.isVisible = hasResults && state.totalPages > 0
            binding.pageInfoText.text = if (state.totalPages > 0) {
                "第 ${state.currentPage} / ${state.totalPages} 页"
            } else {
                "第 0 / 0 页"
            }
            binding.previousPageButton.isEnabled = state.currentPage > 1
            binding.nextPageButton.isEnabled = state.currentPage < state.totalPages

            binding.emptyStateText.isVisible = !hasResults && !state.emptyMessage.isNullOrBlank()
            binding.emptyStateText.text = state.emptyMessage ?: ""
            binding.recommendTitle.isVisible = !hasResults && state.recommendedTags.isNotEmpty()
            binding.recommendedChipGroup.isVisible = !hasResults && state.recommendedTags.isNotEmpty()
            renderRecommendedTags(state.recommendedTags)
        }
    }

    private fun renderSelectedTags() {
        binding.selectedTagChipGroup.removeAllViews()
        selectedTagNames.forEach { tagName ->
            val chip = Chip(requireContext()).apply {
                text = tagName
                isCheckable = false
            }
            binding.selectedTagChipGroup.addView(chip)
        }
    }

    private fun renderRecommendedTags(tags: List<TagUiModel>) {
        binding.recommendedChipGroup.removeAllViews()
        tags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag.tagName
                isCheckable = false
                setOnClickListener {
                    selectedTagIds = mutableListOf(tag.tagId)
                    selectedTagNames = mutableListOf(tag.tagName)
                    renderSelectedTags()
                    binding.sortToggleGroup.check(R.id.hotSortButton)
                    viewModel.searchByTags(selectedTagIds, TagSearchSort.HOT, page = 1)
                }
            }
            binding.recommendedChipGroup.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
