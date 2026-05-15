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
import com.example.poetry.core.util.SearchHistoryManager
import com.example.poetry.core.util.dp
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
    private var dynastyName: String = ""
    private var authorId: Long = 0L
    private var authorName: String = ""
    private var currentSearchSubType: String = SUBTYPE_TITLE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchResultBinding.bind(view)

        searchHistoryManager = SearchHistoryManager(requireContext())

        searchType = SearchType.valueOf(arguments?.getString("searchType") ?: SearchType.TAG.name)
        selectedTagIds = (arguments?.getLongArray("selectedTagIds") ?: longArrayOf()).toMutableList()
        selectedTagNames =
            (arguments?.getStringArrayList("selectedTagNames") ?: arrayListOf()).toMutableList()
        titleQuery = arguments?.getString("titleQuery").orEmpty()
        dynastyName = arguments?.getString("dynastyName").orEmpty()
        authorId = arguments?.getLong("authorId") ?: 0L
        authorName = arguments?.getString("authorName").orEmpty()

        setupResultList()
        setupSearchHistory()
        setupSortToggle()
        setupPagination()
        setupSearchInput()
        setupSearchTypeToggle()
        bindTagSearchState()
        bindTitleSearchState()
        applyInitialMode()
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
                performTextSearch(query, 1)
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

    private fun setupSortToggle() {
        binding.sortToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || !isTagMode() || selectedTagIds.isEmpty()) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.hotSortButton -> viewModel.searchByTags(selectedTagIds, TagSearchSort.HOT, page = 1)
                R.id.publishSortButton -> viewModel.searchByTags(
                    selectedTagIds,
                    TagSearchSort.PUBLISH_TIME,
                    page = 1
                )
            }
        }
    }

    private fun setupPagination() {
        binding.previousPageButton.setOnClickListener {
            when {
                isTagMode() -> {
                    val state = viewModel.searchUiState.value ?: return@setOnClickListener
                    if (state.currentPage > 1 && selectedTagIds.isNotEmpty()) {
                        viewModel.searchByTags(selectedTagIds, state.sort, page = state.currentPage - 1)
                    }
                }

                searchType == SearchType.TITLE -> {
                    val state = viewModel.titleSearchUiState.value ?: return@setOnClickListener
                    if (state.currentPage > 1) {
                        val query = state.query.ifBlank { titleQuery }
                        performTextSearch(query, state.currentPage - 1)
                    }
                }

                searchType == SearchType.DYNASTY -> {
                    val state = viewModel.titleSearchUiState.value ?: return@setOnClickListener
                    if (state.currentPage > 1) {
                        viewModel.searchByDynasty(dynastyName, state.currentPage - 1)
                    }
                }

                searchType == SearchType.AUTHOR_ID -> {
                    val state = viewModel.titleSearchUiState.value ?: return@setOnClickListener
                    if (state.currentPage > 1) {
                        viewModel.searchByAuthorId(authorId, authorName, state.currentPage - 1)
                    }
                }
            }
        }

        binding.nextPageButton.setOnClickListener {
            when {
                isTagMode() -> {
                    val state = viewModel.searchUiState.value ?: return@setOnClickListener
                    if (state.currentPage < state.totalPages && selectedTagIds.isNotEmpty()) {
                        viewModel.searchByTags(selectedTagIds, state.sort, page = state.currentPage + 1)
                    }
                }

                searchType == SearchType.TITLE -> {
                    val state = viewModel.titleSearchUiState.value ?: return@setOnClickListener
                    if (state.currentPage < state.totalPages) {
                        val query = state.query.ifBlank { titleQuery }
                        performTextSearch(query, state.currentPage + 1)
                    }
                }

                searchType == SearchType.DYNASTY -> {
                    val state = viewModel.titleSearchUiState.value ?: return@setOnClickListener
                    if (state.currentPage < state.totalPages) {
                        viewModel.searchByDynasty(dynastyName, state.currentPage + 1)
                    }
                }

                searchType == SearchType.AUTHOR_ID -> {
                    val state = viewModel.titleSearchUiState.value ?: return@setOnClickListener
                    if (state.currentPage < state.totalPages) {
                        viewModel.searchByAuthorId(authorId, authorName, state.currentPage + 1)
                    }
                }
            }
        }
    }

    private fun setupSearchInput() {
        binding.searchButton.setOnClickListener {
            val query = binding.searchInput.text?.toString()?.trim().orEmpty()
            if (query.isEmpty() || isTagMode()) return@setOnClickListener
            titleQuery = query
            searchHistoryManager.addSearchHistory(query)
            updateSearchHistory()
            performTextSearch(query, 1)
        }
    }

    private fun setupSearchTypeToggle() {
        binding.searchTypeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || searchType != SearchType.TITLE && searchType != SearchType.TAG) {
                return@addOnButtonCheckedListener
            }

            when (checkedId) {
                R.id.searchByTitleButton -> activateTextSearchMode(SUBTYPE_TITLE)
                R.id.searchByAuthorButton -> activateTextSearchMode(SUBTYPE_AUTHOR)
                R.id.searchByAllButton -> activateTextSearchMode(SUBTYPE_ALL)
                R.id.searchByTagButton -> activateTagMode()
            }
        }
    }

    private fun bindTagSearchState() {
        viewModel.searchUiState.observe(viewLifecycleOwner) { state ->
            if (!isTagMode()) return@observe

            binding.progressBar.isVisible = state.isLoading
            binding.errorText.isVisible = !state.errorMessage.isNullOrBlank()
            binding.errorText.text = state.errorMessage.orEmpty()

            val hasResults = state.results.isNotEmpty()
            binding.recyclerView.isVisible = hasResults
            adapter.submitList(state.results)
            if (hasResults) {
                scrollResultsToTop()
            }

            binding.paginationContainer.isVisible = hasResults && state.totalPages > 0
            binding.pageInfoText.text = if (state.totalPages > 0) {
                "${state.currentPage} / ${state.totalPages}"
            } else {
                "0 / 0"
            }
            binding.previousPageButton.isEnabled = state.currentPage > 1
            binding.nextPageButton.isEnabled = state.currentPage < state.totalPages

            val suggestionTags = when {
                state.selectedTagIds.isEmpty() -> state.availableTags.ifEmpty { state.recommendedTags }
                !hasResults -> state.recommendedTags.ifEmpty { state.availableTags }
                else -> emptyList()
            }

            binding.emptyStateText.isVisible =
                !hasResults && state.selectedTagIds.isNotEmpty() && !state.emptyMessage.isNullOrBlank()
            binding.emptyStateText.text = state.emptyMessage.orEmpty()

            binding.recommendTitle.isVisible = suggestionTags.isNotEmpty()
            binding.recommendedChipGroup.isVisible = suggestionTags.isNotEmpty()
            binding.recommendTitle.text = if (state.selectedTagIds.isEmpty()) {
                TITLE_HOT_TAGS
            } else {
                TITLE_TRY_OTHER_TAGS
            }
            renderSelectableTags(suggestionTags)

            syncSelectedTagNamesFromTags(suggestionTags)
            renderSelectedTags()
            updateSearchDescription()
        }
    }

    private fun bindTitleSearchState() {
        viewModel.titleSearchUiState.observe(viewLifecycleOwner) { state ->
            if (searchType == SearchType.TAG && isTagMode()) return@observe

            binding.progressBar.isVisible = state.isLoading
            binding.errorText.isVisible = !state.errorMessage.isNullOrBlank()
            binding.errorText.text = state.errorMessage.orEmpty()

            val hasResults = state.results.isNotEmpty()
            binding.recyclerView.isVisible = hasResults
            adapter.submitList(state.results)
            if (hasResults) {
                scrollResultsToTop()
            }

            binding.paginationContainer.isVisible = hasResults && state.totalPages > 0
            binding.pageInfoText.text = if (state.totalPages > 0) {
                "${state.currentPage} / ${state.totalPages}"
            } else {
                "0 / 0"
            }
            binding.previousPageButton.isEnabled = state.currentPage > 1
            binding.nextPageButton.isEnabled = state.currentPage < state.totalPages

            binding.emptyStateText.isVisible = !hasResults && !state.emptyMessage.isNullOrBlank()
            binding.emptyStateText.text = state.emptyMessage.orEmpty()
            binding.recommendTitle.isVisible = false
            binding.recommendedChipGroup.isVisible = false

            updateSearchHistoryVisibility(hasResults = hasResults)
            updateSearchDescription()
        }
    }

    private fun applyInitialMode() {
        when (searchType) {
            SearchType.TAG -> {
                binding.searchTypeToggleGroup.isVisible = true
                binding.searchTypeToggleGroup.check(R.id.searchByTagButton)
            }

            SearchType.TITLE -> {
                binding.searchTypeToggleGroup.isVisible = true
                binding.searchTypeToggleGroup.check(R.id.searchByTitleButton)
                if (titleQuery.isNotBlank()) {
                    performTextSearch(titleQuery, 1)
                }
            }

            SearchType.DYNASTY -> {
                binding.searchTypeToggleGroup.isVisible = false
                binding.searchInputLayout.isVisible = false
                binding.searchButton.isVisible = false
                binding.selectedTagChipGroup.isVisible = false
                binding.sortToggleGroup.isVisible = false
                binding.searchHistoryContainer.isVisible = false
                binding.searchDescription.text =
                    "\u6b63\u5728\u6d4f\u89c8\u300c${dynastyName}\u300d\u671d\u4ee3\u7684\u8bd7\u8bcd\u4f5c\u54c1\u3002"
                if (dynastyName.isNotBlank()) {
                    viewModel.searchByDynasty(dynastyName, 1)
                }
            }

            SearchType.AUTHOR_ID -> {
                binding.searchTypeToggleGroup.isVisible = false
                binding.searchInputLayout.isVisible = false
                binding.searchButton.isVisible = false
                binding.selectedTagChipGroup.isVisible = false
                binding.sortToggleGroup.isVisible = false
                binding.searchHistoryContainer.isVisible = false
                binding.searchDescription.text =
                    "\u6b63\u5728\u6d4f\u89c8\u4f5c\u8005\u300c${authorName}\u300d\u7684\u4f5c\u54c1\uff0c\u53ef\u4ee5\u7ee7\u7eed\u7ffb\u9875\u67e5\u770b\u3002"
                if (authorId > 0L) {
                    viewModel.searchByAuthorId(authorId, authorName, 1)
                }
            }
        }
    }

    private fun activateTextSearchMode(subType: String) {
        searchType = SearchType.TITLE
        currentSearchSubType = subType

        binding.sortToggleGroup.isVisible = false
        binding.selectedTagChipGroup.isVisible = false
        binding.recommendTitle.isVisible = false
        binding.recommendedChipGroup.isVisible = false
        binding.searchInputLayout.isVisible = true
        binding.searchButton.isVisible = true

        if (binding.searchInput.text?.toString().orEmpty() != titleQuery) {
            binding.searchInput.setText(titleQuery)
            binding.searchInput.setSelection(binding.searchInput.text?.length ?: 0)
        }

        binding.searchInput.hint = when (subType) {
            SUBTYPE_AUTHOR -> "\u8f93\u5165\u8bd7\u4eba\u59d3\u540d"
            SUBTYPE_ALL -> "\u8f93\u5165\u8bd7\u540d\u6216\u4f5c\u8005"
            else -> "\u8f93\u5165\u8bd7\u8bcd\u6807\u9898"
        }

        updateSearchHistory()
        updateSearchDescription()
    }

    private fun activateTagMode() {
        searchType = SearchType.TAG
        currentSearchSubType = SUBTYPE_TAG

        binding.searchInputLayout.isVisible = false
        binding.searchButton.isVisible = false
        binding.searchHistoryContainer.isVisible = false
        binding.selectedTagChipGroup.isVisible = true
        binding.sortToggleGroup.isVisible = true
        if (binding.sortToggleGroup.checkedButtonId == View.NO_ID) {
            binding.sortToggleGroup.check(R.id.hotSortButton)
        }

        renderSelectedTags()
        updateSearchDescription()

        if (selectedTagIds.isNotEmpty()) {
            val sort = currentTagSort()
            viewModel.searchByTags(selectedTagIds, sort, page = 1)
        } else {
            viewModel.resetTagSearch()
            viewModel.loadHotTags()
        }
    }

    private fun performTextSearch(query: String, page: Int) {
        when (currentSearchSubType) {
            SUBTYPE_AUTHOR -> viewModel.searchByAuthor(query, page)
            SUBTYPE_ALL -> viewModel.searchByAll(query, page)
            else -> viewModel.searchByTitle(query, page)
        }
    }

    private fun updateSearchHistory() {
        val history = searchHistoryManager.getSearchHistory()
        historyAdapter.submitList(history)
        updateSearchHistoryVisibility(hasResults = binding.recyclerView.isVisible)
    }

    private fun updateSearchHistoryVisibility(hasResults: Boolean) {
        val showHistory = !isTagMode() &&
            searchType == SearchType.TITLE &&
            !hasResults &&
            searchHistoryManager.getSearchHistory().isNotEmpty()
        binding.searchHistoryContainer.isVisible = showHistory
    }

    private fun updateSearchDescription() {
        binding.searchDescription.text = when {
            searchType == SearchType.DYNASTY -> {
                "\u6b63\u5728\u6d4f\u89c8\u300c${dynastyName}\u300d\u671d\u4ee3\u7684\u8bd7\u8bcd\u4f5c\u54c1\u3002"
            }

            searchType == SearchType.AUTHOR_ID -> {
                "\u6b63\u5728\u6d4f\u89c8\u4f5c\u8005\u300c${authorName}\u300d\u7684\u4f5c\u54c1\uff0c\u53ef\u4ee5\u7ee7\u7eed\u7ffb\u9875\u67e5\u770b\u3002"
            }

            isTagMode() && selectedTagNames.isNotEmpty() -> {
                "\u4ee5\u4e0b\u7ed3\u679c\u540c\u65f6\u5339\u914d\u6240\u9009\u6807\u7b7e\uff0c\u53ef\u6309\u70ed\u5ea6\u6216\u5165\u5e93\u65f6\u95f4\u6392\u5e8f\u3002"
            }

            isTagMode() -> {
                "\u9009\u62e9\u4e00\u4e2a\u6216\u591a\u4e2a\u70ed\u95e8\u6807\u7b7e\uff0c\u6309\u4e3b\u9898\u6d4f\u89c8\u8bd7\u8bcd\u3002"
            }

            currentSearchSubType == SUBTYPE_AUTHOR -> {
                "\u8f93\u5165\u8bd7\u4eba\u59d3\u540d\uff0c\u5feb\u901f\u67e5\u770b\u76f8\u5173\u4f5c\u54c1\u3002"
            }

            currentSearchSubType == SUBTYPE_ALL -> {
                "\u53ef\u540c\u65f6\u6309\u8bd7\u540d\u548c\u4f5c\u8005\u8fdb\u884c\u7efc\u5408\u641c\u7d22\u3002"
            }

            else -> {
                "\u8f93\u5165\u8bd7\u540d\u5173\u952e\u8bcd\uff0c\u5feb\u901f\u5b9a\u4f4d\u4f60\u60f3\u770b\u7684\u4f5c\u54c1\u3002"
            }
        }
    }

    private fun renderSelectedTags() {
        binding.selectedTagChipGroup.removeAllViews()
        binding.selectedTagChipGroup.isVisible = isTagMode() && selectedTagNames.isNotEmpty()
        selectedTagNames.forEach { tagName ->
            val chip = Chip(requireContext()).apply {
                text = tagName
                isCheckable = false
            }
            binding.selectedTagChipGroup.addView(chip)
        }
    }

    private fun renderSelectableTags(tags: List<TagUiModel>) {
        binding.recommendedChipGroup.removeAllViews()
        tags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag.tagName
                isCheckable = true
                isChecked = selectedTagIds.contains(tag.tagId)
                setOnClickListener {
                    toggleTagSelection(tag)
                }
            }
            binding.recommendedChipGroup.addView(chip)
        }
    }

    private fun toggleTagSelection(tag: TagUiModel) {
        val existingIndex = selectedTagIds.indexOf(tag.tagId)
        if (existingIndex >= 0) {
            selectedTagIds.removeAt(existingIndex)
            if (existingIndex < selectedTagNames.size) {
                selectedTagNames.removeAt(existingIndex)
            } else {
                selectedTagNames.remove(tag.tagName)
            }
        } else {
            selectedTagIds.add(tag.tagId)
            selectedTagNames.add(tag.tagName)
        }

        renderSelectedTags()
        updateSearchDescription()

        if (selectedTagIds.isEmpty()) {
            viewModel.resetTagSearch()
            viewModel.loadHotTags()
        } else {
            viewModel.searchByTags(selectedTagIds, currentTagSort(), page = 1)
        }
    }

    private fun syncSelectedTagNamesFromTags(tags: List<TagUiModel>) {
        if (selectedTagIds.isEmpty()) return
        if (selectedTagNames.size == selectedTagIds.size) return

        val lookup = tags.associateBy { it.tagId }
        selectedTagNames = selectedTagIds.map { tagId ->
            lookup[tagId]?.tagName ?: selectedTagNames.getOrNull(selectedTagIds.indexOf(tagId)).orEmpty()
        }.filter { it.isNotBlank() }.toMutableList()
    }

    private fun currentTagSort(): TagSearchSort {
        return if (binding.sortToggleGroup.checkedButtonId == R.id.publishSortButton) {
            TagSearchSort.PUBLISH_TIME
        } else {
            TagSearchSort.HOT
        }
    }

    private fun isTagMode(): Boolean = currentSearchSubType == SUBTYPE_TAG

    private fun scrollResultsToTop() {
        binding.recyclerView.post {
            binding.recyclerView.stopScroll()
            binding.recyclerView.scrollToPosition(0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        private const val SUBTYPE_TITLE = "TITLE"
        private const val SUBTYPE_AUTHOR = "AUTHOR"
        private const val SUBTYPE_ALL = "ALL"
        private const val SUBTYPE_TAG = "TAG"
        private const val TITLE_HOT_TAGS = "\u70ed\u95e8\u6807\u7b7e"
        private const val TITLE_TRY_OTHER_TAGS = "\u53ef\u4ee5\u8bd5\u8bd5\u8fd9\u4e9b\u6807\u7b7e"
    }
}
