package com.example.poetry.features.home.ui

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
import com.example.poetry.databinding.FragmentHomeBinding
import com.example.poetry.features.home.adapter.CategoryAdapter
import com.example.poetry.features.home.adapter.DailyRecommendationAdapter
import com.example.poetry.features.home.viewmodel.HomeViewModel

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var recommendationAdapter: DailyRecommendationAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupRecommendationList()
        setupCategoryList()
        bindData()
        bindActions()
    }

    private fun setupRecommendationList() {
        recommendationAdapter = DailyRecommendationAdapter(
            onCardClick = {
                findNavController().navigate(
                    R.id.poemDetailFragment,
                    bundleOf("workId" to it.workId)
                )
            },
            onAuthorClick = {
                if (it.authorId > 0L) {
                    findNavController().navigate(
                        R.id.authorDetailFragment,
                        bundleOf(
                            "authorId" to it.authorId,
                            "authorName" to it.author,
                            "dynastyName" to it.dynasty
                        )
                    )
                }
            }
        )

        binding.recommendRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recommendationAdapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
            isNestedScrollingEnabled = false
        }
    }

    private fun setupCategoryList() {
        categoryAdapter = CategoryAdapter { category ->
            findNavController().navigate(
                R.id.action_home_to_categoryList,
                bundleOf("categoryType" to category.type.name)
            )
        }

        binding.categoryRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun bindData() {
        viewModel.dailyRecommendationSection.observe(viewLifecycleOwner) { section ->
            binding.recommendThemeText.text = section.themeName
            binding.recommendThemeText.isVisible = section.themeName.isNotBlank()

            binding.recommendIntroText.text = section.introText
            binding.recommendIntroText.isVisible = section.introText.isNotBlank()

            binding.recommendDateText.text = section.recommendDate
            binding.recommendDateText.isVisible = section.recommendDate.isNotBlank()

            binding.recommendProgressBar.isVisible = section.isLoading
            binding.recommendErrorText.isVisible = !section.errorMessage.isNullOrBlank()
            binding.recommendErrorText.text = section.errorMessage.orEmpty()

            recommendationAdapter.submitList(section.items)
            binding.emptyRecommendText.isVisible = !section.isLoading &&
                section.errorMessage.isNullOrBlank() &&
                section.items.isEmpty()
        }

        viewModel.categories.observe(viewLifecycleOwner) {
            categoryAdapter.submitList(it)
        }
    }

    private fun bindActions() {
        binding.searchEntryCard.setOnClickListener {
            findNavController().navigate(
                R.id.action_home_to_searchResult,
                bundleOf(
                    "searchType" to "TITLE",
                    "titleQuery" to ""
                )
            )
        }

        binding.refreshRecommendButton.setOnClickListener {
            viewModel.refreshDailyRecommendations()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
