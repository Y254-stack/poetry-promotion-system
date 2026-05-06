package com.example.poetry.features.favorite.ui

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
import com.example.poetry.databinding.FragmentFavoriteListBinding
import com.example.poetry.features.poem.adapter.PoemSummaryAdapter
import com.example.poetry.features.favorite.viewmodel.FavoriteViewModel

class FavoriteListFragment : Fragment(R.layout.fragment_favorite_list) {

    private var _binding: FragmentFavoriteListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoriteViewModel by viewModels()
    private lateinit var favoriteAdapter: PoemSummaryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFavoriteListBinding.bind(view)

        favoriteAdapter = PoemSummaryAdapter {
            findNavController().navigate(
                R.id.poemDetailFragment,
                bundleOf("workId" to it.workId)
            )
        }

        binding.favoriteRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoriteAdapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(10)))
        }

        viewModel.loadFavoriteList(1L)

        viewModel.favoriteList.observe(viewLifecycleOwner) { poems ->
            if (poems.isEmpty()) {
                binding.emptyText.visibility = View.VISIBLE
                binding.favoriteRecycler.visibility = View.GONE
            } else {
                binding.emptyText.visibility = View.GONE
                binding.favoriteRecycler.visibility = View.VISIBLE
                favoriteAdapter.submitList(poems)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
