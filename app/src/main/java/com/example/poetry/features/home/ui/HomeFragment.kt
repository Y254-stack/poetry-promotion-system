package com.example.poetry.features.home.ui

import android.os.Bundle
import android.view.View
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
        recommendationAdapter = DailyRecommendationAdapter {
            // TODO: 这里接入真实诗词详情路由参数
            findNavController().navigate(R.id.action_home_to_poemDetail)
        }

        binding.recommendRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recommendationAdapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
            isNestedScrollingEnabled = false
        }
    }

    private fun setupCategoryList() {
        categoryAdapter = CategoryAdapter {
            // TODO: 这里接入真实分类检索逻辑
            findNavController().navigate(R.id.action_home_to_searchResult)
        }

        binding.categoryRecycler.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun bindData() {
        viewModel.dailyRecommendations.observe(viewLifecycleOwner) {
            recommendationAdapter.submitList(it)
        }
        viewModel.categories.observe(viewLifecycleOwner) {
            categoryAdapter.submitList(it)
        }
    }

    private fun bindActions() {
        binding.searchEntryCard.setOnClickListener {
            // TODO: 这里接入真实搜索输入与提交逻辑
            findNavController().navigate(R.id.action_home_to_searchResult)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
