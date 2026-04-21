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
import com.example.poetry.databinding.FragmentSearchResultBinding
import com.example.poetry.features.poem.adapter.PoemSummaryAdapter
import com.example.poetry.features.poem.viewmodel.PoemViewModel

class SearchResultFragment : Fragment(R.layout.fragment_search_result) {

    private var _binding: FragmentSearchResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PoemViewModel by viewModels()
    private lateinit var adapter: PoemSummaryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchResultBinding.bind(view)

        adapter = PoemSummaryAdapter {
            // TODO: 传递真实作品参数
            findNavController().navigate(R.id.action_searchResult_to_poemDetail)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SearchResultFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { adapter.submitList(it) }
        binding.searchButton.setOnClickListener {
            // TODO: 接入真实搜索触发
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
