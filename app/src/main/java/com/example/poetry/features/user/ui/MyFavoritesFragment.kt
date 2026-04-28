package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentMyFavoritesBinding
import com.example.poetry.features.user.adapter.UserCollectionAdapter
import com.example.poetry.features.user.model.UserCollectionUiModel
import com.example.poetry.features.user.viewmodel.UserViewModel

class MyFavoritesFragment : Fragment(R.layout.fragment_my_favorites) {

    private var _binding: FragmentMyFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: UserCollectionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyFavoritesBinding.bind(view)

        adapter = UserCollectionAdapter { openFavorite(it) }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyFavoritesFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
        }

        viewModel.favorites.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    private fun openFavorite(item: UserCollectionUiModel) {
        val wid = item.linkedPoemWorkId
        when {
            wid != null -> {
                findNavController().navigate(
                    R.id.action_myFavorites_to_poemDetail,
                    bundleOf("workId" to wid)
                )
            }
            item.openAuthorDetail -> {
                findNavController().navigate(R.id.action_myFavorites_to_authorDetail)
            }
            else -> Toast.makeText(requireContext(), "暂无跳转目标", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
