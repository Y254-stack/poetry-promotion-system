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
import com.example.poetry.core.util.SearchHistoryManager
import com.example.poetry.databinding.FragmentSearchResultBinding
import com.example.poetry.features.poem.adapter.PoemSummaryAdapter
import com.example.poetry.features.poem.adapter.SearchHistoryAdapter
import com.example.poetry.features.poem.model.SearchType
import com.example.poetry.features.poem.model.TagSearchSort
import com.example.poetry.features.poem.model.TagUiModel
import com.example.poetry.features.poem.viewmodel.PoemViewModel
import com.google.android.material.chip.Chip

class SearchResultFragment : Fragment(R.layout.fragment_search_result) {

    private var _binding: FragmentSearchResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PoemViewModel by viewModels()
    private lateinit var adapter: PoemSummaryAdapter
    private lateinit var historyAdapter: SearchHistoryAdapter
    private lateinit var searchHistoryManager: SearchHistoryManager

    private var searchType: SearchType = SearchType.TAG
    private var selectedTagIds: MutableList<Long> = mutableListOf()
    private var selectedTagNames: MutableList<String> = mutableListOf()
    private var titleQuery: String = ""
    private var currentSearchSubType: String = "TITLE" // TITLE, AUTHOR, ALL

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchResultBinding.bind(view)

        searchHistoryManager = SearchHistoryManager(requireContext())

        searchType = SearchType.valueOf(arguments?.getString("searchType") ?: "TAG")
        selectedTagIds = (arguments?.getLongArray("selectedTagIds") ?: longArrayOf()).toMutableList()
        selectedTagNames = (arguments?.getStringArrayList("selectedTagNames") ?: arrayListOf()).toMutableList()
        titleQuery = arguments?.getString("titleQuery") ?: ""

        setupResultList()
        setupSearchHistory()
        setupSortToggle()
        setupPagination()
        setupSearchInput()

        when (searchType) {
            SearchType.TAG -> {
                binding.sortToggleGroup.check(R.id.hotSortButton)
                binding.sortToggleGroup.isVisible = true
                binding.searchInputLayout.isVisible = false
                renderSelectedTags()
                bindTagSearchState()
                if (selectedTagIds.isNotEmpty()) {
                    viewModel.searchByTags(selectedTagIds, TagSearchSort.HOT, page = 1)
                }
            }
           SearchType.TITLE -> {
    binding.sortToggleGroup.isVisible = false
    binding.selectedTagChipGroup.isVisible = false
    binding.searchInput.setText(titleQuery)
    setupSearchTypeToggle()
    bindTitleSearchState()
    
    // 在bindTitleSearchState之后确保搜索框可见
    binding.searchTypeToggleGroup.isVisible = true
    binding.searchTypeToggleGroup.check(R.id.searchByTitleButton)
    binding.searchInputLayout.isVisible = true
    binding.searchButton.isVisible = true
    
    if (titleQuery.isNotEmpty()) {
        performSearch(titleQuery, 1)
    }
}

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

    private fun setupSearchHistory() {
        historyAdapter = SearchHistoryAdapter(
            onItemClick = { query ->
                binding.searchInput.setText(query)
                titleQuery = query
                performSearch(query, 1)
            },
            onDeleteClick = { query ->
                searchHistoryManager.removeSearchHistory(query)
                updateSearchHistory()
            }
        )
        binding.historyRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        binding.clearHistoryButton.setOnClickListener {
            searchHistoryManager.clearAllHistory()
            updateSearchHistory()
        }

        updateSearchHistory()
    }

    private fun updateSearchHistory() {
        val history = searchHistoryManager.getSearchHistory()
        historyAdapter.submitList(history)
        binding.searchHistoryContainer.isVisible = history.isNotEmpty() && searchType == SearchType.TITLE
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
            when (searchType) {
                SearchType.TAG -> {
                    val state = viewModel.searchUiState.value ?: return@setOnClickListener
                    if (state.currentPage > 1) {
                        viewModel.searchByTags(selectedTagIds, state.sort, page = state.currentPage - 1)
                    }
                }
                SearchType.TITLE -> {
                    val state = viewModel.titleSearchUiState.value ?: return@setOnClickListener
                    if (state.currentPage > 1) {
                        performSearch(titleQuery, state.currentPage - 1)
                    }
                }
            }
        }

        binding.nextPageButton.setOnClickListener {
            when (searchType) {
                SearchType.TAG -> {
                    val state = viewModel.searchUiState.value ?: return@setOnClickListener
                    if (state.currentPage < state.totalPages) {
                        viewModel.searchByTags(selectedTagIds, state.sort, page = state.currentPage + 1)
                    }
                }
                SearchType.TITLE -> {
                    val state = viewModel.titleSearchUiState.value ?: return@setOnClickListener
                    if (state.currentPage < state.totalPages) {
                        performSearch(titleQuery, state.currentPage + 1)
                    }
                }
            }
        }
    }

    private fun setupSearchInput() {
        binding.searchButton.setOnClickListener {
            val query = binding.searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                titleQuery = query
                searchHistoryManager.addSearchHistory(query)
                updateSearchHistory()
                performSearch(query, 1)
            }
        }
    }

    private fun setupSearchTypeToggle() {
        binding.searchTypeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            currentSearchSubType = when (checkedId) {
                R.id.searchByTitleButton -> "TITLE"
                R.id.searchByAuthorButton -> "AUTHOR"
                R.id.searchByAllButton -> "ALL"
                else -> "TITLE"
            }

            // Update hint based on search type
            binding.searchInput.hint = when (currentSearchSubType) {
                "TITLE" -> "输入诗词标题"
                "AUTHOR" -> "输入诗人名字"
                "ALL" -> "输入诗词标题或诗人"
                else -> "输入诗词标题"
            }

            // Re-search if there's a query
            if (titleQuery.isNotEmpty()) {
                performSearch(titleQuery, 1)
            }
        }
    }

    private fun performSearch(query: String, page: Int) {
        when (currentSearchSubType) {
            "TITLE" -> viewModel.searchByTitle(query, page)
            "AUTHOR" -> viewModel.searchByAuthor(query, page)
            "ALL" -> viewModel.searchByAll(query, page)
        }
    }

    private fun bindTagSearchState() {
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

    private fun bindTitleSearchState() {
        viewModel.titleSearchUiState.observe(viewLifecycleOwner) { state ->
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
            binding.recommendTitle.isVisible = false
            binding.recommendedChipGroup.isVisible = false

            binding.searchHistoryContainer.isVisible = !hasResults && searchHistoryManager.getSearchHistory().isNotEmpty()
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
