package com.example.poetry.features.home.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.poetry.R
import com.example.poetry.core.network.ApiAuthorDto
import com.example.poetry.core.network.ApiDynastyDto
import com.example.poetry.core.network.NetworkModule
import com.example.poetry.databinding.FragmentCategoryListBinding
import com.example.poetry.features.home.adapter.CategoryDetailAdapter
import com.example.poetry.features.home.adapter.CategoryDetailItem
import com.example.poetry.features.home.model.CategoryType
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CategoryListFragment : Fragment(R.layout.fragment_category_list) {

    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CategoryDetailAdapter
    private var categoryType: CategoryType = CategoryType.DYNASTY

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCategoryListBinding.bind(view)

        categoryType = CategoryType.valueOf(arguments?.getString("categoryType") ?: "DYNASTY")

        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = CategoryDetailAdapter { item ->
            when (categoryType) {
                CategoryType.DYNASTY -> {
                    findNavController().navigate(
                        R.id.searchResultFragment,
                        bundleOf(
                            "searchType" to "DYNASTY",
                            "dynastyName" to item.name
                        )
                    )
                }
                CategoryType.AUTHOR -> {
                    findNavController().navigate(
                        R.id.searchResultFragment,
                        bundleOf(
                            "searchType" to "AUTHOR_ID",
                            "authorId" to item.id,
                            "authorName" to item.name
                        )
                    )
                }
            }
        }

        binding.categoryRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@CategoryListFragment.adapter
        }
    }

    private fun loadData() {
        binding.progressBar.isVisible = true

        when (categoryType) {
            CategoryType.DYNASTY -> {
                binding.titleText.text = "朝代分类"
                binding.subtitleText.text = "选择一个朝代查看该朝代的诗词"
                loadDynasties()
            }
            CategoryType.AUTHOR -> {
                binding.titleText.text = "诗人分类"
                binding.subtitleText.text = "选择一个诗人查看其作品"
                loadAuthors()
            }
        }
    }

    private fun loadDynasties() {
        NetworkModule.poetryApiService.getDynasties().enqueue(object : Callback<List<ApiDynastyDto>> {
            override fun onResponse(call: Call<List<ApiDynastyDto>>, response: Response<List<ApiDynastyDto>>) {
                binding.progressBar.isVisible = false
                val items = response.body()?.map {
                    CategoryDetailItem(it.dynastyId, it.dynastyName)
                } ?: emptyList()
                adapter.submitList(items)
            }

            override fun onFailure(call: Call<List<ApiDynastyDto>>, t: Throwable) {
                binding.progressBar.isVisible = false
                Log.e("CategoryListFragment", "Failed to load dynasties", t)
            }
        })
    }

    private fun loadAuthors() {
        NetworkModule.poetryApiService.getAuthors().enqueue(object : Callback<List<ApiAuthorDto>> {
            override fun onResponse(call: Call<List<ApiAuthorDto>>, response: Response<List<ApiAuthorDto>>) {
                binding.progressBar.isVisible = false
                val items = response.body()?.map {
                    CategoryDetailItem(it.authorId, it.authorName, it.dynastyName)
                } ?: emptyList()
                adapter.submitList(items)
            }

            override fun onFailure(call: Call<List<ApiAuthorDto>>, t: Throwable) {
                binding.progressBar.isVisible = false
                Log.e("CategoryListFragment", "Failed to load authors", t)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
