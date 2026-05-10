package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentUserPublicProfileBinding
import com.example.poetry.features.favorite.adapter.FavoritePostAdapter
import com.example.poetry.features.user.viewmodel.UserPublicProfileViewModel

class UserPublicProfileFragment : Fragment(R.layout.fragment_user_public_profile) {

    private var _binding: FragmentUserPublicProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserPublicProfileViewModel by viewModels()
    private lateinit var postsAdapter: FavoritePostAdapter

    private val userId: Long
        get() = arguments?.getLong("userId", 0L) ?: 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUserPublicProfileBinding.bind(view)

        postsAdapter = FavoritePostAdapter(
            showRemove = false,
            onRemove = null,
            onClick = {
                findNavController().navigate(
                    R.id.action_userPublicProfile_to_postDetail,
                    bundleOf("postId" to it.postId)
                )
            }
        )
        binding.postsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postsAdapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(10)))
        }

        viewModel.header.observe(viewLifecycleOwner) { h ->
            if (h == null) {
                binding.nameText.text = ""
                binding.accountText.text = ""
                binding.avatarInitial.text = ""
                updatePostsEmpty()
                return@observe
            }
            binding.nameText.text = h.displayName
            binding.accountText.text = h.accountLine
            val initial = h.displayName.firstOrNull()?.uppercaseChar()
                ?: h.accountLine.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()
                ?: '?'
            binding.avatarInitial.text = initial.toString()
            updatePostsEmpty()
        }

        viewModel.posts.observe(viewLifecycleOwner) {
            postsAdapter.submitList(it)
            updatePostsEmpty()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loadingProgress.isVisible = it == true
            updatePostsEmpty()
        }

        viewModel.loadError.observe(viewLifecycleOwner) { err ->
            updatePostsEmpty()
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.loadUser(userId)
    }

    private fun updatePostsEmpty() {
        val posts = viewModel.posts.value.orEmpty()
        val loading = viewModel.isLoading.value == true
        val headerOk = viewModel.header.value != null
        binding.emptyPostsText.isVisible = headerOk && !loading && posts.isEmpty()
        binding.emptyPostsText.text = if (viewModel.loadError.value != null) {
            getString(R.string.user_public_posts_load_failed_hint)
        } else {
            getString(R.string.user_public_posts_empty_hint)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
